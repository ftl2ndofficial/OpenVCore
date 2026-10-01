package vcore.features.modules.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventSync;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.features.modules.misc.ClientSettings;
import vcore.features.modules.render.HudEditor;
import vcore.injection.accesors.IClientPlayerEntity;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public final class ElytraTarget extends Module {
   private static final double PREDICT_BOX_SIZE = 1.0;
   private static final int PREDICT_FILL_ALPHA = 85;
   private static final int PREDICT_OUTLINE_ALPHA = 255;
   private static final float PREDICT_LINE_WIDTH = 1.5F;
   private static final double PREDICT_DASH_LENGTH = 0.15;
   private static final double PREDICT_GAP_LENGTH = 0.08;
   private static final float PREDICT_DARK_FACTOR = 0.1F;
   private final Setting<Boolean> prediction = new Setting<>("Prediction", true);
   private final Setting<Boolean> igoneAuraRotation = new Setting<>("Igone Aura Rotation", true);
   private final Setting<Boolean> predictAim = new Setting<>("Predict Aim", false, v -> this.prediction.getValue() && this.isPredictAimAvailable());
   private final Setting<Boolean> packetCrit = new Setting<>("Packet Crit", false);
   private final Setting<Boolean> autoFireWork = new Setting<>("Auto FireWork", false);
   private final Setting<Boolean> visual = new Setting<>("Visual", true, v -> this.prediction.getValue());
   public final Setting<ElytraTarget.Mode> mode = new Setting<>("Mode", ElytraTarget.Mode.Auto, v -> this.prediction.getValue());
   private final Setting<Float> predictRange = new Setting<>(
      "Predict Range", 2.6F, 2.0F, 5.0F, v -> this.prediction.getValue() && this.mode.is(ElytraTarget.Mode.Default)
   );
   private final Setting<Float> predictMax = new Setting<>(
      "Predict Max", 2.6F, 2.0F, 5.0F, v -> this.prediction.getValue() && this.mode.is(ElytraTarget.Mode.Auto)
   );
   public boolean status = true;
   public boolean disableForward = false;
   private final Timer hurtTimer = new Timer();
   private final Timer targetSwingTimer = new Timer();
   private final Timer fireWorkTimer = new Timer();
   private LivingEntity trackedTarget;
   private boolean targetSwingSeen;
   private double bps;
   private double scale;
   private Entity cachedTargetVecEntity;
   private Vec3 cachedTargetVec;
   private boolean cachedTargetVecPrediction;
   private long cachedTargetVecWorldTime = Long.MIN_VALUE;

   public ElytraTarget() {
      super("ElytraTarget", "Predicts elytra target movement for Aura.", Module.Category.COMBAT);
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send event) {
      if (this.packetCrit.getValue() && this.shouldSendPacketCrit()) {
         if (event.getPacket() instanceof ServerboundInteractPacket packet) {
            if (Criticals.getInteractType(packet) == Criticals.InteractType.ATTACK) {
               if (Criticals.getEntity(packet) == Aura.target) {
                  this.sendPacket(
                     new PosRot(
                        mc.player.getX(),
                        mc.player.getY() - 1.0E-6,
                        mc.player.getZ(),
                        ((IClientPlayerEntity)mc.player).getLastYaw(),
                        ((IClientPlayerEntity)mc.player).getLastPitch(),
                        false,
                        mc.player.horizontalCollision
                     )
                  );
               }
            }
         }
      }
   }

   private boolean shouldSendPacketCrit() {
      return mc.player != null
         && mc.level != null
         && mc.player.isFallFlying()
         && Aura.target != null
         && !mc.player.isInLava()
         && !mc.player.isUnderWater();
   }

   @EventHandler
   public void onSync(EventSync event) {
      if (mc.player != null && mc.level != null) {
         LivingEntity target = null;
         if (Aura.target instanceof LivingEntity livingTarget) {
            target = livingTarget;
         }

         this.status = this.prediction.getValue();
         if (target == null) {
            this.disableForward = false;
            this.bps = 0.0;
            this.clearTargetVecCache();
            this.clearTrackedTarget();
         } else {
            this.updateTrackedTarget(target);
            if (mc.player.hurtTime > 0 && this.hasRecentTargetSwing()) {
               this.disableForward = true;
               this.hurtTimer.reset();
            }

            if (this.hurtTimer.passedMs(500L)) {
               this.disableForward = false;
            }

            this.handleAutoFireWork(target);
            this.updatePredictionState(target);
            this.clearTargetVecCache();
         }
      }
   }

   @Override
   public void onRender3D(PoseStack stack) {
      if (mc.player != null && mc.level != null && this.visual.getValue()) {
         if (Aura.target instanceof LivingEntity livingTarget) {
            if (this.shouldTarget(livingTarget)) {
               AABB box = this.getPredictRenderBox(livingTarget);
               int offset = livingTarget.tickCount * 2;
               Color[] fillColors = this.getPredictThemeColors(85, offset);
               Color[] outlineColors = this.getPredictThemeColors(255, offset);
               Render3DEngine.drawGradientFilledFadeBox(stack, box, fillColors, fillColors);
               Render3DEngine.drawGradientDashedBoxOutline(box, outlineColors, 1.5F, 0.15, 0.08);
            }
         }
      }
   }

   private AABB getPredictRenderBox(LivingEntity livingTarget) {
      Vec3 predictionOffset = livingTarget.getDeltaMovement().scale(this.getPrediction(livingTarget));
      Vec3 predictCenter = livingTarget.getBoundingBox().move(predictionOffset).getCenter();
      return AABB.ofSize(predictCenter, 1.0, 1.0, 1.0);
   }

   private Color[] getPredictThemeColors(int alpha, int offset) {
      return new Color[]{
         this.getPredictThemeColor(offset, alpha, true),
         this.getPredictThemeColor(offset + 90, alpha, false),
         this.getPredictThemeColor(offset + 180, alpha, true),
         this.getPredictThemeColor(offset + 270, alpha, false)
      };
   }

   private Color getPredictThemeColor(int offset, int alpha, boolean darken) {
      Color themeColor = HudEditor.getColor(offset);
      if (darken) {
         themeColor = Render2DEngine.darker(themeColor, 0.1F);
      }

      return Render2DEngine.injectAlpha(themeColor, alpha);
   }

   public double getPrediction(LivingEntity target) {
      return this.mode.is(ElytraTarget.Mode.Auto) ? this.scale : this.predictRange.getValue().floatValue();
   }

   public boolean shouldUsePredictAim() {
      return this.isPredictAimAvailable() && this.predictAim.getValue();
   }

   public boolean shouldUseElytraTargetRotationSpeed() {
      return this.igoneAuraRotation.getValue();
   }

   public boolean shouldUseElytraTargetRotation(LivingEntity livingTarget) {
      return this.shouldUseElytraTargetRotationSpeed() && this.shouldUseElytraRotationGate(livingTarget);
   }

   public boolean shouldUseAuraRotationSpeed(LivingEntity livingTarget) {
      return !this.shouldUseElytraTargetRotationSpeed() && this.shouldUseElytraRotationGate(livingTarget);
   }

   private boolean shouldUseElytraRotationGate(LivingEntity livingTarget) {
      return ModuleManager.aura != null && this.isAuraRotationModeSupported() && this.shouldTarget(livingTarget);
   }

   private boolean isAuraRotationModeSupported() {
      return ModuleManager.aura.rotationMode.not(Aura.Mode.None) && ModuleManager.aura.rotationMode.not(Aura.Mode.Snap);
   }

   public boolean shouldAimAtPrediction() {
      return !this.isPredictAimAvailable() ? true : this.predictAim.getValue();
   }

   private boolean isPredictAimAvailable() {
      return ModuleManager.elytraBoost != null
         && ModuleManager.elytraBoost.isControlModeActive()
         && ModuleManager.aura != null
         && this.prediction.getValue()
         && this.isAuraRotationModeSupported();
   }

   private void handleAutoFireWork(LivingEntity target) {
      if (this.autoFireWork.getValue()
         && target != null
         && mc.player != null
         && mc.player.isFallFlying()
         && this.canRunAutoFireWork(target)
         && this.fireWorkTimer.passedMs(500L)) {
         this.useFirework();
         this.fireWorkTimer.reset();
      }
   }

   private boolean canRunAutoFireWork(LivingEntity target) {
      return ModuleManager.elytraMotion != null && ModuleManager.elytraMotion.isEnabled() ? this.shouldTarget(target) : true;
   }

   private void useFirework() {
      if (mc.player != null && mc.gameMode != null) {
         if (!mc.player.getCooldowns().isOnCooldown(Items.FIREWORK_ROCKET.getDefaultInstance())) {
            if (ClientSettings.fireWorkMode.is(ClientSettings.FireWorkMode.OffHand)) {
               this.useFireworkOffhand();
            } else {
               this.useFireworkMainHand();
            }
         }
      }
   }

   private void useFireworkMainHand() {
      if (mc.player.getOffhandItem().getItem() == Items.FIREWORK_ROCKET) {
         this.useFireworkOffhandPacket();
      } else {
         SearchInvResult fireWorkResult = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET);
         if (fireWorkResult.found()) {
            int currentSlot = mc.player.getInventory().getSelectedSlot();
            int hotbarSlot = currentSlot % 8 + 1;
            int itemSlot = fireWorkResult.slot();
            if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
               this.useFireworkWithOffhandSwap(itemSlot);
            } else if (itemSlot >= 36) {
               this.useFireworkFromHotbar(itemSlot - 36, currentSlot);
            } else {
               mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, itemSlot, hotbarSlot, ClickType.SWAP, mc.player);
               this.useFireworkFromHotbar(hotbarSlot, currentSlot);
               mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, itemSlot, hotbarSlot, ClickType.SWAP, mc.player);
            }
         }
      }
   }

   private void useFireworkOffhand() {
      if (mc.player.getOffhandItem().getItem() == Items.FIREWORK_ROCKET) {
         this.useFireworkOffhandPacket();
      } else {
         SearchInvResult fireWorkResult = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET);
         if (fireWorkResult.found() && mc.player.containerMenu == mc.player.inventoryMenu) {
            int slotIndex = convertFireworkSlot(fireWorkResult.slot());
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slotIndex, 40, ClickType.SWAP, mc.player);
            this.useFireworkOffhandPacket();
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slotIndex, 40, ClickType.SWAP, mc.player);
         }
      }
   }

   private void useFireworkFromHotbar(int fireworkSlot, int returnSlot) {
      InventoryUtility.switchToSilent(fireworkSlot);
      float[] rotation = this.getUseRotation();
      this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, rotation[0], rotation[1]));
      InventoryUtility.switchToSilent(returnSlot);
   }

   private void useFireworkWithOffhandSwap(int itemSlot) {
      int slotIndex = convertFireworkSlot(itemSlot);
      mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slotIndex, 40, ClickType.SWAP, mc.player);
      this.useFireworkOffhandPacket();
      mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slotIndex, 40, ClickType.SWAP, mc.player);
   }

   private void useFireworkOffhandPacket() {
      float[] rotation = this.getUseRotation();
      this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, rotation[0], rotation[1]));
   }

   private static int convertFireworkSlot(int slotIndex) {
      return slotIndex < 9 ? 36 + slotIndex : slotIndex;
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

   public boolean shouldTarget(LivingEntity livingEntity) {
      if (this.isEnabled() && livingEntity != null && !this.disableForward && mc.player != null) {
         boolean isTargetValid = livingEntity.isFallFlying();
         return this.status && mc.player.isFallFlying() && isTargetValid && this.getBps(livingEntity) > 13.5;
      } else {
         return false;
      }
   }

   public double getBps(LivingEntity entity) {
      if (entity == null) {
         return 0.0;
      }

      double dx = entity.getX() - entity.xo;
      double dy = entity.getY() - entity.yo;
      double dz = entity.getZ() - entity.zo;
      return Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
   }

   private void updatePredictionState(LivingEntity target) {
      if (target != null && this.shouldTarget(target)) {
         this.bps = this.getBps(target);
         this.scale = Math.sqrt(this.bps) / 2.0;
         this.scale = Math.min(this.scale, this.predictMax.getValue().floatValue());
      } else {
         this.bps = 0.0;
      }
   }

   public Vec3 getTargetVec(Entity entity, boolean applyPrediction) {
      if (mc.player != null && entity != null) {
         long worldTime = mc.level == null ? Long.MIN_VALUE : mc.level.getGameTime();
         if (entity == this.cachedTargetVecEntity
            && this.cachedTargetVecPrediction == applyPrediction
            && this.cachedTargetVecWorldTime == worldTime
            && this.cachedTargetVec != null) {
            return this.cachedTargetVec;
         }

         Vec3 targetVec = this.calculateTargetVec(entity, applyPrediction);
         this.cachedTargetVecEntity = entity;
         this.cachedTargetVec = targetVec;
         this.cachedTargetVecPrediction = applyPrediction;
         this.cachedTargetVecWorldTime = worldTime;
         return targetVec;
      } else {
         return null;
      }
   }

   private Vec3 calculateTargetVec(Entity entity, boolean applyPrediction) {
      if (entity instanceof LivingEntity livingTarget) {
         Vec3 basePos = this.getBestPoint(mc.player.getEyePosition(), livingTarget.getBoundingBox());
         if (applyPrediction && this.shouldTarget(livingTarget)) {
            basePos = basePos.add(livingTarget.getDeltaMovement().scale(this.getPrediction(livingTarget)));
         }

         return basePos;
      } else {
         return entity.position().add(0.0, entity.getEyeHeight(entity.getPose()), 0.0);
      }
   }

   private void clearTargetVecCache() {
      this.cachedTargetVecEntity = null;
      this.cachedTargetVec = null;
      this.cachedTargetVecPrediction = false;
      this.cachedTargetVecWorldTime = Long.MIN_VALUE;
   }

   private Vec3 getBestPoint(Vec3 pos, AABB box) {
      return new Vec3(Mth.clamp(pos.x, box.minX, box.maxX), Mth.clamp(pos.y, box.minY, box.maxY), Mth.clamp(pos.z, box.minZ, box.maxZ));
   }

   private void updateTrackedTarget(LivingEntity target) {
      if (this.trackedTarget != target) {
         this.trackedTarget = target;
         this.targetSwingSeen = false;
      }

      if (this.isTargetSwinging(target)) {
         this.targetSwingSeen = true;
         this.targetSwingTimer.reset();
      }
   }

   private boolean hasRecentTargetSwing() {
      return this.targetSwingSeen && !this.targetSwingTimer.passedMs(500L);
   }

   private void clearTrackedTarget() {
      this.trackedTarget = null;
      this.targetSwingSeen = false;
   }

   private boolean isTargetSwinging(LivingEntity target) {
      return target.swinging || target.swingTime > 0 || target.attackAnim > 0.0F;
   }

   public enum Mode {
      Auto,
      Default;
   }
}
