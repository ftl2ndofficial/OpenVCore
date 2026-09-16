package vcore.injection;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
   protected MixinTitleScreen(Component title) {
      super(title);
   }

   @Inject(method = "init", at = @At("RETURN"))
   public void postInitHook(CallbackInfo ci) {
      if (ModuleManager.clickGui.getBind().getKey() == -1) {
         ModuleManager.clickGui.setBind(InputConstants.getKey("key.keyboard.right.shift").getValue(), false, false);
      }
   }
}
