package vcore.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import vcore.core.InputBlocker;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.utility.player.InteractionUtility;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.MovementUtility;
import vcore.utility.player.PlayerUtility;
import vcore.utility.player.SearchInvResult;

public class ClickAction extends Module {
   private final Setting<Boolean> pearlElement = new Setting<>("Pearl", false);
   private final Setting<Boolean> xpElement = new Setting<>("XP", false);
   private final Setting<Boolean> friendElement = new Setting<>("Friend", false);
   private final Setting<ClickAction.Mode> mode = new Setting<>("Mode", ClickAction.Mode.Legit, v -> this.pearlElement.getValue());
   private final Setting<Bind> pearlBind = new Setting<>("Pearl Key", new Bind(-1, false, false), v -> this.pearlElement.getValue());
   private final Setting<Bind> xpBind = new Setting<>("XP Key", new Bind(-1, false, false), v -> this.xpElement.getValue());
   private final Setting<Bind> friendBind = new Setting<>("Friend Key", new Bind(-1, false, false), v -> this.friendElement.getValue());
   private final Setting<BooleanSettingGroup> xpAntiWaste = new Setting<>("AntiWaste", new BooleanSettingGroup(false), v -> this.xpElement.getValue());
   private final Setting<Integer> xpStopOn = new Setting<>("Stop On", 95, 0, 100, v -> this.xpElement.getValue()).addToGroup(this.xpAntiWaste);
   private final Setting<BooleanSettingGroup> xpFeetGroup = new Setting<>("Rotation", new BooleanSettingGroup(false), v -> this.xpElement.getValue());
   private final Setting<Float> xpFeetPitch = new Setting<>("Pitch", 70.0F, 45.0F, 90.0F, v -> this.xpElement.getValue()).addToGroup(this.xpFeetGroup);
   private boolean pearlPressed;
   private boolean friendPressed;
   private boolean xpPressed;
   private boolean pearlRequested;
   private boolean friendRequested;
   private boolean scheduledPearlSwap;
   private int scheduledInventorySlot = -1;
   private int scheduledHotbarSlot = -1;
   private long scheduledActionAt = -1L;
   private int restoreSlot = -1;
   private int restoreHotbar = -1;
   private long restoreAt = -1L;
   private long lastWarn = -1L;
   private boolean isThrowingXp;
   private int xpLastSlot = -1;
   private long lastXpUseTime = 0L;

   public ClickAction() {
      super("ClickAction", "Pearl throwing with hotkeys.", Module.Category.PLAYER);
   }

   @Override
   public void onEnable() {
      this.resetState();
      if (this.pearlBind.getValue() != null && this.pearlBind.getValue().isMouse() && this.pearlBind.getValue().getKey() == 2) {
         ModuleManager.keyPearlAntiPickup = true;
      }
   }

   @Override
   public void onDisable() {
      this.resetState();
      ModuleManager.keyPearlAntiPickup = false;
   }

   @Override
   public void onUpdate() {
      if (!fullNullCheck()) {
         if (this.pearlBind.getValue() != null && this.pearlBind.getValue().isMouse() && this.pearlBind.getValue().getKey() == 2) {
            ModuleManager.keyPearlAntiPickup = true;
         } else {
            ModuleManager.keyPearlAntiPickup = false;
         }

         this.pollKeys();
         if (this.friendRequested) {
            this.handleFriendAction();
            this.friendRequested = false;
         }

         if (this.pearlRequested) {
            this.handlePearlAction();
            this.pearlRequested = false;
         }

         this.handleScheduledPearlSwap();
         this.handleLegitRestore();
         this.handleXpAutomation();
      }
   }

   private void pollKeys() {
      boolean guiOpen = mc.screen != null;
      if (this.pearlElement.getValue()) {
         boolean pressed = this.isKeyPressed(this.pearlBind);
         if (!guiOpen && pressed && !this.pearlPressed) {
            this.pearlRequested = true;
         }

         this.pearlPressed = pressed;
      } else {
         this.pearlPressed = false;
      }

      if (this.friendElement.getValue()) {
         boolean pressed = this.isKeyPressed(this.friendBind);
         if (!guiOpen && pressed && !this.friendPressed) {
            this.friendRequested = true;
         }

         this.friendPressed = pressed;
      } else {
         this.friendPressed = false;
      }

      if (this.xpElement.getValue()) {
         this.xpPressed = this.isKeyPressed(this.xpBind);
      } else {
         this.xpPressed = false;
      }
   }

   private void handleFriendAction() {
      if (!fullNullCheck()) {
         if (!(mc.hitResult instanceof EntityHitResult hit && hit.getType() == Type.ENTITY)) {
            this.warn("Aim at a player first.");
         } else if (hit.getEntity() instanceof Player player && player != mc.player) {
            String name = player.getName().getString();
            if (Managers.FRIEND.isFriend(name)) {
               Managers.FRIEND.removeFriend(name);
               this.sendMessage("Removed friend: " + name);
            } else {
               Managers.FRIEND.addFriend(name);
               this.sendMessage("Added friend: " + name);
            }
         } else {
            this.warn("That isn't a player.");
         }
      }
   }

