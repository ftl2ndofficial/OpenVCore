package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(ItemInHandLayer.class)
public abstract class MixinHeldItemFeatureRenderer<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> extends RenderLayer<T, M> {
   @Shadow
   @Final
   private ItemInHandRenderer itemInHandRenderer;

   protected MixinHeldItemFeatureRenderer(RenderLayerParent<T, M> context) {
      super(context);
   }

   @Inject(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/layers/ItemInHandLayer;renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
         ordinal = 0
      )
   )
   private void applyCustomModelHeldItemRoot(
      PoseStack matrices,
      MultiBufferSource vertexConsumers,
      int light,
      T entity,
      float limbAngle,
      float limbDistance,
      float tickDelta,
      float animationProgress,
      float headYaw,
      float headPitch,
      CallbackInfo ci
   ) {
      if (ModuleManager.customModel != null) {
         ModuleManager.customModel.applyHeldItemRootTransform(matrices, entity);
      }
   }

   @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
   private void renderCustomModelHeldItem(
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext renderMode,
      HumanoidArm arm,
      PoseStack matrices,
      MultiBufferSource vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      if (ModuleManager.customModel != null && ModuleManager.customModel.shouldTransformHeldItem(entity)) {
         ci.cancel();
         if (!stack.isEmpty()) {
            matrices.pushPose();
            ((ArmedModel)this.getParentModel()).translateToHand(arm, matrices);
            matrices.mulPose(Axis.XP.rotationDegrees(-90.0F));
            matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
            if (renderMode == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || renderMode == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
               boolean leftHanded = arm == HumanoidArm.LEFT;
               matrices.translate((leftHanded ? -1.0F : 1.0F) / 6.0F, 0.125F, -0.999F);
            }

            this.itemInHandRenderer.renderItem(entity, stack, renderMode, arm == HumanoidArm.LEFT, matrices, vertexConsumers, light);
            matrices.popPose();
         }
      }
   }
}
