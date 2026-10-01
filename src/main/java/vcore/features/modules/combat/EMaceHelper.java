package vcore.features.modules.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.misc.ClientSettings;
import vcore.features.modules.player.ElytraSwap;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;

public class EMaceHelper extends Module {
   public final Setting<Boolean> autoTarget = new Setting<>("Auto Target", false);
   public final Setting<Boolean> peakAssist = new Setting<>("PeakAssist", false);
   public final Setting<Integer> height = new Setting<>("Height", 100, 25, 1000, v -> this.peakAssist.getValue());
   public final Setting<Boolean> armorSwap = new Setting<>("Armor Swap", false);
   public final Setting<Boolean> autoFireWork = new Setting<>("FireWork Vector", false);
   public final Setting<Boolean> extraAimRange = new Setting<>("Extra AimRange", false);
   public final Setting<Integer> swapDistance = new Setting<>("Swap Distance", 10, 5, 16, v -> this.armorSwap.getValue());
   public final Setting<Integer> fireWorkDistance = new Setting<>("FireWork Distance", 30, 16, 64, v -> this.autoFireWork.getValue());
   public final Setting<Integer> aimDistance = new Setting<>("Aim Distance", 900, 800, 1000, v -> this.extraAimRange.getValue());
   private boolean swapping;
   private boolean fireworkTriggered;
   private boolean peakOffsetActive = true;
   private boolean peakOffsetDisabledPendingFirework;
   private static volatile boolean auraPostAttackTriggered;
   private static volatile boolean peakAssistPostAttackTriggered;
   private final Timer peakAssistFireworkTimer = new Timer();

   public EMaceHelper() {
      super("EMaceHelper", "Elytra mace helper automation.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.swapping = false;
      this.resetFireworkFlags();
      this.resetPeakAssistState();
   }

   @Override
   public void onDisable() {
      this.swapping = false;
      this.resetFireworkFlags();
      this.resetPeakAssistState();
   }

   @Override
   public void onUpdate() {
      if (mc.player != null && mc.level != null) {
         if (this.peakAssist.getValue()) {
            this.handlePeakAssist();
         } else {
            this.resetPeakAssistState();
         }

         if (this.armorSwap.getValue()) {
            this.handleArmorSwap();
         }

         if (this.autoFireWork.getValue()) {
            this.handleAutoFireWork();
         } else {
            this.resetFireworkFlags();
         }
      }
   }

   public boolean shouldSkipAuraTarget(Entity target) {
      if (target == null || target.isRemoved()) {
         return false;
      }

      if (!this.canUseAutoTarget()) {
         return false;
      }

      if (target instanceof LivingEntity livingTarget) {
         if (livingTarget.isFallFlying()) {
            return true;
         } else if (target instanceof Player player && player.getArmorValue() == 0) {
            return true;
         } else {
            return livingTarget.isSwimming() && this.isEyeInWater(livingTarget) ? true : this.isTargetBlockedBySolidBlock(target);
         }
      } else {
         return false;
      }
   }

   private boolean canUseAutoTarget() {
      if (mc.player == null || mc.level == null) {
         return false;
      } else if (!this.isEnabled()) {
         return false;
      } else {
         return !this.autoTarget.getValue() ? false : this.canUseMaceAuraSupport();
      }
   }

   private boolean isEyeInWater(LivingEntity livingTarget) {
      return mc.level.getFluidState(BlockPos.containing(livingTarget.getEyePosition())).is(FluidTags.WATER);
   }

   private boolean isTargetBlockedBySolidBlock(Entity target) {
      Vec3 start = mc.player.getEyePosition();
      Vec3 end = this.getClosestVisiblePoint(target);
      Vec3 direction = end.subtract(start);
      if (direction.lengthSqr() < 1.0E-6) {
         return false;
      }

      Vec3 step = direction.normalize().scale(0.05);

      for (int attempts = 0; attempts < 8; attempts++) {
         BlockHitResult hit = mc.level.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, mc.player));
         if (hit.getType() != Type.BLOCK) {
            return false;
         }

         if (!this.isGrassBlock(hit)) {
            return true;
         }

         start = hit.getLocation().add(step);
      }

