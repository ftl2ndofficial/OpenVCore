package vcore.injection;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.client.ModuleManager;

@Mixin(AvatarRenderer.class)
public class MixinPlayerEntityRenderer {

   @Inject(
      method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;",
      at = @At("RETURN"),
      cancellable = true
   )
   private void getCustomModelTexture(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
      if (ModuleManager.customModel != null && ModuleManager.customModel.isEnabled()) {
         cir.setReturnValue(ModuleManager.customModel.getTexture(state, cir.getReturnValue()));
      }
   }
}
