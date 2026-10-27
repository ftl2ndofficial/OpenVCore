package vcore.gui.misc;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.Render2DEngine;

public class DialogScreen extends Screen {
   private final Identifier pic;
   private final String header;
   private final String description;
   private final String yesText;
   private final String noText;
   private final Runnable yesAction;
   private final Runnable noAction;

   public DialogScreen(Identifier pic, String header, String description, String yesText, String noText, Runnable yesAction, Runnable noAction) {
      super(Component.nullToEmpty("ThDialogScreen"));
      this.pic = pic;
      this.header = header;
      this.description = description;
      this.yesText = yesText;
      this.noText = noText;
      this.yesAction = yesAction;
      this.noAction = noAction;
   }

   @Override
   public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
      if (Module.fullNullCheck()) {
         super.renderBackground(context, mouseX, mouseY, delta);
      }
   }

   @Override
   public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
      if (!Render2DEngine.renderingPostGui) {
         return;
      }

      float mainX = Module.mc.getWindow().getGuiScaledWidth() / 2.0F - 100.0F;
      float mainY = Module.mc.getWindow().getGuiScaledHeight() / 2.0F - 70.0F;
      float mainWidth = 200.0F;
      float mainHeight = 140.0F;
      PoseStack matrices = Render2DEngine.toPoseStack(context.pose());
      Render2DEngine.drawHudBase(matrices, mainX, mainY, mainWidth, mainHeight, 10.0F);
      Render2DEngine.drawHudBase(matrices, mainX + 5.0F, mainY + mainHeight - 25.0F, mainWidth / 2.0F - 7.5F, 20.0F, 5.0F);
      FontRenderers.sf_medium
         .drawCenteredString(matrices, this.yesText, mainX + 5.0F + (mainWidth / 2.0F - 7.5F) / 2.0F, mainY + mainHeight - 18.0F, this.yesColor(mouseX, mouseY));
      Render2DEngine.drawHudBase(matrices, mainX + mainWidth / 2.0F + 2.5F, mainY + mainHeight - 25.0F, mainWidth / 2.0F - 7.5F, 20.0F, 5.0F);
      FontRenderers.sf_medium
         .drawCenteredString(
            matrices,
            this.noText,
            mainX + mainWidth / 2.0F + 2.5F + (mainWidth / 2.0F - 7.5F) / 2.0F,
            mainY + mainHeight - 18.0F,
            this.noColor(mouseX, mouseY)
         );
      FontRenderers.sf_medium.drawCenteredString(matrices, this.header, mainX + mainWidth / 2.0F, mainY + 5.0F, -1);
      FontRenderers.sf_medium.drawCenteredString(matrices, this.description, mainX + mainWidth / 2.0F, mainY + 18.0F, new Color(-1543503873, true).getRGB());
      Render2DEngine.drawTexture(matrices, this.pic, mainX + mainWidth / 2.0F - 35.0F, mainY + 25.0F, 70.0F, 65.0F);
   }

   private boolean isHoveredYes(double mouseX, double mouseY) {
      float mainX = Module.mc.getWindow().getGuiScaledWidth() / 2.0F - 100.0F;
      float mainY = Module.mc.getWindow().getGuiScaledHeight() / 2.0F - 70.0F;
      return Render2DEngine.isHovered(mouseX, mouseY, mainX + 5.0F, mainY + 140.0F - 25.0F, 92.5, 20.0);
   }

   private int yesColor(double mouseX, double mouseY) {
      return this.isHoveredYes(mouseX, mouseY) ? HudEditor.getColor(1).getRGB() : -1;
   }

   private boolean isHoveredNo(double mouseX, double mouseY) {
      float mainX = Module.mc.getWindow().getGuiScaledWidth() / 2.0F - 100.0F;
      float mainY = Module.mc.getWindow().getGuiScaledHeight() / 2.0F - 70.0F;
      return Render2DEngine.isHovered(mouseX, mouseY, mainX + 100.0F + 2.5F, mainY + 140.0F - 25.0F, 92.5, 20.0);
   }

   private int noColor(double mouseX, double mouseY) {
      return this.isHoveredNo(mouseX, mouseY) ? HudEditor.getColor(210).getRGB() : -1;
   }

   @Override
   public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
      if (this.isHoveredYes(event.x(), event.y())) {
         this.yesAction.run();
      } else if (this.isHoveredNo(event.x(), event.y())) {
         this.noAction.run();
      }

      return super.mouseClicked(event, doubleClick);
   }
}
