package vcore.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import vcore.events.impl.EventClickSlot;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class ItemScroller extends Module {
   public Setting<Integer> delay = new Setting<>("Delay", 50, 0, 500);
   private boolean pauseListening = false;

   public ItemScroller() {
      super("ItemScroller", "Fast item swap in chests.", Module.Category.PLAYER);
   }

   @EventHandler
   public void onClick(EventClickSlot e) {
      if ((this.isKeyPressed(340) || this.isKeyPressed(344))
         && (this.isKeyPressed(341) || this.isKeyPressed(345))
         && e.getSlotActionType() == ClickType.THROW
         && !this.pauseListening) {
         Item copy = ((Slot)mc.player.containerMenu.slots.get(e.getSlot())).getItem().getItem();
         this.pauseListening = true;

         for (int i2 = 0; i2 < mc.player.containerMenu.slots.size(); i2++) {
            if (((Slot)mc.player.containerMenu.slots.get(i2)).getItem().getItem() == copy) {
               mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, i2, 1, ClickType.THROW, mc.player);
            }
         }

         this.pauseListening = false;
      }
   }
}
