package vcore.features.modules.player;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.utility.Timer;

public class InventoryCleaner extends Module {
   public final Setting<ItemSelectSetting> items = new Setting<>("Items", new ItemSelectSetting(new ArrayList<>()));
   private final Setting<InventoryCleaner.DropWhen> dropWhen = new Setting<>("DropWhen", InventoryCleaner.DropWhen.NotInInventory);
   private final Setting<Integer> delay = new Setting<>("Delay", 50, 0, 500);
   private final Setting<Boolean> cleanChests = new Setting<>("CleanChests", false);
   private final Timer delayTimer = new Timer();
   private boolean dirty;

   public InventoryCleaner() {
      super("InventoryCleaner", "Cleans unwanted items.", Module.Category.PLAYER);
   }

   @Override
   public void onRender3D(PoseStack stack) {
      boolean inInv = mc.screen instanceof InventoryScreen;
      if (mc.player.containerMenu instanceof ChestMenu chest && this.cleanChests.getValue()) {
         for (int i = 0; i < chest.getContainer().getContainerSize(); i++) {
            Slot slot = chest.getSlot(i);
            if (slot.hasItem()
               && this.dropThisShit(slot.getItem())
               && mc.screen != null
               && !mc.screen.getTitle().getString().contains("Ð\u0090ÑƒÐºÑ†Ð¸Ð¾Ð½")
               && !mc.screen.getTitle().getString().contains("Ð¿Ð¾ÐºÑƒÐ¿ÐºÐ¸")
               && this.delayTimer.every(this.delay.getValue().intValue())) {
               mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, i, 1, ClickType.THROW, mc.player);
               this.dirty = true;
            }
         }
      }

      if (this.dropWhen.getValue() != InventoryCleaner.DropWhen.Inventory || inInv) {
         if (this.dropWhen.getValue() != InventoryCleaner.DropWhen.NotInInventory || !inInv) {
            for (int slot = 0; slot < 36; slot++) {
               ItemStack itemFromslot = mc.player.getInventory().getItem(slot);
               if (this.dropThisShit(itemFromslot)) {
                  this.drop(slot);
               }
            }

            if (this.dirty && this.delayTimer.passedMs(this.delay.getValue() + 100)) {
               this.sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
               this.debug("after click cleaning...");
               this.dirty = false;
            }
         }
      }
   }

   private void drop(int slot) {
      if (this.delayTimer.every(this.delay.getValue().intValue())) {
         mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot < 9 ? slot + 36 : slot, 1, ClickType.THROW, mc.player);
         this.dirty = true;
      }
   }

   private boolean dropThisShit(ItemStack stack) {
      return this.items.getValue().getItemsById().contains(stack.getItem().getDescriptionId().replace("block.minecraft.", "").replace("item.minecraft.", ""));
   }

   public enum DropWhen {
      Inventory,
      Always,
      NotInInventory;
   }
}