   private void handlePearlAction() {
      if (!fullNullCheck() && !mc.player.getCooldowns().isOnCooldown(Items.ENDER_PEARL.getDefaultInstance())) {
         SearchInvResult hotbarPearl = InventoryUtility.findItemInHotBar(Items.ENDER_PEARL);
         SearchInvResult invPearl = hotbarPearl.found() ? SearchInvResult.notFound() : InventoryUtility.findItemInInventory(Items.ENDER_PEARL);
         if (!hotbarPearl.found() && !invPearl.found()) {
            this.warn("No pearls found.");
         } else if (this.mode.is(ClickAction.Mode.Rage)) {
            if (hotbarPearl.found()) {
               this.throwPearlFromHotbar(hotbarPearl.slot(), true);
            } else {
               this.throwPearlWithSwap(invPearl.slot(), mc.player.getInventory().getSelectedSlot(), false);
            }
         } else if (hotbarPearl.found()) {
            this.throwPearlFromHotbar(hotbarPearl.slot(), false);
         } else if (this.restoreSlot != -1) {
            this.warn("Waiting for previous swap to finish.");
         } else {
            int targetHotbar = this.pickSwapHotbarSlot();
            if (MovementUtility.isMoving()) {
               InputBlocker.block();
               this.schedulePearlSwap(invPearl.slot(), targetHotbar, 5L);
            } else {
               this.throwPearlWithSwap(invPearl.slot(), targetHotbar, true);
            }
         }
      }
   }

   private void schedulePearlSwap(int inventorySlot, int hotbarSlot, long delayMs) {
      if (inventorySlot != -1) {
         this.scheduledPearlSwap = true;
         this.scheduledInventorySlot = inventorySlot;
         this.scheduledHotbarSlot = hotbarSlot;
         this.scheduledActionAt = System.currentTimeMillis() + delayMs;
      }
   }

   private void handleScheduledPearlSwap() {
      if (this.scheduledPearlSwap) {
         if (System.currentTimeMillis() >= this.scheduledActionAt) {
            try {
               if (this.scheduledInventorySlot != -1) {
                  clickSlot(this.scheduledInventorySlot, this.scheduledHotbarSlot, ClickType.SWAP);
                  this.throwPearlFromHotbar(this.scheduledHotbarSlot, false);
                  clickSlot(this.scheduledInventorySlot, this.scheduledHotbarSlot, ClickType.SWAP);
               }
            } catch (Throwable var3) {
            }

            this.scheduledPearlSwap = false;
            this.scheduledInventorySlot = -1;
            this.scheduledHotbarSlot = -1;
            this.scheduledActionAt = -1L;

            try {
               InputBlocker.unblock();
            } catch (Throwable var2) {
            }
         }
      }
   }

   private void throwPearlWithSwap(int inventorySlot, int hotbarSlot, boolean scheduleRestore) {
      if (inventorySlot != -1) {
         clickSlot(inventorySlot, hotbarSlot, ClickType.SWAP);
         this.throwPearlFromHotbar(hotbarSlot, this.mode.is(ClickAction.Mode.Rage));
         if (this.mode.is(ClickAction.Mode.Rage) && !scheduleRestore) {
            clickSlot(inventorySlot, hotbarSlot, ClickType.SWAP);
         } else {
            this.restoreSlot = inventorySlot;
            this.restoreHotbar = hotbarSlot;
            this.restoreAt = System.currentTimeMillis();
         }
      }
   }

   private void throwPearlFromHotbar(int hotbarSlot, boolean rageMode) {
      if (rageMode) {
         InventoryUtility.saveSlot();
         InventoryUtility.switchTo(hotbarSlot);
         this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
         mc.player.swing(InteractionHand.MAIN_HAND);
         InventoryUtility.returnSlot();
      } else {
         int previousSlot = mc.player.getInventory().getSelectedSlot();
         mc.player.getInventory().setSelectedSlot(hotbarSlot);
         mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         mc.player.swing(InteractionHand.MAIN_HAND);
         mc.player.getInventory().setSelectedSlot(previousSlot);
      }
   }

   private void handleLegitRestore() {
      if (this.mode.is(ClickAction.Mode.Legit) && this.restoreAt >= 0L) {
         if (System.currentTimeMillis() >= this.restoreAt) {
            if (this.restoreSlot != -1) {
               if (MovementUtility.isMoving()) {
                  return;
               }

               clickSlot(this.restoreSlot, this.restoreHotbar, ClickType.SWAP);
            }

            this.restoreSlot = -1;
            this.restoreHotbar = -1;
            this.restoreAt = -1L;

            try {
               InputBlocker.unblock();
            } catch (Throwable var2) {
            }

            this.stopXpUse();
            this.xpPressed = this.xpElement.getValue() && this.isKeyPressed(this.xpBind);
            this.lastXpUseTime = 0L;
         }
      }
   }

