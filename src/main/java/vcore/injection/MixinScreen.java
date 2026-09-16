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
   @Shadow
   public abstract void init(Minecraft var1, int var2, int var3);

   @Inject(
      method = "handleComponentClicked",
      at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V", ordinal = 1, remap = false),
      cancellable = true
   )
   private void onRunCommand(Style style, CallbackInfoReturnable<Boolean> cir) {
      if (Objects.requireNonNull(style.getClickEvent()) instanceof ClientClickEvent clientClickEvent
         && clientClickEvent.getValue().startsWith(Managers.COMMAND.getPrefix())) {
         try {
            CommandManager manager = Managers.COMMAND;
            manager.getDispatcher().execute(style.getClickEvent().getValue().substring(Managers.COMMAND.getPrefix().length()), manager.getSource());
            cir.setReturnValue(true);
         } catch (CommandSyntaxException var5) {
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
   private void renderInGameBackground(CallbackInfo info) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.disableGuiBackGround.getValue()) {
         info.cancel();
      }
   }

   @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
   public void onRenderBackground(GuiGraphics context, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.disableGuiBackGround.getValue() && Module.mc.level != null) {
         ci.cancel();
      }
   }
}
