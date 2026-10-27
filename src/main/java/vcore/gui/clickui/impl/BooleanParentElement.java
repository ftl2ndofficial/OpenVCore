package vcore.gui.clickui.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import vcore.core.Managers;
import vcore.gui.clickui.AbstractElement;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.clickui.ModuleButton;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.SettingControlRenderer;
import vcore.utility.render.TextureStorage;
import vcore.utility.render.animation.AnimationUtility;

public class BooleanParentElement extends AbstractElement {
   private static final float CHECKBOX_RIGHT_PADDING = 22.0F;
   private final ModuleButton owner;
   private final Setting<BooleanSettingGroup> parentSetting;
   float animation;
   float arrowAnimation;

   public BooleanParentElement(ModuleButton owner, Setting<BooleanSettingGroup> setting) {
      super(setting);
      this.owner = owner;
      this.parentSetting = setting;
   }

   @Override
   public void init() {
      this.animation = this.getParentSetting().getValue().isEnabled() ? 1.0F : 0.0F;
      this.arrowAnimation = this.getParentSetting().getValue().isExtended() ? 0.0F : 1.0F;
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      PoseStack matrixStack = Render2DEngine.toPoseStack(context.pose());
      float tx = this.x + this.width - 11.0F;
      float ty = this.y + 7.5F;
      this.drawGroupGuide(context, this.height);
      this.arrowAnimation = AnimationUtility.fast(this.arrowAnimation, this.getParentSetting().getValue().isExtended() ? 0.0F : 1.0F, 15.0F);
      context.pose().pushMatrix();
      context.pose().translate(tx, ty);
      context.pose().rotate((float) Math.toRadians(-180.0F * this.arrowAnimation));
      context.pose().translate(-tx, -ty);
      context.pose().translate(this.x + this.width - 14.0F, this.y + 4.5F);
      Render2DEngine.drawTexture(Render2DEngine.toPoseStack(context.pose()), TextureStorage.guiArrow, 0.0F, 0.0F, 6.0F, 6.0F);
      context.pose().popMatrix();
      FontRenderers.sf_medium_mini
         .drawString(matrixStack, this.setting.getName(), this.getSettingNameX(), this.y + this.height / 2.0F - 1.0F, new Color(-1).getRGB());
      this.animation = AnimationUtility.fast(this.animation, this.getParentSetting().getValue().isEnabled() ? 1.0F : 0.0F, 15.0F);
      float checkboxX = this.x + this.width - 10.0F - 22.0F;
      float checkboxY = this.y + this.height / 2.0F - 5.0F;
      SettingControlRenderer.drawCheckbox(matrixStack, checkboxX, checkboxY, this.animation);
      if (Render2DEngine.isHovered(mouseX, mouseY, checkboxX, checkboxY, 10.0, 10.0)) {
         if (GLFW.glfwGetPlatform() != 393219) {
            Render2DEngine.setCursor(221188);
         }

         ClickGUI.anyHovered = true;
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      if (this.hovered) {
         if (button == 0) {
            this.getParentSetting().getValue().setEnabled(!this.getParentSetting().getValue().isEnabled());
            Managers.SOUND.playBoolean();
         } else {
            boolean opening = !this.getParentSetting().getValue().isExtended();
            if (opening) {
               this.owner.closeSiblingSettingPanels(this.getParentSetting());
            }

            this.getParentSetting().getValue().setExtended(opening);
            if (opening) {
               Managers.SOUND.playSwipeIn();
            } else {
               Managers.SOUND.playSwipeOut();
            }
         }
      }

      super.mouseClicked(mouseX, mouseY, button);
   }

   public Setting<BooleanSettingGroup> getParentSetting() {
      return this.parentSetting;
   }
}
