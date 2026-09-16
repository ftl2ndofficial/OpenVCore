package vcore.features.modules.combat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vcore.core.InputBlocker;
import vcore.features.modules.Module;
import vcore.injection.accesors.IMinecraftClient;
import vcore.setting.Setting;

public class BowSpam extends Module {
   private final Setting<Boolean> auto = new Setting<>("Auto", false);
   private final Setting<Integer> holdDelay = new Setting<>("HoldDelay", 400, 120, 1000);
   private final Setting<Integer> shootsDelay = new Setting<>("ShootsDelay", 60, 0, 1000);
   private long holdStartAt = 0L;
   private long lastShotAt = 0L;
   private boolean autoPullActive;
   private boolean useKeyHeld;

   public BowSpam() {
      super("BowSpam", "Auto releases bow.", Module.Category.COMBAT);
   }

   @Override
   public void onUpdate() {
      if (!this.canProcess()) {
         this.resetState();
      } else if (this.isUsingBow()) {
         this.handleActiveBowUse();
      } else {
         this.resetPullState();
         if (this.auto.getValue()) {
            this.tryStartAutoPull();
         }
      }
   }

   @Override
   public void onDisable() {
      if (this.canProcess()) {
         this.releaseAutoPull(this.isUsingBow());
      } else {
         this.resetState();
      }

      super.onDisable();
   }

   private void handleActiveBowUse() {
      if (!this.canUseBowNow()) {
         this.stopUsingBow(false);
      } else {
         long now = System.currentTimeMillis();
         if (this.holdStartAt == 0L) {
            this.holdStartAt = now;
         }

         if (now - this.holdStartAt >= this.holdDelay.getValue().intValue()) {
            this.stopUsingBow(true);
         }
      }
   }

   private void tryStartAutoPull() {
      if (this.canUseBowNow()) {
         InteractionHand bowHand = this.getAvailableBowHand();
         if (bowHand != null && this.canUseBowFromHand(bowHand)) {
            this.startUsingBow(bowHand);
            this.holdStartAt = System.currentTimeMillis();
            this.autoPullActive = true;
         }
      }
   }

   private void stopUsingBow(boolean releasedShot) {
      this.releaseAutoPull(releasedShot);
      if (releasedShot) {
         this.lastShotAt = System.currentTimeMillis();
         int delayMs = this.shootsDelay.getValue();
         if (delayMs > 0) {
            InputBlocker.blockUseFor(delayMs);
         }
      }
   }

   private boolean canProcess() {
      return mc.player != null && mc.gameMode != null;
   }

   private boolean canUseBowNow() {
      if (InputBlocker.isUseBlocked()) {
         return false;
      }

      int delayMs = this.shootsDelay.getValue();
      return delayMs <= 0 || System.currentTimeMillis() - this.lastShotAt >= delayMs;
   }

   private boolean isUsingBow() {
      return mc.player != null && mc.player.isUsingItem() && mc.player.getUseItem().getItem() == Items.BOW;
   }

   private InteractionHand getAvailableBowHand() {
      if (mc.player == null) {
         return null;
      } else if (this.isBow(mc.player.getMainHandItem())) {
         return InteractionHand.MAIN_HAND;
      } else {
         return this.isBow(mc.player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
      }
   }

   private boolean canUseBowFromHand(InteractionHand hand) {
      if (mc.player == null) {
         return false;
      }

      ItemStack bowStack = mc.player.getItemInHand(hand);
      return this.isBow(bowStack) && (mc.player.getAbilities().instabuild || !mc.player.getProjectile(bowStack).isEmpty());
   }

   private boolean isBow(ItemStack stack) {
      return stack.getItem() == Items.BOW;
   }

   private void startUsingBow(InteractionHand hand) {
      if (mc.screen != null) {
         ((IMinecraftClient)mc).idoItemUse();
      } else {
         mc.options.keyUse.setDown(true);
         this.useKeyHeld = true;
      }
   }

   private void releaseAutoPull(boolean stopUsingItem) {
      if (this.useKeyHeld) {
         mc.options.keyUse.setDown(false);
         this.useKeyHeld = false;
      }

      if (stopUsingItem && this.canProcess() && this.isUsingBow()) {
         mc.gameMode.releaseUsingItem(mc.player);
      }

      this.resetPullState();
   }

   private void resetPullState() {
      this.holdStartAt = 0L;
      this.autoPullActive = false;
   }

   private void resetState() {
      this.resetPullState();
      this.lastShotAt = 0L;
      if (this.useKeyHeld) {
         mc.options.keyUse.setDown(false);
         this.useKeyHeld = false;
      }
   }
}
