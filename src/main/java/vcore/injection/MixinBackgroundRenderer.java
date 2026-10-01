package vcore.injection;

import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.WorldTweaks;

@Mixin(FogRenderer.class)
public class MixinBackgroundRenderer {
   @ModifyArgs(
      method = "setupFog",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
      )
   )
   private void onUpdateFogBuffer(Args args) {
      if (ModuleManager.noRender.isEnabled()) {
         if (ModuleManager.noRender.blindness.getValue()) {
            args.set(3, 100000.0F);
            args.set(4, 100000.0F);
         }
         if (ModuleManager.noRender.fog.getValue()) {
            args.set(5, 100000.0F);
            args.set(6, 100000.0F);
         }
      }

      if (ModuleManager.worldTweaks.isEnabled() && WorldTweaks.fogModify.getValue().isEnabled()) {
         args.set(5, (float) WorldTweaks.fogStart.getValue().intValue());
         args.set(6, (float) WorldTweaks.fogEnd.getValue().intValue());
         Vector4f color = args.get(2);
         if (color != null) {
            color.set(
               WorldTweaks.fogColor.getValue().getGlRed(),
               WorldTweaks.fogColor.getValue().getGlGreen(),
               WorldTweaks.fogColor.getValue().getGlBlue(),
               WorldTweaks.fogColor.getValue().getGlAlpha()
            );
         }
      }
   }
}
