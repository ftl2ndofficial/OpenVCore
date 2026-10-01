package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventHeldItemRenderer;
import vcore.features.modules.Module;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinHeldItemRenderer {
   @Inject(
      method = "renderArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
      )
   )
   private void onRenderItem(
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      if (!Module.fullNullCheck()) {
         EventHeldItemRenderer event = new EventHeldItemRenderer(hand, itemStack, inverseArmHeight, poseStack);
         Vcore.EVENT_BUS.post(event);
      }
   }

   @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
   private void onRenderItemHook(
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      if (Managers.MODULE != null && ModuleManager.animations.shouldAnimate(itemStack) && !itemStack.isEmpty() && !(itemStack.getItem() instanceof MapItem)) {
         ci.cancel();
         ModuleManager.animations
            .renderFirstPersonItemCustom(player, frameInterp, xRot, hand, attack, itemStack, inverseArmHeight, poseStack, submitNodeCollector, lightCoords);
      }
   }

   private void applyEatOrDrinkTransformationCustom(PoseStack matrices, float tickDelta, HumanoidArm arm, @NotNull ItemStack stack) {
      float f = Module.mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F;
      float g = f / stack.getUseDuration(Module.mc.player);
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

   @Inject(method = "applyEatTransform", at = @At("HEAD"), cancellable = true)
   private void applyEatOrDrinkTransformationHook(PoseStack poseStack, float frameInterp, HumanoidArm arm, ItemStack itemStack, Player player, CallbackInfo ci) {
      if (ModuleManager.animations.shouldAnimate(itemStack)) {
         this.applyEatOrDrinkTransformationCustom(poseStack, frameInterp, arm, itemStack);
         ci.cancel();
      }
   }
}
