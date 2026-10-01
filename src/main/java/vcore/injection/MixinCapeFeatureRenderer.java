package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(CapeLayer.class)
public class MixinCapeFeatureRenderer {
   @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
   private void renderCustomModelCape(
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      AvatarRenderState state,
      float yRot,
      float xRot,
      CallbackInfo ci
   ) {
      if (ModuleManager.customModel != null
         && ModuleManager.customModel.isEnabled()
         && ModuleManager.customModel.renderCape(poseStack, submitNodeCollector, lightCoords, state)) {
         ci.cancel();
      }
   }
}
