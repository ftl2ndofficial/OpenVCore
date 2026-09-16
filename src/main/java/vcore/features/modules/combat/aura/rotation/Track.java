package vcore.features.modules.combat.aura.rotation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.core.manager.player.PlayerManager;
import vcore.features.modules.combat.Aura;
import vcore.utility.math.MathUtility;
import vcore.utility.player.PlayerUtility;

public class Track implements RotationModeHandler {
   @Override
   public void rotate(Aura aura, boolean ready) {
      rotateClassic(aura, ready);
   }

   static void rotateClassic(Aura aura, boolean ready) {
      if (ready) {
         aura.trackticks = Aura.mc
               .level
               .getBlockCollisions(Aura.mc.player, Aura.mc.player.getBoundingBox().inflate(-0.25, 0.0, -0.25).move(0.0, 1.0, 0.0))
               .iterator()
               .hasNext()
            ? 1
            : aura.snapTicks.getValue();
      } else if (aura.trackticks > 0) {
         aura.trackticks--;
      }

      if (Aura.target != null) {
         Entity targetEntity = Aura.target;
         Vec3 targetVec;
         if (!Aura.mc.player.isFallFlying() && !ModuleManager.elytraPlus.isEnabled()) {
            targetVec = getDynamicLookPoint(aura, targetEntity);
         } else if (targetEntity instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldUseAuraRotationSpeed(livingTarget)) {
            targetVec = ModuleManager.elytraTarget.getTargetVec(livingTarget, ModuleManager.elytraTarget.shouldAimAtPrediction());
         } else {
            targetVec = targetEntity.getEyePosition();
         }

         targetVec = ModuleManager.eMaceHelper.applyPeakAssistAimOffset(targetEntity, targetVec);
         if (targetVec != null) {
            aura.pitchAcceleration = Managers.PLAYER
                  .checkRtx(
                     aura.rotationYaw, aura.rotationPitch, aura.getRange() + aura.getAimRange(), aura.getRange() + aura.getAimRange(), aura.rayTrace.getValue()
                  )
               ? aura.aimedPitchStep.getValue()
               : (
                  aura.pitchAcceleration < aura.maxPitchStep.getValue()
                     ? aura.pitchAcceleration * aura.pitchAccelerate.getValue()
                     : aura.maxPitchStep.getValue()
               );
            float deltaYawRaw = Mth.wrapDegrees(
                  (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(targetVec.z - Aura.mc.player.getZ(), targetVec.x - Aura.mc.player.getX())) - 90.0)
                     - aura.rotationYaw
               )
               + (aura.isWallsBypassYawOffset() && !ready && !Aura.mc.player.hasLineOfSight(targetEntity) ? 20 : 0);
            float deltaPitch = (float)(
               -Math.toDegrees(
                     Math.atan2(
                        targetVec.y - (Aura.mc.player.position().y + Aura.mc.player.getEyeHeight(Aura.mc.player.getPose())),
                        Math.sqrt(Math.pow(targetVec.x - Aura.mc.player.getX(), 2.0) + Math.pow(targetVec.z - Aura.mc.player.getZ(), 2.0))
                     )
                  )
                  - aura.rotationPitch
            );
            float yawStep = aura.rotationMode.getValue() != Aura.Mode.Track
               ? 360.0F
               : MathUtility.random(aura.minYawStep.getValue().intValue(), aura.maxYawStep.getValue().intValue());
            float pitchStep = aura.rotationMode.getValue() != Aura.Mode.Track
               ? 180.0F
               : (Managers.PLAYER.ticksElytraFlying > 5 ? 180.0F : aura.pitchAcceleration + MathUtility.random(-1.0F, 1.0F));
            if (ready) {
               switch ((Aura.AccelerateOnHit)aura.accelerateOnHit.getValue()) {
                  case Off:
                  default:
                     break;
                  case Yaw:
                     yawStep = 180.0F;
                     break;
                  case Pitch:
                     pitchStep = 90.0F;
                     break;
                  case Both:
                     yawStep = 180.0F;
                     pitchStep = 90.0F;
               }
            }

            if (deltaYawRaw > 180.0F) {
               deltaYawRaw -= 180.0F;
            }

            float deltaYaw = Mth.clamp(Mth.abs(deltaYawRaw), -yawStep, yawStep);
            float clampedPitch = Mth.clamp(deltaPitch, -pitchStep, pitchStep);
            float newYaw = aura.rotationYaw + (deltaYawRaw > 0.0F ? deltaYaw : -deltaYaw);
            float newPitch = Mth.clamp(aura.rotationPitch + clampedPitch, -90.0F, 90.0F);
            double gcdFix = Math.pow((Double)Aura.mc.options.sensitivity().get() * 0.6 + 0.2, 3.0) * 1.2;
            if (aura.trackticks <= 0 && aura.rotationMode.getValue() != Aura.Mode.Track) {
               aura.rotationYaw = Aura.mc.player.getYRot();
               aura.rotationPitch = Aura.mc.player.getXRot();
            } else {
               aura.rotationYaw = (float)(newYaw - (newYaw - aura.rotationYaw) % gcdFix);
               aura.rotationPitch = (float)(newPitch - (newPitch - aura.rotationPitch) % gcdFix);
            }

            ModuleManager.moveFix.fixRotation = aura.rotationYaw;
            aura.lookingAtHitbox = Managers.PLAYER
               .checkRtx(aura.rotationYaw, aura.rotationPitch, aura.getRange(), aura.getWallRange(), aura.rayTrace.getValue());
            if (targetEntity instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
               aura.lookingAtHitbox = aura.canAttackElytraTarget(livingTarget);
            }
         }
      }
   }

   private static Vec3 getDynamicLookPoint(Aura aura, Entity targetEntity) {
      float minMotionXZ = 0.003F;
      float maxMotionXZ = 0.03F;
      float minMotionY = 0.001F;
      float maxMotionY = 0.03F;
      double lengthX = targetEntity.getBoundingBox().getXsize();
      double lengthY = targetEntity.getBoundingBox().getYsize();
      double lengthZ = targetEntity.getBoundingBox().getZsize();
      if (aura.rotationMotion.equals(Vec3.ZERO)) {
         aura.rotationMotion = new Vec3(MathUtility.random(-0.05F, 0.05F), MathUtility.random(-0.05F, 0.05F), MathUtility.random(-0.05F, 0.05F));
      }

      aura.rotationPoint = aura.rotationPoint.add(aura.rotationMotion);
      if (aura.rotationPoint.x >= (lengthX - 0.05) / 2.0) {
         aura.rotationMotion = new Vec3(-MathUtility.random(minMotionXZ, maxMotionXZ), aura.rotationMotion.y(), aura.rotationMotion.z());
      }

      if (aura.rotationPoint.y >= lengthY) {
         aura.rotationMotion = new Vec3(aura.rotationMotion.x(), -MathUtility.random(minMotionY, maxMotionY), aura.rotationMotion.z());
      }

      if (aura.rotationPoint.z >= (lengthZ - 0.05) / 2.0) {
         aura.rotationMotion = new Vec3(aura.rotationMotion.x(), aura.rotationMotion.y(), -MathUtility.random(minMotionXZ, maxMotionXZ));
      }

      if (aura.rotationPoint.x <= -(lengthX - 0.05) / 2.0) {
         aura.rotationMotion = new Vec3(MathUtility.random(minMotionXZ, 0.03F), aura.rotationMotion.y(), aura.rotationMotion.z());
      }

      if (aura.rotationPoint.y <= 0.05) {
         aura.rotationMotion = new Vec3(aura.rotationMotion.x(), MathUtility.random(minMotionY, maxMotionY), aura.rotationMotion.z());
      }

      if (aura.rotationPoint.z <= -(lengthZ - 0.05) / 2.0) {
         aura.rotationMotion = new Vec3(aura.rotationMotion.x(), aura.rotationMotion.y(), MathUtility.random(minMotionXZ, maxMotionXZ));
      }

      aura.rotationPoint.add(MathUtility.random(-0.03F, 0.03F), 0.0, MathUtility.random(-0.03F, 0.03F));
      if (aura.igoneWall.getValue() && !Aura.mc.player.hasLineOfSight(targetEntity) && aura.isWallsBypassPeekHigh()) {
         return targetEntity.position().add(MathUtility.random(-0.15, 0.15), lengthY, MathUtility.random(-0.15, 0.15));
      }

      if (!Managers.PLAYER.checkRtx(aura.rotationYaw, aura.rotationPitch, aura.getRange(), aura.getWallRange(), aura.rayTrace.getValue())) {
         float[] baseRotation = PlayerManager.calcAngle(targetEntity.position().add(0.0, targetEntity.getEyeHeight(targetEntity.getPose()) / 2.0F, 0.0));
         if (PlayerUtility.squaredDistanceFromEyes(targetEntity.position().add(0.0, targetEntity.getEyeHeight(targetEntity.getPose()) / 2.0F, 0.0))
               <= aura.attackRange.getPow2Value()
            && Managers.PLAYER.checkRtx(baseRotation[0], baseRotation[1], aura.getRange(), 0.0F, aura.rayTrace.getValue())) {
            aura.rotationPoint = new Vec3(
               MathUtility.random(-0.1F, 0.1F),
               targetEntity.getEyeHeight(targetEntity.getPose()) / MathUtility.random(1.8F, 2.5F),
               MathUtility.random(-0.1F, 0.1F)
            );
         } else {
            float halfBox = (float)(lengthX / 2.0);

            for (float x1 = -halfBox; x1 <= halfBox; x1 += 0.05F) {
               for (float z1 = -halfBox; z1 <= halfBox; z1 += 0.05F) {
                  for (float y1 = 0.05F; y1 <= targetEntity.getBoundingBox().getYsize(); y1 += 0.15F) {
                     Vec3 point = new Vec3(targetEntity.getX() + x1, targetEntity.getY() + y1, targetEntity.getZ() + z1);
                     if (!(PlayerUtility.squaredDistanceFromEyes(point) > aura.attackRange.getPow2Value())) {
                        float[] rotation = PlayerManager.calcAngle(point);
                        if (Managers.PLAYER.checkRtx(rotation[0], rotation[1], aura.getRange(), 0.0F, aura.rayTrace.getValue())) {
                           aura.rotationPoint = new Vec3(x1, y1, z1);
                           break;
                        }
                     }
                  }
               }
            }
         }
      }

      return targetEntity.position().add(aura.rotationPoint);
   }
}
