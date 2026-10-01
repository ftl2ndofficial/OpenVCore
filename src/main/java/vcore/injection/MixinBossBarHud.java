package vcore.injection;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(BossHealthOverlay.class)
public class MixinBossBarHud {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void renderHook(GuiGraphics graphics, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.bossbar.getValue()) {
         ci.cancel();
      }
   }
}
