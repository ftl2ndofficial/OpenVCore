package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(WingsLayer.class)
public class MixinElytraFeatureRenderer {
   @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
   private void onSubmitElytra(
      PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, HumanoidRenderState state, float yRot, float xRot, CallbackInfo ci
   ) {
      if (ModuleManager.customModel != null && ModuleManager.customModel.isEnabled() && state instanceof AvatarRenderState) {
         ci.cancel();
      }
   }
}
