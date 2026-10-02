package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(ItemInHandLayer.class)
public abstract class MixinHeldItemFeatureRenderer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ArmedModel<S>> extends RenderLayer<S, M> {
   protected MixinHeldItemFeatureRenderer(RenderLayerParent<S, M> context) {
      super(context);
   }

   @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
   private void renderCustomModelHeldItem(
      S state,
      ItemStackRenderState item,
      ItemStack itemStack,
      HumanoidArm arm,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      if (ModuleManager.customModel != null && ModuleManager.customModel.shouldTransformHeldItem(state)) {
         ci.cancel();
         if (!item.isEmpty()) {
            poseStack.pushPose();
            ModuleManager.customModel.applyHeldItemRootTransform(poseStack, state);
            this.getParentModel().translateToHand(state, arm, poseStack);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            boolean leftHanded = arm == HumanoidArm.LEFT;
            poseStack.translate((leftHanded ? -1.0F : 1.0F) / 6.0F, 0.125F, -0.999F);
            item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
         }
      }
   }
}
