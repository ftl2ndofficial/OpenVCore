package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventHeldItemRenderer;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.injection.accesors.IHeldItemRenderer;
import vcore.setting.Setting;

public class Animations extends Module {
   private static final float SELF_OLD_MAIN_HAND_X_OFFSET = 0.12F;
   private static final float SELF_OLD_MAIN_HAND_Y_OFFSET = 0.1F;
   private final Setting<Integer> power = new Setting<>("Power", 6, 1, 10);
   public final Setting<Integer> slowAnimationVal = new Setting<>("SlowValue", 12, 1, 20, v -> this.shouldShowSlowValue());
   private final Setting<Integer> offset = new Setting<>("Offset", 0, 0, 10, v -> this.shouldShowOffset());
   private final Setting<Boolean> onlyaura = new Setting<>("OnlyAura", false);
   public final Setting<Boolean> slowAnimation = new Setting<>("Slow", true);
   private final Setting<Animations.Mode> mode = new Setting<>("Mode", Animations.Mode.Smooth);
   public boolean flip;

   public Animations() {
      super("Animations", "Custom hand animations.", Module.Category.RENDER);
   }

   private boolean shouldShowSlowValue() {
      return this.slowAnimation.getValue();
   }

   private boolean shouldShowOffset() {
      return this.mode.getValue() == Animations.Mode.Self || this.mode.getValue() == Animations.Mode.Old;
   }

   public boolean shouldRunAnimationLogic() {
      return !this.isEnabled() ? false : !this.onlyaura.getValue() || ModuleManager.aura.isEnabled() && Aura.target != null;
   }

   public boolean shouldAnimate(ItemStack item) {
      return this.shouldRunAnimationLogic();
   }

   public boolean shouldChangeAnimationDuration() {
      return this.shouldRunAnimationLogic() && this.slowAnimation.getValue();
   }

   public int getHandSwingDuration() {
      return this.slowAnimationVal.getValue();
   }

