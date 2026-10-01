package vcore.features.modules.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.setting.Setting;
import vcore.utility.Timer;

public class AutoSprint extends Module {
   public static final Setting<Boolean> sprint = new Setting<>("KeepSprint", true);
   public static final Setting<Float> motion = new Setting<>("Motion", 1.0F, 0.0F, 1.0F, v -> sprint.getValue());
   public static volatile boolean forcedDisabled = false;
   private static volatile boolean auraPostAttackTriggered;
   private int ticksSinceGround;
   private boolean canSprint = true;
   private final Timer legitExtraHitCooldown = new Timer();

   public AutoSprint() {
      super("AutoSprint", "Automatically holds sprint.", Module.Category.MOVEMENT);
   }

   @Override
   public void onUpdate() {
      if (mc.player != null) {
         if (ModuleManager.guiMove.shouldSuppressSprint()) {
            mc.player.setSprinting(false);
            mc.options.keySprint.setDown(false);
         } else {
            this.handleAuraLegitSprintControl();
            this.handleAuraLegitExtraSprintControl();
            if (forcedDisabled) {
               mc.player.setSprinting(false);
               mc.options.keySprint.setDown(false);
            } else if (!this.canSprint) {
               mc.player.setSprinting(false);
               mc.options.keySprint.setDown(false);
            } else {
               mc.player
                  .setSprinting(
                     mc.player.getFoodData().getFoodLevel() > 6
                        && !mc.player.horizontalCollision
                        && vcore.utility.player.InputUtility.getForward(mc.player.input) > 0.0F
                        && (!mc.player.isShiftKeyDown() || ModuleManager.noSlow.isEnabled() && ModuleManager.noSlow.sneak.getValue())
                  );
            }
         }
      }
   }

   public static void setForcedDisabled(boolean disabled) {
      forcedDisabled = disabled;
   }

   public static boolean isForcedDisabled() {
      return forcedDisabled;
   }

   private void handleAuraLegitSprintControl() {
      LocalPlayer player = mc.player;
      if (player != null && this.shouldUseAuraSprintLogic()) {
         boolean onGround = player.onGround();
         boolean auraPostAttack = auraPostAttackTriggered;
         int airborneDelay = this.getAirborneDelayTicks();
         if (onGround) {
            this.ticksSinceGround = 0;
         } else if (this.ticksSinceGround < airborneDelay) {
            this.ticksSinceGround++;
         }

         boolean passedDelay = this.ticksSinceGround >= airborneDelay;
         if (!onGround && passedDelay && !auraPostAttack && !forcedDisabled) {
            this.pauseSprint();
         }

         if ((onGround || auraPostAttack) && forcedDisabled) {
            this.resumeSprint();
         }

         if (onGround) {
            auraPostAttackTriggered = false;
         }
      } else {
         if (forcedDisabled) {
            this.resumeSprint();
         }

         auraPostAttackTriggered = false;
         this.ticksSinceGround = 0;
      }
   }

   private void handleAuraLegitExtraSprintControl() {
      if (mc.player != null && this.shouldUseAuraLegitExtraLogic()) {
         boolean cancelReason = this.shouldCancelCritForLegitExtra();
         boolean isInDistance = ModuleManager.aura.isInRange(Aura.target);
         boolean canHit = ModuleManager.aura.isLookingAtHitbox()
            && isInDistance
            && this.legitExtraHitCooldown.passedMs(300L)
            && ModuleManager.aura.getAttackCooldown() >= 0.8F;
         this.canSprint = !canHit || cancelReason;
      } else {
         this.canSprint = true;
      }
   }

   private boolean shouldUseAuraLegitExtraLogic() {
      return ModuleManager.aura.isEnabled() && ModuleManager.aura.sprintMode.getValue() == Aura.SprintMode.LegitExtra && Aura.target != null;
   }

   private boolean shouldCancelCritForLegitExtra() {
      return ModuleManager.criticals.isEnabled() && !mc.player.onGround()
         || mc.player.isUnderWater()
         || mc.player.isInLava()
         || Managers.PLAYER.isInWeb()
         || mc.player.onClimbable()
         || mc.player.isPassenger()
         || mc.player.getAbilities().flying
         || mc.player.hasEffect(MobEffects.BLINDNESS)
         || mc.player.hasEffect(MobEffects.LEVITATION)
         || mc.player.hasEffect(MobEffects.SLOW_FALLING);
   }

   private boolean shouldUseAuraSprintLogic() {
      if (mc.player != null && Aura.target != null) {
         float maxRange = ModuleManager.aura.attackRange.getValue() + ModuleManager.aura.aimRange.getValue();
         double maxRangeSq = maxRange * maxRange;
         return ModuleManager.aura.isEnabled()
            && ModuleManager.aura.sprintMode.getValue() == Aura.SprintMode.Legit
            && ModuleManager.aura.smartCrit.getValue()
            && !mc.player.isFallFlying()
            && !mc.player.isUnderWater()
            && !mc.player.isInWater()
            && mc.player.distanceToSqr(Aura.target) <= maxRangeSq;
      } else {
         return false;
      }
   }

   private int getAirborneDelayTicks() {
      if (mc == null || mc.player == null || mc.options == null || mc.level == null) {
         return 0;
      }

      if (!mc.options.keyJump.isDown()) {
         return 0;
      }

      BlockPos headPos = BlockPos.containing(mc.player.getX(), mc.player.getY() + mc.player.getBbHeight() + 0.001, mc.player.getZ());
      if (mc.level.getBlockState(headPos).isCollisionShapeFullBlock(mc.level, headPos)) {
         return 0;
      }

      BlockPos belowPos = BlockPos.containing(mc.player.position().add(0.0, -0.4, 0.0));
      return mc.level.getBlockState(belowPos).getBlock() == Blocks.WATER ? 0 : 5;
   }

   private void pauseSprint() {
      if (mc.player != null && mc.options != null) {
         mc.player.setSprinting(false);
         mc.options.keySprint.setDown(false);
         setForcedDisabled(true);
      }
   }

   private void resumeSprint() {
      if (mc.player != null && mc.options != null) {
         mc.player.setSprinting(true);
         mc.options.keySprint.setDown(true);
         setForcedDisabled(false);
      }
   }

   public static void markAuraPostAttack() {
      auraPostAttackTriggered = true;
   }

   public void markAuraHitCooldown() {
      this.legitExtraHitCooldown.reset();
   }
}
