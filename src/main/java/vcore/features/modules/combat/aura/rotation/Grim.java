package vcore.features.modules.combat.aura.rotation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.combat.Aura;
import vcore.utility.math.MathUtility;

public class Grim implements RotationModeHandler {
   @Override
   public void rotate(Aura aura, boolean ready) {
      if (Aura.target != null) {
         if (Float.isNaN(aura.rotationYaw)) {
            aura.rotationYaw = 0.0F;
         }

         if (Float.isNaN(aura.rotationPitch)) {
            aura.rotationPitch = 0.0F;
         }

         boolean canSeeTarget = Aura.mc.player.hasLineOfSight(Aura.target);
         Vec3 aimPoint = Aura.target.getBoundingBox().getCenter();
         if (Aura.target instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldUseAuraRotationSpeed(livingTarget)) {
            Vec3 elytraTargetAimPoint = ModuleManager.elytraTarget.getTargetVec(livingTarget, ModuleManager.elytraTarget.shouldAimAtPrediction());
            if (elytraTargetAimPoint != null) {
               aimPoint = elytraTargetAimPoint;
            }
         }

         if (aura.igoneWall.getValue() && !canSeeTarget && aura.isWallsBypassPeekHigh()) {
            aimPoint = Aura.target.position().add(MathUtility.random(-0.15, 0.15), Aura.target.getBoundingBox().getYsize(), MathUtility.random(-0.15, 0.15));
         }

         aimPoint = ModuleManager.eMaceHelper.applyPeakAssistAimOffset(Aura.target, aimPoint);
         Vec3 targetPos = aimPoint.subtract(Aura.mc.player.getEyePosition()).normalize();
         float shortestYawPath = (float)(((Math.toDegrees(Math.atan2(targetPos.z, targetPos.x)) - 90.0 - aura.rotationYaw) % 360.0 + 540.0) % 360.0 - 180.0);
         if (aura.igoneWall.getValue() && !ready && !canSeeTarget && aura.isWallsBypassYawOffset()) {
            shortestYawPath += 20.0F;
         }

         float findPitch = (float)Math.min(90.0, -Math.toDegrees(Math.atan2(targetPos.y, Math.hypot(targetPos.x, targetPos.z))));
         float targetYaw = aura.rotationYaw + shortestYawPath;
         float targetPitch = Mth.clamp(findPitch, -90.0F, 90.0F);
         targetYaw += MathUtility.random(-1.0F, 1.0F);
         targetPitch += MathUtility.random(-1.0F, 1.0F);
         float[] correctedRotation = this.correctGrimRotation(aura, targetYaw, targetPitch);
         if (!Float.isNaN(correctedRotation[0]) && !Float.isNaN(correctedRotation[1])) {
            aura.rotationYaw = correctedRotation[0];
            aura.rotationPitch = correctedRotation[1];
         }

         ModuleManager.moveFix.fixRotation = aura.rotationYaw;
         aura.lookingAtHitbox = Managers.PLAYER.checkRtx(aura.rotationYaw, aura.rotationPitch, aura.getRange(), aura.getWallRange(), aura.rayTrace.getValue());
         if (Aura.target instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
            aura.lookingAtHitbox = aura.canAttackElytraTarget(livingTarget);
         }
      }
   }

   private float[] correctGrimRotation(Aura aura, float yaw, float pitch) {
      if ((yaw != -90.0F || pitch != 90.0F) && yaw != -180.0F) {
         float gcd = this.getGrimGcd(aura);
         yaw -= yaw % gcd;
         pitch -= pitch % gcd;
         return new float[]{yaw, pitch};
      } else {
         return new float[]{Aura.mc.player.getYRot(), Aura.mc.player.getXRot()};
      }
   }

   private float getGrimGcd(Aura aura) {
      double sensitivity = (Double)Aura.mc.options.sensitivity().get();
      double value = sensitivity * 0.6 + 0.2;
      return (float)(Math.pow(value, 1.5) * 0.8 * 0.15);
   }
}
