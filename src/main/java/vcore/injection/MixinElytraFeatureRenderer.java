package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(ElytraLayer.class)
public class MixinElytraFeatureRenderer<T extends LivingEntity> {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void onRenderElytra(
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
      if (ModuleManager.customModel != null && ModuleManager.customModel.shouldHideArmorSlotRender(entity)) {
         ci.cancel();
      }
   }
}