   @Override
   public void onUpdate() {
      if (!fullNullCheck() && this.shouldRunAnimationLogic()) {
         if (((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).getEquippedProgressMainHand() <= 1.0F) {
            ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).setEquippedProgressMainHand(1.0F);
            ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).setItemStackMainHand(mc.player.getMainHandItem());
         }

         if (((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).getEquippedProgressOffHand() <= 1.0F) {
            ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).setEquippedProgressOffHand(1.0F);
            ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).setItemStackOffHand(mc.player.getOffhandItem());
         }
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (e.getPacket() instanceof ServerboundSwingPacket) {
         this.flip = !this.flip;
      }
   }

   private void renderSwordAnimation(PoseStack matrices, float f, float swingProgress, float equipProgress, HumanoidArm arm, InteractionHand hand, Animations.Mode animMode) {
      if (arm != HumanoidArm.LEFT || animMode != Animations.Mode.Self && animMode != Animations.Mode.Old) {
         switch (animMode) {
            case Default:
               this.applyEquipOffset(matrices, arm, equipProgress);
               this.translateToViewModel(matrices);
               float sin2_def = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
               matrices.mulPose(Axis.XP.rotationDegrees(sin2_def * -(this.power.getValue().intValue() * 10.0F)));
               matrices.mulPose(Axis.ZP.rotationDegrees(sin2_def * 45.0F));
               matrices.mulPose(Axis.YP.rotationDegrees(sin2_def * 15.0F));
               this.translateBack(matrices);
               break;
            case Smooth:
               this.applyEquipOffset(matrices, arm, equipProgress);
               this.translateToViewModel(matrices);
               float s_f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
               float s_sin2 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
               float s_f1 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
               matrices.mulPose(Axis.YP.rotationDegrees(s_sin2 * (45.0F + s_f * -20.0F)));
               matrices.mulPose(Axis.ZP.rotationDegrees(s_sin2 * s_f1 * -20.0F));
               matrices.mulPose(Axis.XP.rotationDegrees(s_f1 * -(this.power.getValue().intValue() * 10.0F)));
               matrices.mulPose(Axis.YP.rotationDegrees(s_sin2 * -45.0F));
               this.translateBack(matrices);
               break;
            case Fade:
               this.applyEquipOffset(matrices, arm, equipProgress);
               this.translateToViewModel(matrices);
               int handDir = arm == HumanoidArm.RIGHT ? 1 : -1;
               float sinSq = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
               float sinSqrt = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
               float sinLinear = Mth.sin(swingProgress * (float) Math.PI);
               matrices.mulPose(Axis.YP.rotationDegrees(handDir * sinSqrt * (45.0F + sinSq * -5.0F)));
               matrices.mulPose(Axis.ZP.rotationDegrees(handDir * sinSqrt * sinLinear * -20.0F));
               matrices.mulPose(Axis.XP.rotationDegrees(sinLinear * -((this.power.getValue().intValue() + 2.0F) * 8.0F)));
               matrices.mulPose(Axis.YP.rotationDegrees(handDir * sinSqrt * -45.0F));
               this.translateBack(matrices);
               break;
            case Self: {
               float anim = Mth.sin(swingProgress * (float) Math.PI);
               this.applyEquipOffset(matrices, arm, 0.0F);
               this.translateToSelfOldViewModel(matrices, hand, animMode);
               matrices.mulPose(Axis.YP.rotationDegrees(90.0F));
               matrices.mulPose(Axis.ZP.rotationDegrees(-70.0F));
               float baseX = -90.0F + this.offset.getValue().intValue() * 10.0F;
               matrices.mulPose(Axis.XP.rotationDegrees(baseX - this.power.getValue().intValue() * 10.0F * anim));
               this.translateBackSelfOldViewModel(matrices, hand, animMode);
               break;
            }
            case Old: {
               float anim = Mth.sin(swingProgress * (float) Math.PI);
               this.applyEquipOffset(matrices, arm, 0.0F);
               this.translateToSelfOldViewModel(matrices, hand, animMode);
               matrices.mulPose(Axis.YP.rotationDegrees(90.0F));
               matrices.mulPose(Axis.ZP.rotationDegrees(-30.0F));
               float baseX = -90.0F + this.offset.getValue().intValue() * 10.0F;
               matrices.mulPose(Axis.XP.rotationDegrees(baseX - this.power.getValue().intValue() * 10.0F * anim));
               this.translateBackSelfOldViewModel(matrices, hand, animMode);
            }
         }
      } else {
         float viewModelX = this.getSelfOldViewModelX(hand, animMode);
         float viewModelY = this.getSelfOldViewModelY(hand, animMode);
         float viewModelZ = this.getViewModelZ();
         this.applyEquipOffset(matrices, arm, equipProgress);
         matrices.translate(-viewModelX, viewModelY, viewModelZ);
         this.applySwingOffset(matrices, arm, swingProgress);
         matrices.translate(viewModelX, -viewModelY, -viewModelZ);
      }
   }

   public void renderFirstPersonItemCustom(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector submitNodeCollector,
      int light
   ) {
      if (!player.isScoping()) {
         if (this.onlyaura.getValue() && (!ModuleManager.aura.isEnabled() || Aura.target == null)) {
            return;
         }

         boolean bl = hand == InteractionHand.MAIN_HAND;
         HumanoidArm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
         matrices.pushPose();
         float f = 0.0F;
         if (item.is(Items.CROSSBOW)) {
            boolean bl2 = CrossbowItem.isCharged(item);
            boolean bl3 = arm == HumanoidArm.RIGHT;
            int i = bl3 ? 1 : -1;
            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
               this.applyEquipOffset(matrices, arm, equipProgress);
               matrices.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
               matrices.mulPose(Axis.XP.rotationDegrees(-11.935F));
               matrices.mulPose(Axis.YP.rotationDegrees(i * 65.3F));
               matrices.mulPose(Axis.ZP.rotationDegrees(i * -9.785F));
               f = item.getUseDuration(mc.player) - (mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F);
               float g = f / CrossbowItem.getChargeDuration(item, mc.player);
               if (g > 1.0F) {
                  g = 1.0F;
               }

               if (g > 0.1F) {
                  float h = Mth.sin((f - 0.1F) * 1.3F);
                  float j = g - 0.1F;
                  float k = h * j;
                  matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
               }

               matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
               matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
               matrices.mulPose(Axis.YN.rotationDegrees(i * 45.0F));
            } else {
               f = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
               float g = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
               float h = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
               matrices.translate(i * f, g, h);
               this.applyEquipOffset(matrices, arm, equipProgress);
               this.applySwingOffset(matrices, arm, swingProgress);
               if (bl2 && swingProgress < 0.001F && bl) {
                  matrices.translate(i * -0.641864F, 0.0F, 0.0F);
                  matrices.mulPose(Axis.YP.rotationDegrees(i * 10.0F));
               }
            }

            this.renderItem(
               player,
               item,
               bl3 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
               !bl3,
               matrices,
               submitNodeCollector,
               light
            );
         } else if (this.shouldAnimate(item)) {
            boolean bl2 = arm == HumanoidArm.RIGHT;
            float m = 0.0F;
            Animations.Mode chosenMode = Animations.Mode.Fade;
            if (this.isSword(item.getItem()) || this.isAxe(item.getItem()) || this.isPickaxe(item.getItem())) {
               chosenMode = this.mode.getValue();
            }

            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
               int l = bl2 ? 1 : -1;
               switch (item.getUseAnimation()) {
                  case NONE:
                  case BLOCK:
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     break;
                  case EAT:
                  case DRINK:
                     this.applyEatOrDrinkTransformationCustom(matrices, tickDelta, arm, item);
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     break;
                  case BOW:
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     matrices.translate(l * -0.2785682F, 0.18344387F, 0.15731531F);
                     matrices.mulPose(Axis.XP.rotationDegrees(-13.935F));
                     matrices.mulPose(Axis.YP.rotationDegrees(l * 35.3F));
                     matrices.mulPose(Axis.ZP.rotationDegrees(l * -9.785F));
                     m = item.getUseDuration(mc.player) - (mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                     f = m / 20.0F;
                     f = (f * f + f * 2.0F) / 3.0F;
                     if (f > 1.0F) {
                        f = 1.0F;
                     }

                     if (f > 0.1F) {
                        float g = Mth.sin((m - 0.1F) * 1.3F);
                        float h = f - 0.1F;
                        float j = g * h;
                        matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                     }

                     matrices.translate(f * 0.0F, f * 0.0F, f * 0.04F);
                     matrices.scale(1.0F, 1.0F, 1.0F + f * 0.2F);
                     matrices.mulPose(Axis.YN.rotationDegrees(l * 45.0F));
                     break;
                  case SPEAR:
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     matrices.translate(l * -0.5F, 0.7F, 0.1F);
                     matrices.mulPose(Axis.XP.rotationDegrees(-55.0F));
                     matrices.mulPose(Axis.YP.rotationDegrees(l * 35.3F));
                     matrices.mulPose(Axis.ZP.rotationDegrees(l * -9.785F));
                     m = item.getUseDuration(mc.player) - (mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                     f = m / 10.0F;
                     if (f > 1.0F) {
                        f = 1.0F;
                     }

                     if (f > 0.1F) {
                        float g = Mth.sin((m - 0.1F) * 1.3F);
                        float h = f - 0.1F;
                        float j = g * h;
                        matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                     }

                     matrices.translate(0.0F, 0.0F, f * 0.2F);
                     matrices.scale(1.0F, 1.0F, 1.0F + f * 0.2F);
                     matrices.mulPose(Axis.YN.rotationDegrees(l * 45.0F));
                     break;
                  case BRUSH:
                     this.applyBrushTransformation(matrices, tickDelta, arm, item, equipProgress);
                     break;
                  default:
                     this.applyEquipOffset(matrices, arm, equipProgress);
               }
            } else if (player.isAutoSpinAttack()) {
               this.applyEquipOffset(matrices, arm, equipProgress);
               int l = bl2 ? 1 : -1;
               matrices.translate(l * -0.4F, 0.8F, 0.3F);
               matrices.mulPose(Axis.YP.rotationDegrees(l * 65.0F));
               matrices.mulPose(Axis.ZP.rotationDegrees(l * -85.0F));
            } else {
               this.renderSwordAnimation(matrices, f, swingProgress, equipProgress, arm, hand, chosenMode);
            }

            EventHeldItemRenderer event = new EventHeldItemRenderer(hand, item, equipProgress, matrices);
            Vcore.EVENT_BUS.post(event);
            this.applySelfOldMainHandOffset(matrices, hand, chosenMode);
            this.renderItem(
               player,
               item,
               bl2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
               !bl2,
               matrices,
               submitNodeCollector,
               light
            );
         } else {
            boolean isRight = arm == HumanoidArm.RIGHT;
            this.renderItem(
               player,
               item,
               isRight ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
               !isRight,
               matrices,
               submitNodeCollector,
               light
            );
         }

         matrices.popPose();
      }
   }

   private void applyBrushTransformation(PoseStack matrices, float tickDelta, HumanoidArm arm, @NotNull ItemStack stack, float equipProgress) {
      this.applyEquipOffset(matrices, arm, equipProgress);
      float f = mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F;
      float g = 1.0F - f / stack.getUseDuration(mc.player);
      float m = -15.0F + 75.0F * Mth.cos(g * 45.0F * (float) Math.PI);
      if (arm != HumanoidArm.RIGHT) {
         matrices.translate(0.1, 0.83, 0.35);
         matrices.mulPose(Axis.XP.rotationDegrees(-80.0F));
         matrices.mulPose(Axis.YP.rotationDegrees(-90.0F));
         matrices.mulPose(Axis.XP.rotationDegrees(m));
         matrices.translate(-0.3, 0.22, 0.35);
      } else {
         matrices.translate(-0.25, 0.22, 0.35);
         matrices.mulPose(Axis.XP.rotationDegrees(-80.0F));
         matrices.mulPose(Axis.YP.rotationDegrees(90.0F));
         matrices.mulPose(Axis.ZP.rotationDegrees(0.0F));
         matrices.mulPose(Axis.XP.rotationDegrees(m));
      }
   }

   private void applyEquipOffset(@NotNull PoseStack matrices, HumanoidArm arm, float equipProgress) {
      int i = arm == HumanoidArm.RIGHT ? 1 : -1;
      matrices.translate(i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
   }

   private void applySwingOffset(@NotNull PoseStack matrices, HumanoidArm arm, float swingProgress) {
      int i = arm == HumanoidArm.RIGHT ? 1 : -1;
      float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
      matrices.mulPose(Axis.YP.rotationDegrees(i * (45.0F + f * -20.0F)));
      float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
      matrices.mulPose(Axis.ZP.rotationDegrees(i * g * -20.0F));
      matrices.mulPose(Axis.XP.rotationDegrees(g * -80.0F));
      matrices.mulPose(Axis.YP.rotationDegrees(i * -45.0F));
   }

   public void renderItem(
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext renderMode,
      boolean leftHanded,
      PoseStack matrices,
      SubmitNodeCollector submitNodeCollector,
      int light
   ) {
      if (!stack.isEmpty()) {
         ItemStackRenderState itemstackrenderstate = new ItemStackRenderState();
         mc.getItemModelResolver().updateForTopItem(itemstackrenderstate, stack, renderMode, entity.level(), entity, entity.getId() + renderMode.ordinal());
         itemstackrenderstate.submit(matrices, submitNodeCollector, light, OverlayTexture.NO_OVERLAY, 0);
      }
   }

   private void applyEatOrDrinkTransformationCustom(PoseStack matrices, float tickDelta, HumanoidArm arm, @NotNull ItemStack stack) {
      float f = mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F;
      float g = f / stack.getUseDuration(mc.player);
      if (g < 0.8F) {
         float h = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.005F);
         matrices.translate(0.0F, h, 0.0F);
      }

      float h = 1.0F - (float)Math.pow(g, 27.0);
      int i = arm == HumanoidArm.RIGHT ? 1 : -1;
      matrices.translate(h * 0.6F * i * ModuleManager.viewModel.getEatXFactor(), h * -0.5F * ModuleManager.viewModel.eatY.getValue(), h * 0.0F);
      matrices.mulPose(Axis.YP.rotationDegrees(i * h * 90.0F));
      matrices.mulPose(Axis.XP.rotationDegrees(h * 10.0F));
      matrices.mulPose(Axis.ZP.rotationDegrees(i * h * 30.0F));
   }

   private void translateToViewModel(PoseStack matrices) {
      if (ModuleManager.viewModel.isEnabled()) {
         matrices.translate(
            ModuleManager.viewModel.positionMainX.getValue(),
            ModuleManager.viewModel.positionMainY.getValue(),
            ModuleManager.viewModel.positionMainZ.getValue()
         );
      }
   }

   private void translateToViewModelOff(PoseStack matrices) {
      if (ModuleManager.viewModel.isEnabled()) {
         matrices.translate(
            -ModuleManager.viewModel.positionMainX.getValue(),
            ModuleManager.viewModel.positionMainY.getValue(),
            ModuleManager.viewModel.positionMainZ.getValue()
         );
      }
   }

   private void translateBack(PoseStack matrices) {
      if (ModuleManager.viewModel.isEnabled()) {
         matrices.translate(
            -ModuleManager.viewModel.positionMainX.getValue(),
            -ModuleManager.viewModel.positionMainY.getValue(),
            -ModuleManager.viewModel.positionMainZ.getValue()
         );
      }
   }

   private void translateToSelfOldViewModel(PoseStack matrices, InteractionHand hand, Animations.Mode animMode) {
      matrices.translate(this.getSelfOldViewModelX(hand, animMode), this.getSelfOldViewModelY(hand, animMode), this.getViewModelZ());
   }

   private void translateBackSelfOldViewModel(PoseStack matrices, InteractionHand hand, Animations.Mode animMode) {
      matrices.translate(-this.getSelfOldViewModelX(hand, animMode), -this.getSelfOldViewModelY(hand, animMode), -this.getViewModelZ());
   }

   private float getSelfOldViewModelX(InteractionHand hand, Animations.Mode animMode) {
      float x = ModuleManager.viewModel.isEnabled() ? ModuleManager.viewModel.positionMainX.getValue() : 0.0F;
      return x + this.getSelfOldMainHandXOffset(hand, animMode);
   }

   private float getSelfOldViewModelY(InteractionHand hand, Animations.Mode animMode) {
      return this.getViewModelY() + this.getSelfMainHandYOffset(hand, animMode);
   }

   private float getViewModelY() {
      return ModuleManager.viewModel.isEnabled() ? ModuleManager.viewModel.positionMainY.getValue() : 0.0F;
   }

   private float getViewModelZ() {
      return ModuleManager.viewModel.isEnabled() ? ModuleManager.viewModel.positionMainZ.getValue() : 0.0F;
   }

   private void applySelfOldMainHandOffset(PoseStack matrices, InteractionHand hand, Animations.Mode animMode) {
      float xOffset = this.getSelfOldMainHandXOffset(hand, animMode);
      float yOffset = this.getSelfMainHandYOffset(hand, animMode);
      if (xOffset != 0.0F || yOffset != 0.0F) {
         matrices.translate(xOffset, yOffset, 0.0F);
      }
   }

   private float getSelfOldMainHandXOffset(InteractionHand hand, Animations.Mode animMode) {
      return hand != InteractionHand.MAIN_HAND || animMode != Animations.Mode.Self && animMode != Animations.Mode.Old ? 0.0F : 0.12F;
   }

   private float getSelfMainHandYOffset(InteractionHand hand, Animations.Mode animMode) {
      return hand != InteractionHand.MAIN_HAND || animMode != Animations.Mode.Self && animMode != Animations.Mode.Old ? 0.0F : 0.1F;
   }

   private void translateBacklOff(PoseStack matrices) {
      if (ModuleManager.viewModel.isEnabled()) {
         matrices.translate(
            ModuleManager.viewModel.positionMainX.getValue(),
            -ModuleManager.viewModel.positionMainY.getValue(),
            -ModuleManager.viewModel.positionMainZ.getValue()
         );
      }
   }

   private boolean isSword(Item i) {
      return i == Items.WOODEN_SWORD
         || i == Items.STONE_SWORD
         || i == Items.IRON_SWORD
         || i == Items.GOLDEN_SWORD
         || i == Items.DIAMOND_SWORD
         || i == Items.NETHERITE_SWORD
         || i == Items.MACE;
   }

   private boolean isPickaxe(Item i) {
      return i == Items.WOODEN_PICKAXE
         || i == Items.STONE_PICKAXE
         || i == Items.IRON_PICKAXE
         || i == Items.GOLDEN_PICKAXE
         || i == Items.DIAMOND_PICKAXE
         || i == Items.NETHERITE_PICKAXE;
   }

   private boolean isAxe(Item i) {
      return i == Items.WOODEN_AXE
         || i == Items.STONE_AXE
         || i == Items.IRON_AXE
         || i == Items.GOLDEN_AXE
         || i == Items.DIAMOND_AXE
         || i == Items.NETHERITE_AXE;
   }

   private enum Mode {
      Default,
      Smooth,
      Fade,
      Self,
      Old;
   }
}