   private void handleXpAutomation() {
      if (!this.xpElement.getValue() || !this.xpPressed || mc.screen != null || mc.player == null) {
         this.stopXpUse();
      } else if (this.xpAntiWaste.getValue().isEnabled() && !this.needXpRepair()) {
         this.stopXpUse();
      } else if (!this.hasAvailableXpBottle()) {
         this.stopXpUse();
      } else {
         if (!this.isThrowingXp) {
            this.xpLastSlot = mc.player.getInventory().getSelectedSlot();
            this.isThrowingXp = true;
            this.lastXpUseTime = 0L;
         }

         long now = System.currentTimeMillis();
         int delay = 20;
         if (now - this.lastXpUseTime >= delay) {
            float throwPitch = this.getXpThrowPitch();
            float throwYaw = mc.player.getYRot();
            if (mc.player.getOffhandItem().getItem() == Items.EXPERIENCE_BOTTLE) {
               InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, throwYaw, throwPitch));
               mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.OFF_HAND));
            } else {
               int xpSlot = InventoryUtility.findItemInHotBar(Items.EXPERIENCE_BOTTLE).slot();
               if (xpSlot == -1) {
                  this.stopXpUse();
                  return;
               }

               if (xpSlot != mc.player.getInventory().getSelectedSlot()) {
                  mc.player.getInventory().setSelectedSlot(xpSlot);
               }

               InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, throwYaw, throwPitch));
               mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }

            this.lastXpUseTime = now;
         }
      }
   }

   private boolean needXpRepair() {
      int stopOn = this.xpStopOn.getValue();

      for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
         ItemStack stack = mc.player.getItemBySlot(slot);
         float percent = PlayerUtility.calculatePercentage(stack);
         if (percent < stopOn) {
            return true;
         }
      }

      return false;
   }

   private void stopXpUse() {
      if (mc.player != null && this.isThrowingXp && this.xpLastSlot != -1 && this.xpLastSlot != mc.player.getInventory().getSelectedSlot()) {
         mc.player.getInventory().setSelectedSlot(this.xpLastSlot);
      }

      this.isThrowingXp = false;
      this.xpLastSlot = -1;
      this.lastXpUseTime = 0L;
   }

   @EventHandler(priority = -100)
   private void onSync(EventSync event) {
      if (!fullNullCheck()) {
         if (this.shouldSpoofFeetPitch()) {
            mc.player.setXRot(this.xpFeetPitch.getValue());
         }
      }
   }

   private boolean shouldSpoofFeetPitch() {
      if (!this.xpElement.getValue() || !this.xpFeetGroup.getValue().isEnabled()) {
         return false;
      } else if (!this.xpPressed || mc.screen != null || mc.player == null) {
         return false;
      } else {
         return this.xpAntiWaste.getValue().isEnabled() && !this.needXpRepair() ? false : this.hasAvailableXpBottle();
      }
   }

   private int pickSwapHotbarSlot() {
      int selected = mc.player.getInventory().getSelectedSlot();
      return (selected + 1) % 9;
   }

   private void warn(String message) {
      long now = System.currentTimeMillis();
      if (now - this.lastWarn > 750L) {
         this.sendMessage(message);
         this.lastWarn = now;
      }
   }

   private float getXpThrowPitch() {
      return this.shouldSpoofFeetPitch() ? this.xpFeetPitch.getValue() : mc.player.getXRot();
   }

   private boolean hasAvailableXpBottle() {
      return mc.player != null
         && (mc.player.getOffhandItem().getItem() == Items.EXPERIENCE_BOTTLE || InventoryUtility.findItemInHotBar(Items.EXPERIENCE_BOTTLE).found());
   }

   private void resetState() {
      this.pearlPressed = this.pearlElement.getValue() && this.isKeyPressed(this.pearlBind);
      this.friendPressed = this.friendElement.getValue() && this.isKeyPressed(this.friendBind);
      this.pearlRequested = false;
      this.friendRequested = false;
      this.restoreSlot = -1;
      this.restoreHotbar = -1;
      this.restoreAt = -1L;
      if (this.isEnabled() && this.pearlBind.getValue() != null && this.pearlBind.getValue().isMouse() && this.pearlBind.getValue().getKey() == 2) {
         ModuleManager.keyPearlAntiPickup = true;
      } else if (!this.isEnabled()) {
         ModuleManager.keyPearlAntiPickup = false;
      }
   }

   private enum Mode {
      Legit,
      Rage;
   }
}
