package vcore.injection;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(LevelRenderer.class)
public abstract class MixinWorldRenderer {
   @ModifyArg(
      method = "renderLevel",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V"
      ),
      index = 3
   )
   private boolean renderSetupTerrainModifyArg(boolean spectator) {
      return ModuleManager.freeCam.isEnabled() || spectator;
   }

   @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
   private void renderWeatherHook(LightTexture manager, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noWeather.getValue()) {
         ci.cancel();
      }
   }
}
