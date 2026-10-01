package vcore.features.modules.player;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vcore.core.Managers;
import vcore.features.modules.Module;
import vcore.gui.notification.Notification;
import vcore.setting.Setting;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;

public class ElytraReplace extends Module {
   private final Setting<Integer> durability = new Setting<>("Durability", 5, 0, 100);

   public ElytraReplace() {
      super("ElytraReplace", "Replaces broken elytras.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      ItemStack is = mc.player.getItemBySlot(EquipmentSlot.CHEST);
      if (is.is(Items.ELYTRA) && 100.0F - (float)is.getDamageValue() / is.getMaxDamage() * 100.0F <= this.durability.getValue().intValue()) {
          SearchInvResult result = InventoryUtility.findInInventory(
             stack -> stack.is(Items.ELYTRA)
                ? 100.0F - (float)stack.getDamageValue() / stack.getMaxDamage() * 100.0F > this.durability.getValue().intValue()
                : false
          );
         if (result.found()) {
            clickSlot(result.slot());
            clickSlot(6);
            clickSlot(result.slot());
            this.sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            Managers.NOTIFICATION.publicity("ElytraReplace", "Swapping the old elytra for a new one!", 2, Notification.Type.SUCCESS);
            this.sendMessage("Swapping the old elytra for a new one!");
         }
      }
   }
}
