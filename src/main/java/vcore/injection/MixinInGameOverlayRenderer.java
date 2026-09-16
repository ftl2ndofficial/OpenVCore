package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(ScreenEffectRenderer.class)
public class MixinInGameOverlayRenderer {
   @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
   private static void renderFireOverlayHook(Minecraft minecraftClient, PoseStack matrixStack, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fireOverlay.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
   private static void renderUnderwaterOverlayHook(Minecraft minecraftClient, PoseStack matrixStack, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.waterOverlay.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
   private static void renderInWallOverlayHook(TextureAtlasSprite sprite, PoseStack matrices, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.blockOverlay.getValue()) {
         ci.cancel();
      }
   }
}
