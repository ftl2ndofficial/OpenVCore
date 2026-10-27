package vcore.injection;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.clickui.ManagerScreen;
import vcore.gui.clickui.impl.ItemSelectScreen;
import vcore.gui.misc.DialogScreen;

@Mixin(Gui.class)
public abstract class MixinInGameHud {
   @Inject(at = @At("HEAD"), method = "render")
   public void renderHook(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
   }

   @Inject(at = @At("HEAD"), method = "renderSelectedItemName", cancellable = true)
   public void renderHeldItemTooltipHook(GuiGraphics context, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.hotbarItemName.getValue()) {
         ci.cancel();
      }
   }

   @Inject(at = @At("HEAD"), method = "renderEffects", cancellable = true)
   public void renderStatusEffectOverlayHook(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (ModuleManager.hudEditor.isHudEnabled("PotionHud")) {
         ci.cancel();
      }
   }

   @Inject(
      method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void renderScoreboardSidebarHook(GuiGraphics context, Objective objective, CallbackInfo ci) {
      if ((ModuleManager.noRender.noScoreBoard.getValue() && ModuleManager.noRender.isEnabled())
         || Module.mc.screen instanceof ClickGUI
         || Module.mc.screen instanceof ManagerScreen
         || Module.mc.screen instanceof DialogScreen
         || Module.mc.screen instanceof ItemSelectScreen) {
         ci.cancel();
      }
   }

   @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
   private void renderVignetteOverlayHook(GuiGraphics context, Entity camera, CallbackInfo ci) {
      if (ModuleManager.noRender.vignette.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
   private void renderPortalOverlayHook(GuiGraphics context, float nauseaStrength, CallbackInfo ci) {
      if (ModuleManager.noRender.portal.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   public void renderCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (ModuleManager.crosshair.isEnabled()) {
         ci.cancel();
      }
   }
}
