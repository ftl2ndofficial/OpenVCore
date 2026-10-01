package vcore.features.modules.movement;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import vcore.events.impl.EventClickSlot;
import vcore.events.impl.EventKeyboardInput;
import vcore.events.impl.EventPostSync;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.TimerUtil;
import vcore.utility.player.MovementUtility;

public class GuiMove extends Module {
   private final Setting<GuiMove.Bypass> clickBypass = new Setting<>("Bypass", GuiMove.Bypass.None);
   private final Queue<ServerboundContainerClickPacket> grimPacketQueue = new ConcurrentLinkedQueue<>();
   private final Queue<ServerboundContainerClickPacket> grimOldPacketQueue = new ConcurrentLinkedQueue<>();
   private final Set<ServerboundContainerClickPacket> sendingPackets = new HashSet<>();
   private final Set<ServerboundContainerClickPacket> grimOldSendingPackets = new HashSet<>();
   private final TimerUtil grimOldTimer = new TimerUtil();
   private ServerboundContainerClosePacket pendingClosePacket;
   private boolean wasGrimOldInventoryOpen;
   private boolean stopInputQueued;
   private boolean stopInputApplied;
   private boolean sendingClosePacket;

   public GuiMove() {
      super("GuiMove", "Move while in GUI.", Module.Category.MOVEMENT);
   }

   @Override
   public void onUpdate() {
      KeyMapping[] keys = this.getMovementKeys();
      if (mc.screen instanceof ChatScreen || mc.screen instanceof SignEditScreen) {
         for (KeyMapping key : keys) {
            key.setDown(false);
         }
      } else if (this.isGrimOld()) {
         this.clearGrimState();
         this.handleGrimOldUpdate(keys);
      } else {
         this.clearGrimOldState();
         this.updateKeyBindingState(keys);
         if (this.isGrim()) {
            this.queueStopInputBeforeGrimAction();
         } else {
            this.clearGrimState();
         }
      }
   }

   @Override
   public void onDisable() {
      this.clearGrimState();
      this.clearGrimOldState();
   }

   @EventHandler
   public void onClickSlot(EventClickSlot e) {
      if (this.clickBypass.is(GuiMove.Bypass.DisableClicks) && (MovementUtility.isMoving() || mc.options.keyJump.isDown())) {
         e.cancel();
      }
   }

   @EventHandler
   public void onKeyboardInput(EventKeyboardInput e) {
      if (this.isGrim() && this.stopInputQueued) {
         e.clearMovementInput();
         this.stopInputApplied = true;
      }
   }

