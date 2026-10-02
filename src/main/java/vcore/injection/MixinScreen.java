package vcore.injection;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
   public abstract void renderTransparentBackground(GuiGraphics graphics);

   @Unique
   private static final float INVENTORY_ANIM_DURATION = 250.0F;
   @Unique
   private long inventoryOpenTime;
   @Unique
   private boolean inventoryAnimActive;

   @Unique
   private static float getBackOutScale(float progress) {
      float x = progress - 1.0F;
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow(x, 3.0) + c1 * (float)Math.pow(x, 2.0);
   }

   @Inject(method = "init(II)V", at = @At("RETURN"))
   private void onScreenInit(int width, int height, CallbackInfo ci) {
      if ((Object)this instanceof InventoryScreen) {
         this.inventoryOpenTime = System.currentTimeMillis();
      }
   }

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
      if (this.inventoryAnimActive || (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.disableGuiBackGround.getValue())) {
         info.cancel();
      }
   }

   @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
   public void onExtractBackground(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.disableGuiBackGround.getValue() && Module.mc.level != null) {
         ci.cancel();
      }
   }

   @Inject(method = "renderWithTooltipAndSubtitles", at = @At("HEAD"))
   private void onRenderWithTooltipAndSubtitlesHead(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if ((Object)this instanceof InventoryScreen
         && Module.mc.gameMode != null
         && Module.mc.gameMode.getPlayerMode() == GameType.SURVIVAL) {
         float progress = Math.min(1.0F, (float)(System.currentTimeMillis() - this.inventoryOpenTime) / INVENTORY_ANIM_DURATION);
         if (progress < 1.0F) {
            if (!(ModuleManager.noRender.isEnabled() && ModuleManager.noRender.disableGuiBackGround.getValue())) {
               this.renderTransparentBackground(graphics);
            }
            float anim = getBackOutScale(progress);
            int centerX = graphics.guiWidth() / 2;
            int centerY = graphics.guiHeight() / 2;
            graphics.pose().pushMatrix();
            graphics.pose().translate(centerX, centerY);
            graphics.pose().scale(anim, anim);
            graphics.pose().translate(-centerX, -centerY);
            this.inventoryAnimActive = true;
         } else {
            this.inventoryAnimActive = false;
         }
      } else {
         this.inventoryAnimActive = false;
      }
   }

   @Inject(method = "renderWithTooltipAndSubtitles", at = @At("TAIL"))
   private void onRenderWithTooltipAndSubtitlesTail(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.inventoryAnimActive) {
         graphics.pose().popMatrix();
         this.inventoryAnimActive = false;
      }
   }
}
