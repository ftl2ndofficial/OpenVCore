package vcore.gui.clickui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.gui.clickui.impl.ManagerPanel;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.shaders.BlurProgram;

public class ManagerScreen extends Screen {
   private final Screen parent;
   private final ManagerPanel panel;

   public ManagerScreen(Screen parent) {
      super(Component.nullToEmpty("ManagerScreen"));
      this.parent = parent;
      this.panel = new ManagerPanel();
      this.panel.setOpen(true);
   }

   public ManagerPanel getPanel() {
      return this.panel;
   }

   @Override
   public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
      if (Module.fullNullCheck()) {
         super.renderBackground(context, mouseX, mouseY, delta);
      }
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      if (!Render2DEngine.renderingPostGui) {
         return;
      }

      if (!Module.fullNullCheck() && ModuleManager.clickGui.blur.getValue()) {
         Module.mc.gameRenderer.processBlurEffect();
         com.mojang.blaze3d.vertex.BufferUploader.invalidateCommandEncoder();
         BlurProgram.beginFrame();
      }

      this.panel.setSize(400.0F, 250.0F);
      this.panel.render(context, mouseX, mouseY, delta);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      return this.panel.mouseClicked((int)event.x(), (int)event.y(), event.button()) ? true : super.mouseClicked(event, doubleClick);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.panel.mouseScrolled(mouseX, mouseY, verticalAmount);
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      int keyCode = event.key();
      if (this.panel.keyPressed(keyCode)) {
         return true;
      } else if (keyCode == 256) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      char chr = (char)event.codepoint();
      return this.panel.charTyped(chr) ? true : super.charTyped(event);
   }

   @Override
   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }
}
