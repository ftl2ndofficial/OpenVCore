package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.client.ModuleManager;

@Mixin(PlayerRenderer.class)
public class MixinPlayerEntityRenderer {
   @Inject(
      method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
      at = @At("HEAD")
   )
   private void beginCustomModelRender(
      AbstractClientPlayer player, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci
   ) {
      if (ModuleManager.customModel != null) {
         ModuleManager.customModel.beginPlayerRender(player);
      }
   }

   @Inject(
      method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
      at = @At("RETURN")
   )
   private void endCustomModelRender(
      AbstractClientPlayer player, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci
   ) {
      if (ModuleManager.customModel != null) {
         ModuleManager.customModel.endPlayerRender(player);
      }
   }

   @Inject(
      method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
      at = @At("RETURN"),
      cancellable = true
   )
   private void getCustomModelTexture(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
      if (ModuleManager.customModel != null && ModuleManager.customModel.isEnabled()) {
         cir.setReturnValue(ModuleManager.customModel.getTexture(player, (ResourceLocation)cir.getReturnValue()));
      }
   }
}