   @EventHandler
   public void onPostSync(EventPostSync e) {
      if (this.isGrim() && this.stopInputQueued && this.stopInputApplied) {
         this.stopInputQueued = false;
         this.stopInputApplied = false;
         this.flushQueuedClicks();
         this.flushPendingClose();
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (this.isGrim() && e.getPacket() instanceof ServerboundContainerClosePacket closePacket) {
         if (this.sendingClosePacket) {
            this.sendingClosePacket = false;
            return;
         }

         if (mc.screen instanceof InventoryScreen && this.isGrimActionInputActive()) {
            this.pendingClosePacket = closePacket;
            e.cancel();
            return;
         }
      }

      if (this.isGrim() && e.getPacket() instanceof ServerboundContainerClickPacket click) {
         if (this.sendingPackets.remove(click)) {
            return;
         }

         if (click.containerId() == 0 && mc.screen instanceof InventoryScreen && this.isGrimActionInputActive()) {
            this.grimPacketQueue.add(click);
            e.cancel();
            return;
         }
      }

      if (this.isGrimOld() && e.getPacket() instanceof ServerboundContainerClickPacket click) {
         if (this.grimOldSendingPackets.remove(click)) {
            return;
         }

         if (click.containerId() == 0 && MovementUtility.isMoving() && mc.screen instanceof AbstractContainerScreen) {
            this.grimOldPacketQueue.add(click);
            e.cancel();
            return;
         }
      }

      if (MovementUtility.isMoving() && mc.options.keyJump.isDown()) {
         if (this.clickBypass.is(GuiMove.Bypass.DisableClicks) && e.getPacket() instanceof ServerboundContainerClickPacket) {
            e.cancel();
         }
      }
   }

   private void updateKeyBindingState(KeyMapping[] keys) {
      for (KeyMapping key : keys) {
         key.setDown(this.isKeyPressed(InputConstants.getKey(key.saveString()).getValue()));
      }
   }

   private void handleGrimOldUpdate(KeyMapping[] keys) {
      if (!this.grimOldPacketQueue.isEmpty() && !this.grimOldTimer.hasTimeElapsed(100L)) {
         this.setKeyBindingState(keys, false);
      } else {
         this.updateKeyBindingState(keys);
         boolean inventoryOpen = mc.screen instanceof InventoryScreen;
         if (inventoryOpen) {
            this.wasGrimOldInventoryOpen = true;
         } else if (this.wasGrimOldInventoryOpen) {
            this.sendQueuedGrimOldPackets();
            this.wasGrimOldInventoryOpen = false;
            this.grimOldTimer.reset();
         }
      }
   }

   private void setKeyBindingState(KeyMapping[] keys, boolean pressed) {
      for (KeyMapping key : keys) {
         key.setDown(pressed);
      }
   }

   private KeyMapping[] getMovementKeys() {
      return new KeyMapping[]{mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight, mc.options.keyJump, mc.options.keySprint};
   }

   private void queueStopInputBeforeGrimAction() {
      if (!this.grimPacketQueue.isEmpty() || this.pendingClosePacket != null) {
         if (!this.stopInputQueued) {
            this.stopInputQueued = true;
            this.stopInputApplied = false;
         }
      }
   }

   private void flushQueuedClicks() {
      if (mc.getConnection() != null) {
         while (!this.grimPacketQueue.isEmpty()) {
            ServerboundContainerClickPacket packet = this.grimPacketQueue.poll();
            if (packet != null) {
               this.sendingPackets.add(packet);
               mc.getConnection().send(packet);
            }
         }
      }
   }

   private void flushPendingClose() {
      if (mc.getConnection() != null && this.pendingClosePacket != null) {
         this.sendingClosePacket = true;
         mc.getConnection().send(this.pendingClosePacket);
         this.pendingClosePacket = null;
      }
   }

   private void clearGrimState() {
      this.grimPacketQueue.clear();
      this.sendingPackets.clear();
      this.pendingClosePacket = null;
      this.stopInputQueued = false;
      this.stopInputApplied = false;
      this.sendingClosePacket = false;
   }

   private void sendQueuedGrimOldPackets() {
      new Thread(() -> {
         try {
            Thread.sleep(80L);
            mc.execute(this::flushQueuedGrimOldPackets);
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
         }
      }, "VCore-GuiMove-GrimOld").start();
   }

   private void flushQueuedGrimOldPackets() {
      if (mc.getConnection() == null) {
         this.grimOldPacketQueue.clear();
         this.grimOldSendingPackets.clear();
      } else {
         while (!this.grimOldPacketQueue.isEmpty()) {
            ServerboundContainerClickPacket packet = this.grimOldPacketQueue.poll();
            if (packet != null) {
               this.grimOldSendingPackets.add(packet);
               mc.getConnection().send(packet);
            }
         }
      }
   }

   private void clearGrimOldState() {
      this.grimOldPacketQueue.clear();
      this.grimOldSendingPackets.clear();
      this.wasGrimOldInventoryOpen = false;
   }

   private boolean isGrim() {
      return this.clickBypass.is(GuiMove.Bypass.Grim);
   }

   private boolean isGrimOld() {
      return this.clickBypass.is(GuiMove.Bypass.GrimOld);
   }

   private boolean isGrimActionInputActive() {
      return mc.options.keyUp.isDown()
         || mc.options.keyDown.isDown()
         || mc.options.keyLeft.isDown()
         || mc.options.keyRight.isDown()
         || mc.options.keyJump.isDown()
         || mc.options.keyShift.isDown()
         || mc.options.keySprint.isDown()
         || mc.player != null && (mc.player.isSprinting() || mc.player.isShiftKeyDown());
   }

   public boolean shouldSuppressSprint() {
      return false;
   }

   private enum Bypass {
      None("None"),
      Grim("Grim"),
      GrimOld("Grim Old"),
      DisableClicks("DisableClicks");

      private final String name;

      Bypass(String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }
}
