package vcore.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class TotemAnimation extends Module {
   private final Setting<TotemAnimation.Mode> mode = new Setting<>("Mode", TotemAnimation.Mode.FadeOut);
   private final Setting<Integer> speed = new Setting<>("Speed", 40, 1, 100);
   private ItemStack floatingItem = null;
   private int floatingItemTimeLeft;

   public TotemAnimation() {
      super("TotemAnimation", "Custom totem animations.", Module.Category.RENDER);
   }

   public void showFloatingItem(ItemStack floatingItem) {
      this.floatingItem = floatingItem;
      this.floatingItemTimeLeft = this.getTime();
   }

   @Override
   public void onUpdate() {
      if (this.floatingItemTimeLeft > 0) {
         this.floatingItemTimeLeft--;
         if (this.floatingItemTimeLeft == 0) {
            this.floatingItem = null;
         }
      }
   }

   public void renderFloatingItem(PoseStack matrixStack, float tickDelta, SubmitNodeCollector submitNodeCollector) {
      if (this.floatingItem != null && this.floatingItemTimeLeft > 0 && !this.mode.is(TotemAnimation.Mode.Off)) {
         int scaledWidth = mc.getWindow().getGuiScaledWidth();
         int scaledHeight = mc.getWindow().getGuiScaledHeight();
         int elapsedTime = this.getTime() - this.floatingItemTimeLeft;
         float animationProgress = (elapsedTime + tickDelta) / this.getTime();
         float progressSquared = animationProgress * animationProgress;
         float progressCubed = animationProgress * progressSquared;
         float oscillationFactor = 10.25F * progressCubed * progressSquared
            - 24.95F * progressSquared * progressSquared
            + 25.5F * progressCubed
            - 13.8F * progressSquared
            + 4.0F * animationProgress;
         float oscillationRadians = oscillationFactor * (float) Math.PI;
         matrixStack.pushPose();
         float adjustedProgress = elapsedTime + tickDelta;
         float scale = 50.0F + 175.0F * Mth.sin(oscillationRadians);
         switch ((TotemAnimation.Mode)this.mode.getValue()) {
            case FadeOut:
               float x2 = (float)(Math.sin(adjustedProgress * 112.0F / 180.0F) * 100.0);
               float y2 = (float)(Math.cos(adjustedProgress * 112.0F / 180.0F) * 50.0);
               matrixStack.translate(scaledWidth / 2.0F + x2, scaledHeight / 2.0F + y2, -50.0F);
               matrixStack.scale(scale, -scale, scale);
               break;
            case Size:
               matrixStack.translate(scaledWidth / 2.0F, scaledHeight / 2.0F, -50.0F);
               matrixStack.scale(scale, -scale, scale);
               break;
            case Otkisuli:
               matrixStack.translate(scaledWidth / 2.0F, scaledHeight / 2.0F, -50.0F);
               matrixStack.mulPose(Axis.XP.rotationDegrees(adjustedProgress * 2.0F));
               matrixStack.mulPose(Axis.ZP.rotationDegrees(adjustedProgress * 2.0F));
               matrixStack.scale(200.0F - adjustedProgress * 1.5F, -200.0F + adjustedProgress * 1.5F, 200.0F - adjustedProgress * 1.5F);
               break;
            case Insert:
               matrixStack.translate(scaledWidth / 2.0F, scaledHeight / 2.0F, -50.0F);
               matrixStack.mulPose(Axis.XP.rotationDegrees(adjustedProgress * 3.0F));
               matrixStack.scale(200.0F - adjustedProgress * 1.5F, -200.0F + adjustedProgress * 1.5F, 200.0F - adjustedProgress * 1.5F);
               break;
            case Fall: {
               float downFactor = (float)(Math.pow(adjustedProgress, 3.0) * 0.2F);
               matrixStack.translate(scaledWidth / 2.0F, scaledHeight / 2.0F + downFactor, -50.0F);
               matrixStack.mulPose(Axis.ZP.rotationDegrees(adjustedProgress * 5.0F));
               matrixStack.scale(200.0F - adjustedProgress * 1.5F, -200.0F + adjustedProgress * 1.5F, 200.0F - adjustedProgress * 1.5F);
               break;
            }
            case Rocket: {
               float downFactor = (float)(Math.pow(adjustedProgress, 3.0) * 0.2F) - 20.0F;
               matrixStack.translate(scaledWidth / 2.0F, scaledHeight / 2.0F - downFactor, -50.0F);
               matrixStack.mulPose(Axis.YP.rotationDegrees(adjustedProgress * this.floatingItemTimeLeft * 2.0F));
               matrixStack.scale(200.0F - adjustedProgress * 1.5F, -200.0F + adjustedProgress * 1.5F, 200.0F - adjustedProgress * 1.5F);
               break;
            }
            case Roll:
               float rightFactor = (float)(Math.pow(adjustedProgress, 2.0) * 4.5);
               matrixStack.translate(scaledWidth / 2.0F + rightFactor, scaledHeight / 2.0F, -50.0F);
               matrixStack.mulPose(Axis.ZP.rotationDegrees(adjustedProgress * 40.0F));
               matrixStack.scale(200.0F - adjustedProgress * 1.5F, -200.0F + adjustedProgress * 1.5F, 200.0F - adjustedProgress * 1.5F);
         }

         ItemStackRenderState itemState = new ItemStackRenderState();
         mc.getItemModelResolver().updateForTopItem(itemState, this.floatingItem, ItemDisplayContext.FIXED, mc.level, null, 0);
         itemState.submit(matrixStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
         matrixStack.popPose();
      }
   }

   private int getTime() {
      int invertedSpeed = 101 - this.speed.getValue();
      if (this.mode.is(TotemAnimation.Mode.FadeOut)) {
         return invertedSpeed / 4;
      } else {
         return this.mode.is(TotemAnimation.Mode.Insert) ? invertedSpeed / 2 : invertedSpeed;
      }
   }

   private enum Mode {
      FadeOut,
      Size,
      Otkisuli,
      Insert,
      Fall,
      Rocket,
      Roll,
      Off;
   }
}
