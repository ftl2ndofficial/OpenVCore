package vcore.features.hud.impl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlStateManager;
import vcore.utility.render.RenderSystem;
import vcore.utility.render.RenderSystem.SourceFactor;
import vcore.utility.render.RenderSystem.DestFactor;
import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import vcore.features.hud.HudElement;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.setting.impl.PositionSetting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;

public class Coords extends HudElement {
   public Coords(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("Coords", 100, 10, position, activeElementsSupplier);
   }

   @Override
   public void render(GuiGraphics context) {
      super.render(context);
      int posX = (int)mc.player.getX();
      int posY = (int)mc.player.getY();
      int posZ = (int)mc.player.getZ();
      String coordinates = "XYZ " + ChatFormatting.WHITE + posX + " " + posY + " " + posZ;
      float width = FontRenderers.getModulesRenderer().getStringWidth(coordinates) + 21.0F;
      float pX = this.getPosX() > mc.getWindow().getGuiScaledWidth() / 2.0F ? this.getPosX() - width : this.getPosX();
      Render2DEngine.drawHudBase(Render2DEngine.toPoseStack(context.pose()), pX, this.getPosY(), width, 13.0F, 3.0F);
      Render2DEngine.drawRect(Render2DEngine.toPoseStack(context.pose()), pX + 14.0F, this.getPosY() + 2.0F, 0.5F, 8.0F, new Color(1157627903, true));
      Render2DEngine.setupRender();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      RenderSystem.setShaderTexture(0, TextureStorage.coordsIcon);
      Render2DEngine.renderGradientTexture(
         Render2DEngine.toPoseStack(context.pose()),
         pX + 2.0F,
         this.getPosY() + 1.0F,
         10.0,
         10.0,
         0.0F,
         0.0F,
         512.0,
         512.0,
         512.0,
         512.0,
         HudEditor.getColor(270),
         HudEditor.getColor(0),
         HudEditor.getColor(180),
         HudEditor.getColor(90)
      );
      Render2DEngine.endRender();
      FontRenderers.getModulesRenderer().drawString(context.pose(), coordinates, pX + 18.0F, this.getPosY() + 5.0F, HudEditor.getColor(1).getRGB());
      this.setBounds(pX, this.getPosY(), width, 13.0F);
   }
}