      return false;
   }

   private Vec3 getClosestVisiblePoint(Entity target) {
      Vec3 eyePos = mc.player.getEyePosition();
      return new Vec3(
         Mth.clamp(eyePos.x, target.getBoundingBox().minX, target.getBoundingBox().maxX),
         Mth.clamp(eyePos.y, target.getBoundingBox().minY, target.getBoundingBox().maxY),
         Mth.clamp(eyePos.z, target.getBoundingBox().minZ, target.getBoundingBox().maxZ)
      );
   }

   private boolean isGrassBlock(BlockHitResult hit) {
      BlockState state = mc.level.getBlockState(hit.getBlockPos());
      return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS);
   }

   public Vec3 applyPeakAssistAimOffset(Entity target, Vec3 aimPos) {
      if (aimPos == null || target == null || target.isRemoved()) {
         return aimPos;
      } else if (!this.canUsePeakAssist()) {
         return aimPos;
      } else if (!this.peakOffsetActive) {
         return aimPos;
      } else if (Aura.target != target) {
         return aimPos;
      } else {
         Vec3 offsetPos = aimPos.add(0.0, this.height.getValue().intValue(), 0.0);
         if (this.shouldDisablePeakOffset(offsetPos)) {
            this.peakOffsetActive = false;
            this.peakOffsetDisabledPendingFirework = true;
            return aimPos;
         } else {
            return offsetPos;
         }
      }
   }

   private void handlePeakAssist() {
      if (!this.canUsePeakAssist()) {
         this.resetPeakAssistState();
      } else {
         if (peakAssistPostAttackTriggered) {
            peakAssistPostAttackTriggered = false;
            this.peakOffsetActive = true;
            this.peakAssistFireworkTimer.reset();
         }

         Entity target = Aura.target;
         if (target != null && !target.isRemoved()) {
            Vec3 offsetPos = this.getPeakAssistOffsetPos(target);
            if (offsetPos != null) {
               if (this.peakOffsetActive && this.shouldDisablePeakOffset(offsetPos)) {
                  this.peakOffsetActive = false;
                  this.peakOffsetDisabledPendingFirework = true;
               }

               if (this.peakOffsetActive) {
                  if (this.peakAssistFireworkTimer.every(500L)) {
                     this.triggerPeakAssistFirework();
                  }
               } else {
                  if (this.peakOffsetDisabledPendingFirework) {
                     this.triggerPeakAssistFirework();
                     this.peakOffsetDisabledPendingFirework = false;
                  }
               }
            }
         } else {
            this.peakOffsetDisabledPendingFirework = false;
         }
      }
   }

   private boolean canUsePeakAssist() {
      if (mc.player == null || mc.level == null) {
         return false;
      } else if (!this.isEnabled()) {
         return false;
      } else if (!this.peakAssist.getValue()) {
         return false;
      } else {
         return !mc.player.isFallFlying() ? false : this.canUseMaceAuraSupport();
      }
   }

   private Vec3 getPeakAssistOffsetPos(Entity target) {
      Vec3 baseAimPos;
      if (target instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
         baseAimPos = ModuleManager.elytraTarget.getTargetVec(livingTarget, true);
      } else {
         baseAimPos = target.getEyePosition();
      }

      return baseAimPos == null ? null : baseAimPos.add(0.0, this.height.getValue().intValue(), 0.0);
   }

   private boolean shouldDisablePeakOffset(Vec3 offsetPos) {
      return mc.player.getY() > offsetPos.y ? true : mc.player.position().distanceToSqr(offsetPos) < 1.0;
   }

   private void triggerPeakAssistFirework() {
      if (this.hasFirework()) {
         if (!mc.player.getCooldowns().isOnCooldown(Items.FIREWORK_ROCKET.getDefaultInstance())) {
            this.useFireWorkByMode();
         }
      }
   }

   private void resetPeakAssistState() {
      this.peakOffsetActive = true;
      this.peakOffsetDisabledPendingFirework = false;
      peakAssistPostAttackTriggered = false;
      this.peakAssistFireworkTimer.reset();
   }

   private boolean isPeakOffsetRunning() {
      return this.canUsePeakAssist() && this.peakOffsetActive;
   }

   public boolean isPeakAssistActive() {
      return this.isPeakOffsetRunning();
   }

   private void handleArmorSwap() {
      if (!this.isPeakOffsetRunning()) {
         if (mc.screen == null && !this.swapping) {
            if (this.canUseMaceAuraSupport()) {
               if (ModuleManager.maceSwap.isEnabled()) {
                  if (ModuleManager.maceSwap.hasMaceInHotbar()) {
                     Entity target = Aura.target;
                     if (target != null && !target.isRemoved()) {
                        if (this.isWearingElytra()) {
                           if (mc.player.isFallFlying()) {
                              if (!(mc.player.getY() <= target.getY() + 5.0)) {
                                 double distance = this.getArmorSwapDistance(target);
                                 if (!(distance > this.swapDistance.getValue().intValue())) {
                                    this.swapToChestplate();
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void swapToChestplate() {
      int slot = ElytraSwap.getChestPlateSlot();
      if (slot != -1) {
         this.swapping = true;

         try {
            clickSlot(slot);
            clickSlot(6);
            clickSlot(slot);
            this.sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
         } finally {
            this.swapping = false;
         }
      }
   }

   private double getArmorSwapDistance(Entity target) {
      if (target instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
         Vec3 predictedPos = ModuleManager.elytraTarget.getTargetVec(livingTarget, true);
         if (predictedPos != null) {
            return mc.player.position().distanceTo(predictedPos);
         }
      }

      return mc.player.position().distanceTo(target.position());
   }

   private void handleAutoFireWork() {
      Entity target = Aura.target;
      if (target != null && !target.isRemoved()) {
         boolean wearingElytra = mc.player.getInventory().getItem(38).getItem() == Items.ELYTRA;
         boolean isFallFlying = mc.player.isFallFlying();
         double distance = mc.player.position().distanceTo(target.position());
         if (this.shouldResetFirework(wearingElytra, isFallFlying, distance)) {
            this.resetFireworkFlags();
         } else if (this.canUseMaceAuraSupport()) {
            if (wearingElytra) {
               if (isFallFlying) {
                  if (this.hasFirework()) {
                     boolean holdingMace = this.isMace(mc.player.getMainHandItem());
                     if (!holdingMace) {
                        if (!ModuleManager.maceSwap.isEnabled()) {
                           return;
                        }

                        if (!ModuleManager.maceSwap.hasMaceInHotbar()) {
                           return;
                        }
                     }

                     if (!(mc.player.getY() <= target.getY() + 10.0)) {
                        if (!(distance > this.fireWorkDistance.getValue().intValue())) {
                           if (!this.fireworkTriggered && this.useFireWorkByMode()) {
                              this.fireworkTriggered = true;
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.resetFireworkFlags();
      }
   }

   private boolean shouldResetFirework(boolean wearingElytra, boolean isFallFlying, double distance) {
      return !wearingElytra || !isFallFlying || auraPostAttackTriggered || distance > this.fireWorkDistance.getValue().intValue();
   }

   private void resetFireworkFlags() {
      this.fireworkTriggered = false;
      auraPostAttackTriggered = false;
   }

   public float getElytraAimRangeOverride(float defaultRange) {
      return !this.shouldUseExtraAimRange() ? defaultRange : this.aimDistance.getValue().floatValue();
   }

   private boolean shouldUseExtraAimRange() {
      if (mc.player == null || mc.level == null) {
         return false;
      } else if (!this.isEnabled()) {
         return false;
      } else {
         return !this.extraAimRange.getValue() ? false : this.canUseMaceAuraSupport();
      }
   }

   private boolean canUseMaceAuraSupport() {
      if (mc.player == null || mc.level == null) {
         return false;
      } else {
         return !ModuleManager.aura.isEnabled()
            ? false
            : this.isMace(mc.player.getMainHandItem()) || ModuleManager.maceSwap.isEnabled() && ModuleManager.maceSwap.hasMaceInHotbar();
      }
   }

   private boolean hasFirework() {
      return mc.player.getOffhandItem().getItem() == Items.FIREWORK_ROCKET
         ? true
         : InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET).found() || InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET).found();
   }

   private boolean useFireWorkByMode() {
      return ClientSettings.fireWorkMode.is(ClientSettings.FireWorkMode.OffHand) ? this.useFireWorkOffhandSilent() : this.useFireWorkMainHandSilent();
   }

   private boolean useFireWorkMainHandSilent() {
      if (mc.player.getOffhandItem().getItem() == Items.FIREWORK_ROCKET) {
         this.useFireworkOffhandPacket();
         return true;
      }

      SearchInvResult hotbarFireWorkResult = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET);
      SearchInvResult fireWorkResult = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET);
      if (!hotbarFireWorkResult.found() && !fireWorkResult.found()) {
         return false;
      }

      InventoryUtility.saveSlot();
      boolean swappedFromInventory = false;
      if (hotbarFireWorkResult.found()) {
         hotbarFireWorkResult.switchTo();
      } else {
         if (mc.player.containerMenu != mc.player.inventoryMenu) {
            InventoryUtility.returnSlot();
            return false;
         }

         swappedFromInventory = true;
         mc.gameMode
            .handleInventoryMouseClick(mc.player.containerMenu.containerId, fireWorkResult.slot(), mc.player.getInventory().getSelectedSlot(), ClickType.SWAP, mc.player);
         this.sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
      }

      this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, ModuleManager.aura.rotationYaw, ModuleManager.aura.rotationPitch));
      this.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
      InventoryUtility.returnSlot();
      if (swappedFromInventory) {
         mc.gameMode
            .handleInventoryMouseClick(mc.player.containerMenu.containerId, fireWorkResult.slot(), mc.player.getInventory().getSelectedSlot(), ClickType.SWAP, mc.player);
         this.sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
      }

      return true;
   }

   private boolean useFireWorkOffhandSilent() {
      if (mc.player.getOffhandItem().getItem() == Items.FIREWORK_ROCKET) {
         this.useFireworkOffhandPacket();
         return true;
      }

      SearchInvResult fireWorkResult = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET);
      if (!fireWorkResult.found()) {
         return false;
      }

      if (mc.player.containerMenu != mc.player.inventoryMenu) {
         return false;
      }

      int slotIndex = convertFireworkSlot(fireWorkResult.slot());
      mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slotIndex, 40, ClickType.SWAP, mc.player);
      this.useFireworkOffhandPacket();
      mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slotIndex, 40, ClickType.SWAP, mc.player);
      return true;
   }

   private void useFireworkOffhandPacket() {
      this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, ModuleManager.aura.rotationYaw, ModuleManager.aura.rotationPitch));
      this.sendPacket(new ServerboundSwingPacket(InteractionHand.OFF_HAND));
   }

   private static int convertFireworkSlot(int slotIndex) {
      return slotIndex < 9 ? 36 + slotIndex : slotIndex;
   }

   private boolean isMace(ItemStack stack) {
      if (stack != null && stack.getItem() != null) {
         String key = stack.getItem().getDescriptionId();
         return key != null && key.toLowerCase().contains("mace") ? true : stack.getItem().toString().toLowerCase().contains("mace");
      } else {
         return false;
      }
   }

   private boolean isWearingElytra() {
      return mc.player.getInventory().getItem(38).getItem() == Items.ELYTRA;
   }

   public static void markAuraPostAttack() {
      auraPostAttackTriggered = true;
      peakAssistPostAttackTriggered = true;
   }
}
