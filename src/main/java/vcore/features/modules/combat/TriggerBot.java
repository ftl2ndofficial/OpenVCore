package vcore.features.modules.combat;

import java.util.Random;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;

public final class TriggerBot extends Module {
   public final Setting<Float> attackRange = new Setting<>("Range", 3.0F, 1.0F, 7.0F);
   public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
   public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(this.smartCrit);
   public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(this.smartCrit);
   public final Setting<Boolean> ignoreWalls = new Setting<>("IgnoreWalls", false);
   public final Setting<Boolean> pauseEating = new Setting<>("PauseWhileEating", false);
   public final Setting<Integer> minDelay = new Setting<>("RandomDelayMin", 2, 0, 20);
   public final Setting<Integer> maxDelay = new Setting<>("RandomDelayMax", 13, 0, 20);
   private int delay;
   private final Random random = new Random();

   public TriggerBot() {
      super("TriggerBot", "Auto attacks what you look at.", Module.Category.COMBAT);
   }

   @EventHandler
   public void onAttack(PlayerUpdateEvent e) {
      if (!mc.player.isUsingItem() || !this.pauseEating.getValue()) {
         if (!mc.options.keyJump.isDown() && mc.player.onGround() && this.autoJump.getValue()) {
            mc.player.jumpFromGround();
         }

         if (!this.autoCrit() && this.delay > 0) {
            this.delay--;
         } else {
            Entity ent = Managers.PLAYER.getRtxTarget(mc.player.getYRot(), mc.player.getXRot(), this.attackRange.getValue(), this.ignoreWalls.getValue());
            if (ent != null && !Managers.FRIEND.isFriend(ent.getName().getString())) {
               mc.gameMode.attack(mc.player, ent);
               mc.player.swing(InteractionHand.MAIN_HAND);
               this.delay = this.random.nextInt(this.minDelay.getValue(), this.maxDelay.getValue() + 1);
            }
         }
      }
   }

   private boolean autoCrit() {
      boolean reasonForSkipCrit = !this.smartCrit.getValue().isEnabled()
         || mc.player.getAbilities().flying
         || mc.player.isFallFlying()
         || ModuleManager.elytraPlus.isEnabled()
         || mc.player.hasEffect(MobEffects.BLINDNESS)
         || mc.player.isSuppressingSlidingDownLadder()
         || mc.level.getBlockState(BlockPos.containing(mc.player.position())).getBlock() == Blocks.COBWEB;
      if (mc.player.fallDistance > 1.0F && mc.player.fallDistance < 1.14) {
         return false;
      } else if (ModuleManager.aura.getAttackCooldown() < (mc.player.onGround() ? 1.0F : 0.9F)) {
         return false;
      } else {
         boolean mergeWithTargetStrafe = !ModuleManager.targetStrafe.isEnabled() || !ModuleManager.targetStrafe.jump.getValue();
         boolean mergeWithSpeed = !ModuleManager.speed.isEnabled() || mc.player.onGround();
         if (!mc.options.keyJump.isDown() && mergeWithTargetStrafe && mergeWithSpeed && !this.onlySpace.getValue() && !this.autoJump.getValue()) {
            return true;
         } else if (mc.player.isInLava()) {
            return true;
         } else if (!mc.options.keyJump.isDown() && ModuleManager.aura.isAboveWater()) {
            return true;
         } else {
            return reasonForSkipCrit ? true : !mc.player.onGround() && mc.player.fallDistance > 0.0F;
         }
      }
   }
}
