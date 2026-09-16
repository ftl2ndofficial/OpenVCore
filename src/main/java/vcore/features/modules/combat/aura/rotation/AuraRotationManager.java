package vcore.features.modules.combat.aura.rotation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.core.manager.player.PlayerManager;
import vcore.features.modules.combat.Aura;
import vcore.utility.player.PlayerUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public final class AuraRotationManager {
   private final Aura aura;
   private final RotationModeHandler trackRotation = new Track();
   private final RotationModeHandler snapRotation = new Snap();
   private final RotationModeHandler grimRotation = new Grim();
   private final RotationModeHandler noneRotation = new None();
   private final ElytraRotation elytraRotation = new ElytraRotation();

   public AuraRotationManager(Aura aura) {
      this.aura = aura;
   }

   private RotationModeHandler getModeHandler() {
      if (this.shouldUseElytraRotation()) {
         return this.elytraRotation;
      }

      return switch ((Aura.Mode)this.aura.rotationMode.getValue()) {
         case Track -> this.trackRotation;
         case Grim -> this.grimRotation;
         case Snap -> this.snapRotation;
         case None -> this.noneRotation;
      };
   }

   public void rotate(boolean ready) {
      float previousYaw = this.aura.rotationYaw;
      float previousPitch = this.aura.rotationPitch;
      if (this.shouldUseElytraRotation()) {
         this.elytraRotation.rotate(this.aura, ready);
      } else {
         this.getModeHandler().rotate(this.aura, ready);
      }

      this.aura.captureRotationState(previousYaw, previousPitch);
   }

   public boolean skipRayTraceCheck() {
      return this.aura.rotationMode.getValue() == Aura.Mode.None
         || !this.aura.rayTrace.getValue()
         || this.shouldBypassRayTraceForElytraDuel()
         || this.aura.rotationMode.is(Aura.Mode.Snap)
            && (
               this.aura.snapTicks.getValue() <= 1
                  || Aura.mc
                     .level
                     .getBlockCollisions(Aura.mc.player, Aura.mc.player.getBoundingBox().inflate(-0.25, 0.0, -0.25).move(0.0, 1.0, 0.0))
                     .iterator()
                     .hasNext()
            );
   }

   private boolean shouldBypassRayTraceForElytraDuel() {
      return Aura.mc.player != null && Aura.mc.player.isFallFlying() && Aura.target instanceof LivingEntity livingTarget && livingTarget.isFallFlying();
   }

   public void onRender3D() {
      if (this.aura.clientLook.getValue() && (this.aura.rotationMode.getValue() != Aura.Mode.None || this.shouldUseElytraRotation())) {
         Aura.mc.player.setYRot((float)Render2DEngine.interpolate(Aura.mc.player.yRotO, this.aura.getRenderRotationYaw(), Render3DEngine.getTickDelta()));
         Aura.mc
            .player
            .setXRot((float)Render2DEngine.interpolate(Aura.mc.player.xRotO, this.aura.getRenderRotationPitch(), Render3DEngine.getTickDelta()));
      }
   }

   public boolean canAttackElytraTarget(LivingEntity livingTarget) {
      if (Aura.mc.player == null || livingTarget == null) {
         return false;
      }

      if (!ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
         Vec3 eyePos = Aura.mc.player.getEyePosition();
         double rangeSq = this.aura.getRange() * this.aura.getRange();
         return this.isBoxInAttackRange(eyePos, livingTarget.getBoundingBox(), rangeSq);
      }

      Vec3 targetVec = this.getElytraAttackVec(livingTarget);
      if (targetVec == null) {
         return false;
      }

      double maxDistance = ModuleManager.elytraTarget.getPrediction(livingTarget) - 0.5;
      return Aura.mc.player.position().distanceTo(targetVec) <= maxDistance || Aura.mc.player.getBoundingBox().contains(targetVec);
   }

   private boolean shouldUseElytraRotation() {
      return Aura.target instanceof LivingEntity livingTarget ? ModuleManager.elytraTarget.shouldUseElytraTargetRotation(livingTarget) : false;
   }

   private Vec3 getElytraAttackVec(LivingEntity livingTarget) {
      return ModuleManager.elytraTarget.getTargetVec(livingTarget, true);
   }

   private boolean isBoxInAttackRange(Vec3 eyePos, AABB box, double rangeSq) {
      return eyePos.distanceToSqr(this.getClosestPoint(eyePos, box)) <= rangeSq;
   }

   private Vec3 getClosestPoint(Vec3 eyePos, AABB box) {
      return new Vec3(
         Mth.clamp(eyePos.x, box.minX, box.maxX), Mth.clamp(eyePos.y, box.minY, box.maxY), Mth.clamp(eyePos.z, box.minZ, box.maxZ)
      );
   }

   public boolean isWallsBypassYawOffset() {
      return ModuleManager.wallsBypass.isEnabled() && ModuleManager.wallsBypass.isYawOffset();
   }

   public boolean isWallsBypassPeekHigh() {
      return ModuleManager.wallsBypass.isEnabled() && ModuleManager.wallsBypass.isPeekHigh();
   }

   public float getSquaredRotateDistance() {
      float dst = this.aura.getRange();
      boolean needsAimRange = this.aura.rotationMode.getValue() == Aura.Mode.Track
         || this.aura.rotationMode.getValue() == Aura.Mode.Grim
         || this.shouldUseElytraSearchRange();
      if (needsAimRange) {
         dst += this.aura.getAimRange();
      }

      if ((Aura.mc.player.isFallFlying() || ModuleManager.elytraPlus.isEnabled()) && Aura.target != null) {
         dst += 4.0F;
      }

      if (!needsAimRange) {
         dst = this.aura.getRange();
      }

      return dst * dst;
   }

   public boolean isInRange(Entity target) {
      if (target instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
         Vec3 targetVec = ModuleManager.elytraTarget.getTargetVec(livingTarget, true);
         return targetVec != null && PlayerUtility.squaredDistanceFromEyes(targetVec) <= this.getSquaredRotateDistance();
      } else {
         float sqRotate = this.getSquaredRotateDistance();
         AABB bb = target.getBoundingBox();
         Vec3 eye = Aura.mc.player.getEyePosition();
         double cx = Mth.clamp(eye.x, bb.minX, bb.maxX);
         double cy = Mth.clamp(eye.y, bb.minY, bb.maxY);
         double cz = Mth.clamp(eye.z, bb.minZ, bb.maxZ);
         double dx = cx - eye.x;
         double dy = cy - eye.y;
         double dz = cz - eye.z;
         if (dx * dx + dy * dy + dz * dz > sqRotate) {
            return false;
         }

         if (PlayerUtility.squaredDistanceFromEyes(target.position().add(0.0, target.getEyeHeight(target.getPose()), 0.0)) > sqRotate + 4.0F) {
            return false;
         }

         float halfBox = (float)(bb.getXsize() / 2.0);
         float lenY = (float)bb.getYsize();
         double tx = target.getX();
         double ty = target.getY();
         double tz = target.getZ();
         float rangeDist = (float)Math.sqrt(sqRotate);
         float wallRange = this.aura.getWallRange();
         boolean useRayTrace = this.aura.rayTrace.getValue();

         for (float x1 = -halfBox; x1 <= halfBox; x1 += 0.15F) {
            for (float z1 = -halfBox; z1 <= halfBox; z1 += 0.15F) {
               for (float y1 = 0.05F; y1 <= lenY; y1 += 0.25F) {
                  Vec3 point = new Vec3(tx + x1, ty + y1, tz + z1);
                  if (!(PlayerUtility.squaredDistanceFromEyes(point) > sqRotate)) {
                     float[] rotation = PlayerManager.calcAngle(point);
                     if (Managers.PLAYER.checkRtx(rotation[0], rotation[1], rangeDist, wallRange, useRayTrace)) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private boolean shouldUseElytraSearchRange() {
      return Aura.mc.player != null && Aura.mc.player.isFallFlying() && ModuleManager.elytraTarget.isEnabled();
   }
}
