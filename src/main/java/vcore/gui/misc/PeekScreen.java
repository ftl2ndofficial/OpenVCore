package vcore.gui.misc;

import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import vcore.features.modules.render.Tooltips;

public class PeekScreen extends ShulkerBoxScreen {
   private static final ItemStack[] ITEMS = new ItemStack[27];

   public PeekScreen(ShulkerBoxMenu handler, Inventory inventory, Component title, Block block) {
      super(handler, inventory, title);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 2
         && this.hoveredSlot != null
         && !this.hoveredSlot.getItem().isEmpty()
         && this.minecraft.player.inventoryMenu.getCarried().isEmpty()) {
         ItemStack itemStack = this.hoveredSlot.getItem();
         if (Tooltips.hasItems(itemStack) && Tooltips.middleClickOpen.getValue()) {
            Arrays.fill(ITEMS, ItemStack.EMPTY);
            ItemContainerContents nbt = (ItemContainerContents)itemStack.get(DataComponents.CONTAINER);
            if (nbt != null) {
               List<ItemStack> list = nbt.stream().toList();

               for (int i = 0; i < list.size(); i++) {
                  ITEMS[i] = list.get(i);
               }
            }

            this.minecraft
               .setScreen(
                  new PeekScreen(
                     new ShulkerBoxMenu(0, this.minecraft.player.getInventory(), new SimpleContainer(ITEMS)),
                     this.minecraft.player.getInventory(),
                     this.hoveredSlot.getItem().getHoverName(),
                     ((BlockItem)this.hoveredSlot.getItem().getItem()).getBlock()
                  )
               );
            return true;
         }
      }

      return false;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return false;
   }
}
