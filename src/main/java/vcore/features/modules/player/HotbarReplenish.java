package vcore.features.modules.player;

import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.Timer;

public class HotbarReplenish extends Module {
   private final Setting<HotbarReplenish.Mode> mode = new Setting<>("Mode", HotbarReplenish.Mode.SWAP);
   private final Setting<Integer> delay = new Setting<>("Delay", 2, 0, 10);
   private final Setting<Integer> refillThr = new Setting<>("Threshold", 16, 1, 63);
   private final Setting<Integer> refillSmallThr = new Setting<>("PearlsThreshold", 4, 1, 15);
   private final Timer timer = new Timer();

   public HotbarReplenish() {
      super("HotbarReplenish", "Refills hotbar items.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (mc.screen == null) {
         if (this.timer.passedMs(this.delay.getValue() * 1000)) {
            for (int i = 0; i < 9; i++) {
               if (this.need(i)) {
                  this.timer.reset();
                  return;
               }
            }
         }
      }
   }

   private boolean need(int slot) {
      ItemStack stack = mc.player.getInventory().getItem(slot);
      if (!stack.isEmpty() && stack.isStackable()) {
         if (stack.getMaxStackSize() == 16 && stack.getCount() > this.refillSmallThr.getValue()) {
            return false;
         }

         if (stack.getMaxStackSize() == 64 && stack.getCount() > this.refillThr.getValue()) {
            return false;
         }

         for (int i = 9; i < 36; i++) {
            ItemStack item = mc.player.getInventory().getItem(i);
            if (!item.isEmpty() && this.canMerge(stack, item)) {
               boolean swap = this.mode.is(HotbarReplenish.Mode.QUICK_MOVE);
               clickSlot(i, swap ? slot : 0, swap ? ClickType.SWAP : ClickType.QUICK_MOVE);
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean canMerge(ItemStack source, ItemStack stack) {
      return source.getItem() == stack.getItem() && source.getHoverName().equals(stack.getHoverName());
   }

   private enum Mode {
      QUICK_MOVE,
      SWAP;
   }
}
