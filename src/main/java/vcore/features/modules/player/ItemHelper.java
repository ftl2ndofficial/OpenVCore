package vcore.features.modules.player;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import vcore.core.InputBlocker;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.setting.impl.ItemBindSetting;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.MovementUtility;
import vcore.utility.player.SearchInvResult;

public class ItemHelper extends Module {
   public final Setting<ItemBindSetting> item1 = new Setting<>("Item 1", new ItemBindSetting());
   public final Setting<ItemBindSetting> item2 = new Setting<>("Item 2", new ItemBindSetting());
   public final Setting<ItemBindSetting> item3 = new Setting<>("Item 3", new ItemBindSetting());
   public final Setting<ItemBindSetting> item4 = new Setting<>("Item 4", new ItemBindSetting());
   public final Setting<ItemBindSetting> item5 = new Setting<>("Item 5", new ItemBindSetting());
   public final Setting<Boolean> grimStop = new Setting<>("GrimStop", false);
   private final List<Setting<ItemBindSetting>> itemSettings = Arrays.asList(this.item1, this.item2, this.item3, this.item4, this.item5);
   private final boolean[] pressed = new boolean[this.itemSettings.size()];
   private boolean scheduledUse;
   private int scheduledInventorySlot = -1;
   private int scheduledHotbarSlot = -1;
   private long scheduledUseAt = -1L;
   private static final long GRIM_STOP_DELAY_MS = 5L;

   public ItemHelper() {
      super("ItemHelper", "Quick swap and use saved items.", Module.Category.PLAYER);
   }

   @Override
   public void onEnable() {
      this.resetScheduledUse();
   }

   @Override
   public void onDisable() {
      this.resetScheduledUse();
   }

   @Override
   public void onUpdate() {
      if (fullNullCheck()) {
         this.resetScheduledUse();
      } else {
         this.handleScheduledUse();
         if (mc.screen == null) {
            for (int i = 0; i < this.itemSettings.size(); i++) {
               ItemBindSetting setting = this.itemSettings.get(i).getValue();
               Bind bind = setting.getBind();
               boolean down = bind != null && this.isKeyPressed(bind.getKey());
               if (down && !this.pressed[i] && !this.useItem(setting)) {
                  this.sendMessage("Item does not exist!");
               }

               this.pressed[i] = down;
            }
         }
      }
   }

   public List<Setting<ItemBindSetting>> getItemSettings() {
      return this.itemSettings;
   }

   public int findEmptyIndex() {
      for (int i = 0; i < this.itemSettings.size(); i++) {
         if (!this.itemSettings.get(i).getValue().hasItem()) {
            return i;
         }
      }

      return -1;
   }

   public boolean setItemAt(int index, Item item) {
      if (item != null && item != Items.AIR) {
         Setting<ItemBindSetting> setting = this.itemSettings.get(index);
         ItemBindSetting current = setting.getValue();
         setting.setValue(new ItemBindSetting(BuiltInRegistries.ITEM.getKey(item).toString(), current.getBind()));
         return true;
      } else {
         return false;
      }
   }

   public void clearItemAt(int index) {
      Setting<ItemBindSetting> setting = this.itemSettings.get(index);
      ItemBindSetting current = setting.getValue();
      setting.setValue(new ItemBindSetting("", current.getBind()));
   }

   public void resetAll() {
      for (Setting<ItemBindSetting> setting : this.itemSettings) {
         setting.setValue(new ItemBindSetting());
      }
   }

   private boolean useItem(ItemBindSetting setting) {
      if (this.scheduledUse) {
         return true;
      }

      Item item = setting.getItem();
      if (item != null && item != Items.AIR) {
         SearchInvResult hotbar = InventoryUtility.findInHotBar(stack -> stack.getItem() == item);
         SearchInvResult inv = hotbar.found() ? SearchInvResult.notFound() : InventoryUtility.findInInventory(stack -> stack.getItem() == item);
         if (!hotbar.found() && !inv.found()) {
            return false;
         }

         this.use(hotbar, inv);
         return true;
      } else {
         return false;
      }
   }

   private void use(SearchInvResult result, SearchInvResult invResult) {
      float[] rotation = this.getUseRotation();
      if (result.found()) {
         InventoryUtility.saveAndSwitchTo(result.slot());
         this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, rotation[0], rotation[1]));
         InventoryUtility.returnSlot();
      } else if (invResult.found()) {
         int targetHotbarSlot = mc.player.getInventory().getSelectedSlot();
         if (this.grimStop.getValue() && MovementUtility.isMoving()) {
            this.scheduleInventoryUse(invResult.slot(), targetHotbarSlot, 5L);
            return;
         }

         this.useInventoryItem(invResult.slot(), targetHotbarSlot, rotation);
      }
   }

   private void scheduleInventoryUse(int inventorySlot, int hotbarSlot, long delayMs) {
      if (inventorySlot != -1) {
         try {
            InputBlocker.block();
         } catch (Throwable var6) {
         }

         this.scheduledUse = true;
         this.scheduledInventorySlot = inventorySlot;
         this.scheduledHotbarSlot = hotbarSlot;
         this.scheduledUseAt = System.currentTimeMillis() + delayMs;
      }
   }

   private void handleScheduledUse() {
      if (this.scheduledUse && System.currentTimeMillis() >= this.scheduledUseAt) {
         try {
            if (this.scheduledInventorySlot != -1) {
               float[] rotation = this.getUseRotation();
               this.useInventoryItem(this.scheduledInventorySlot, this.scheduledHotbarSlot, rotation);
            }
         } catch (Throwable var3) {
         }

         try {
            this.resetScheduledUse();
         } catch (Throwable var2) {
         }
      }
   }

   private void useInventoryItem(int inventorySlot, int hotbarSlot, float[] rotation) {
      if (inventorySlot != -1 && mc.player != null && mc.gameMode != null) {
         clickSlot(inventorySlot, hotbarSlot, ClickType.SWAP);
         this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, rotation[0], rotation[1]));
         clickSlot(inventorySlot, hotbarSlot, ClickType.SWAP);
         this.sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
      }
   }

   private void resetScheduledUse() {
      this.scheduledUse = false;
      this.scheduledInventorySlot = -1;
      this.scheduledHotbarSlot = -1;
      this.scheduledUseAt = -1L;

      try {
         InputBlocker.unblock();
      } catch (Throwable var2) {
      }
   }

   private float[] getUseRotation() {
      float yaw = mc.player.getYRot();
      float pitch = mc.player.getXRot();
      if (ModuleManager.aura.isEnabled() && Aura.target != null) {
         yaw = ModuleManager.aura.rotationYaw;
         pitch = ModuleManager.aura.rotationPitch;
      }

      return new float[]{yaw, pitch};
   }
}
