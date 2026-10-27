package vcore.gui.clickui.impl;

import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import vcore.core.Managers;
import vcore.gui.clickui.AbstractElement;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.SettingControlRenderer;
import vcore.utility.render.animation.AnimationUtility;

public class BooleanElement extends AbstractElement {
   private static final float CHECKBOX_RIGHT_PADDING = 7.0F;
   float animation = 0.0F;

   public BooleanElement(Setting setting) {
      super(setting);
   }

   @Override
   public void init() {
      this.animation = (Boolean)this.setting.getValue() ? 1.0F : 0.0F;
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.drawGroupGuide(context, this.height);
      this.animation = AnimationUtility.fast(this.animation, (Boolean)this.setting.getValue() ? 1.0F : 0.0F, 20.0F);
      float checkboxX = this.x + this.width - 10.0F - 7.0F;
      float checkboxY = this.y + this.height / 2.0F - 5.0F;
      com.mojang.blaze3d.vertex.PoseStack matrices = Render2DEngine.toPoseStack(context.pose());
      SettingControlRenderer.drawCheckbox(matrices, checkboxX, checkboxY, this.animation);
      if (Render2DEngine.isHovered(mouseX, mouseY, checkboxX, checkboxY, 10.0, 10.0)) {
         if (GLFW.glfwGetPlatform() != 393219) {
            Render2DEngine.setCursor(221188);
         }

         ClickGUI.anyHovered = true;
      }

      FontRenderers.sf_medium_mini
         .drawString(matrices, this.setting.getName(), this.getSettingNameX(), this.y + this.height / 2.0F - 3.0F + 2.0F, new Color(-1).getRGB());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      if (this.hovered && button == 0) {
         this.setting.setValue(!(Boolean)this.setting.getValue());
         Managers.SOUND.playBoolean();
      }

      super.mouseClicked(mouseX, mouseY, button);
   }
}
