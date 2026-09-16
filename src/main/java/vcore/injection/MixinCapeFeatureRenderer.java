package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(CapeLayer.class)
public class MixinCapeFeatureRenderer {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void renderCustomModelCape(
      PoseStack matrixStack,
      MultiBufferSource vertexConsumerProvider,
      int i,
      AbstractClientPlayer abstractClientPlayerEntity,
      float f,
      float g,
      float h,
      float j,
      float k,
      float l,
      CallbackInfo ci
   ) {
      if (ModuleManager.customModel != null
         && ModuleManager.customModel.isEnabled()
         && ModuleManager.customModel.renderCape(matrixStack, vertexConsumerProvider, i, abstractClientPlayerEntity, h)) {
         ci.cancel();
      }
   }

   @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 6)
   private float renderHook(
      float n,
      PoseStack matrixStack,
      MultiBufferSource vertexConsumerProvider,
      int i,
      AbstractClientPlayer abstractClientPlayerEntity,
      float f,
      float g,
      float h,
      float j,
      float k,
      float l
   ) {
      return Mth.lerp(h, abstractClientPlayerEntity.yBodyRotO, abstractClientPlayerEntity.yBodyRot);
   }
}
