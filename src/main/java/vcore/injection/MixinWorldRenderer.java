package vcore.injection;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(WeatherEffectRenderer.class)
public abstract class MixinWorldRenderer {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void renderWeatherHook(MultiBufferSource bufferSource, Vec3 cameraPos, WeatherRenderState renderState, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noWeather.getValue()) {
         ci.cancel();
      }
   }
}
