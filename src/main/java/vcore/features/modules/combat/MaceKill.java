package vcore.features.modules.combat;

import java.util.List;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;

public class MaceKill extends Module {
   private final Setting<Integer> fallHeight = new Setting<>("FallHeight", 22, 1, 1000);
   private final Setting<Boolean> mineRuaBypass = new Setting<>("MineRua Bypass", false);
   private static final long MINERUA_SPOOF_SYNC_BLOCK_MS = 1000L;
   private static final int PLAYER_CHEST_SCREEN_SLOT = 6;
   private static final int MINERUA_SPOOF_REPEAT_COUNT = 2;
   private boolean sendingDelayedAttack;
   private long mineRuaSpoofSyncBlockUntil;

   public MaceKill() {
      super("MaceKill", "Makes the mace powerful by faking fall height.", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.sendingDelayedAttack = false;
      this.mineRuaSpoofSyncBlockUntil = 0L;
   }

   @Override
   public void onLogout() {
      this.sendingDelayedAttack = false;
      this.mineRuaSpoofSyncBlockUntil = 0L;
   }

   @EventHandler
   private void onPacketSend(PacketEvent.Send event) {
      if (!fullNullCheck() && !this.sendingDelayedAttack) {
         if (event.getPacket() instanceof ServerboundInteractPacket packet) {
            if (Criticals.getInteractType(packet) == Criticals.InteractType.ATTACK) {
               if (this.isHoldingMace()) {
                  int height = this.determineHeight();
                  if (!this.mineRuaBypass.getValue()) {
                     this.spoofLiquidFallHeight(height);
                  } else {
                     event.cancel();
                     if (Criticals.getEntity(packet) instanceof LivingEntity living) {
                        this.performMineRuaBypassAttack(living, packet.isUsingSecondaryAction(), height);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (!fullNullCheck() && this.mineRuaBypass.getValue()) {
         if (this.isBlockingMineRuaSpoofSync()) {
            if (event.getPacket() instanceof ClientboundSetEntityDataPacket packet) {
               if (packet.id() == mc.player.getId()) {
                  if (this.containsLocalStateSync(packet.packedItems())) {
                     event.cancel();
                  }
               }
            }
         }
      }
   }

   private void spoofLiquidFallHeight(int height) {
      if (mc.player != null) {
         if (height > 10) {
            int repeats = (int)Math.ceil(Math.abs(height / 10.0));

            for (int i = 0; i < repeats; i++) {
               this.warp(null, false);
            }
         } else {
            for (int i = 0; i < 2; i++) {
               this.warp(null, mc.player.onGround());
            }
         }

         Vec3 playerPos = mc.player.position();
         this.warp(playerPos.add(0.0, height, 0.0), false);
         this.warp(playerPos, false);
      }
   }

   private int determineHeight() {
      if (mc.player != null && mc.level != null) {
         AABB boundingBox = mc.player.getBoundingBox();

         for (int i = this.fallHeight.getValue(); i >= 1; i--) {
            AABB movedBox = boundingBox.move(0.0, i, 0.0);
            if (!mc.level.getBlockCollisions(mc.player, movedBox).iterator().hasNext()) {
               return i;
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private void warp(Vec3 pos, boolean onGround) {
      if (mc.player != null) {
         if (pos == null) {
            this.sendPacket(new StatusOnly(onGround, mc.player.horizontalCollision));
         } else {
            this.sendPacket(new Pos(pos.x, pos.y, pos.z, onGround, mc.player.horizontalCollision));
         }
      }
   }

   private void performMineRuaBypassAttack(LivingEntity livingEntity, boolean sneaking, int height) {
      if (mc.player != null && mc.level != null && livingEntity.isAlive()) {
         if (this.ensureWearingElytra()) {
            this.sendAirborneStatePacket();
            this.blockMineRuaSpoofClientSync();

            for (int i = 0; i < 2; i++) {
               if (!this.performMineRuaStateCycle(i == 0)) {
                  return;
               }
            }

            this.sendFakeFallAttack(livingEntity, sneaking, height);
         }
      }
   }

   private boolean performMineRuaStateCycle(boolean useStartFirework) {
      if (!this.isWearingElytra()) {
         return false;
      }

      this.sendElytraFlyingPacket();
      if (useStartFirework) {
         this.useStartFireworkMainHandOnly();
      }

      this.sendCancelElytraFlyingPacket();
      return true;
   }

   private void sendElytraFlyingPacket() {
      if (mc.player != null) {
         this.sendPacketSilent(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
      }
   }

   private void sendCancelElytraFlyingPacket() {
      if (mc.player != null) {
         Vec3 pos = mc.player.position();
         this.sendPacketSilent(new Pos(pos.x, pos.y, pos.z, true, mc.player.horizontalCollision));
      }
   }

   private void sendFakeFallAttack(LivingEntity livingEntity, boolean sneaking, int height) {
      if (mc.player != null && mc.level != null && livingEntity.isAlive() && this.isHoldingMace()) {
         this.sendingDelayedAttack = true;

         try {
            this.spoofLiquidFallHeight(height);
            this.sendPacket(ServerboundInteractPacket.createAttackPacket(livingEntity, sneaking));
            this.restoreServerElytraFlyingState();
         } finally {
            this.sendingDelayedAttack = false;
         }
      }
   }

   private void sendAirborneStatePacket() {
      if (mc.player != null) {
         Vec3 pos = mc.player.position();
         this.sendPacketSilent(new Pos(pos.x, pos.y, pos.z, false, mc.player.horizontalCollision));
      }
   }

   private void restoreServerElytraFlyingState() {
      if (mc.player != null && mc.player.isFallFlying() && this.isWearingElytra()) {
         this.sendAirborneStatePacket();
         this.sendElytraFlyingPacket();
      }
   }

   private boolean ensureWearingElytra() {
      if (mc.player == null) {
         return false;
      } else if (this.isWearingElytra()) {
         return true;
      } else {
         SearchInvResult elytra = InventoryUtility.findItemInInventory(Items.ELYTRA);
         if (!elytra.found()) {
            return false;
         } else if (mc.screen == null && mc.player.containerMenu == mc.player.inventoryMenu) {
            clickSlot(elytra.slot());
            clickSlot(6);
            clickSlot(elytra.slot());
            return this.isWearingElytra();
         } else {
            return false;
         }
      }
   }

   private void useStartFireworkMainHandOnly() {
      if (ModuleManager.elytraSwap != null) {
         ModuleManager.elytraSwap.useStartFireworkMainHandOnly();
      }
   }

   private void blockMineRuaSpoofClientSync() {
      this.mineRuaSpoofSyncBlockUntil = System.currentTimeMillis() + 1000L;
   }

   private boolean isBlockingMineRuaSpoofSync() {
      return System.currentTimeMillis() <= this.mineRuaSpoofSyncBlockUntil;
   }

   private boolean containsLocalStateSync(List<DataValue<?>> values) {
      if (values.isEmpty()) {
         return false;
      }

      for (DataValue<?> value : values) {
         if (value.id() == 0) {
            return true;
         }

         if ("FALL_FLYING".equals(String.valueOf(value.value()))) {
            return true;
         }
      }

      return false;
   }

   private boolean isHoldingMace() {
      return mc.player != null && mc.player.getMainHandItem().is(Items.MACE);
   }

   private boolean isWearingElytra() {
      return mc.player != null && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
   }
}
