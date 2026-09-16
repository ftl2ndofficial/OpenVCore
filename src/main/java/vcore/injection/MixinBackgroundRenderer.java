package vcore.injection;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.WorldTweaks;

@Mixin(FogRenderer.class)
public class MixinBackgroundRenderer {
   @Inject(method = "setupFog", at = @At("TAIL"))
   private static void onApplyFog(Camera camera, FogMode fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo info) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fog.getValue() && fogType == FogMode.FOG_TERRAIN) {
         RenderSystem.setShaderFogStart(viewDistance * 4.0F);
         RenderSystem.setShaderFogEnd(viewDistance * 4.25F);
      }

      if (ModuleManager.worldTweaks.isEnabled() && WorldTweaks.fogModify.getValue().isEnabled()) {
         RenderSystem.setShaderFogStart(WorldTweaks.fogStart.getValue().intValue());
         RenderSystem.setShaderFogEnd(WorldTweaks.fogEnd.getValue().intValue());
         RenderSystem.setShaderFogColor(
            WorldTweaks.fogColor.getValue().getGlRed(), WorldTweaks.fogColor.getValue().getGlGreen(), WorldTweaks.fogColor.getValue().getGlBlue()
         );
      }
   }

   @Inject(
      method = "getPriorityFogFunction(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/FogRenderer$MobEffectFogFunction;",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.blindness.getValue()) {
         info.setReturnValue(null);
      }
   }
}
