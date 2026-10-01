package vcore.injection;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.Managers;
import vcore.core.manager.client.CommandManager;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.ClientClickEvent;
import vcore.features.modules.Module;
import vcore.gui.misc.DialogScreen;
import vcore.utility.render.TextureStorage;

@Mixin(Screen.class)
public abstract class MixinScreen {
   @Inject(method = "defaultHandleClickEvent", at = @At("HEAD"), cancellable = true)
   private static void onRunCommand(net.minecraft.network.chat.ClickEvent event, Minecraft minecraft, Screen activeScreen, CallbackInfo ci) {
      if (event instanceof ClientClickEvent clientClickEvent
         && clientClickEvent.value().startsWith(Managers.COMMAND.getPrefix())) {
         try {
            CommandManager manager = Managers.COMMAND;
            manager.getDispatcher().execute(clientClickEvent.value().substring(Managers.COMMAND.getPrefix().length()), manager.getSource());
            ci.cancel();
         } catch (CommandSyntaxException ignored) {
         }
      }
   }

   @Inject(method = "onFilesDrop", at = @At("HEAD"))
   public void filesDragged(List<Path> paths, CallbackInfo ci) {
      String configPath = paths.get(0).toString();
      File cfgFile = new File(configPath);
      String fileName = cfgFile.getName();
      if (fileName.contains(".vc")) {
         DialogScreen dialogScreen = new DialogScreen(
            TextureStorage.setting, "Config detected!", "Are you sure you want to load " + fileName + "?", "Yes", "No", () -> {
               Managers.MODULE.onUnload("none");
               Managers.CONFIG.load(cfgFile);
               Managers.MODULE.onLoad("none");
               Module.mc.setScreen(null);
            }, () -> Module.mc.setScreen(null)
         );
         Module.mc.setScreen(dialogScreen);
      }
   }

   @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
   private void renderInGameBackground(GuiGraphics graphics, CallbackInfo info) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.disableGuiBackGround.getValue()) {
         info.cancel();
      }
   }

   @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
   public void onExtractBackground(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.disableGuiBackGround.getValue() && Module.mc.level != null) {
         ci.cancel();
      }
   }
}
