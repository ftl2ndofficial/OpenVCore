package vcore.gui.misc;

import java.awt.Color;
import java.util.ArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.Render2DEngine;

public class GuiScanner extends Screen {
   public static boolean neartrack = false;
   public static boolean track = false;
   public static boolean busy = false;
   public ArrayList<String> consoleout = new ArrayList<>();
   int radarx;
   int radary;
   int radarx1;
   int radary1;
   int centerx;
   int centery;
   int consolex;
   int consoley;
   int consolex1;
   int consoley1;
   int hovery;
   int hoverx;
   int searchx;
   int searchy;
   int wheely;

   public GuiScanner() {
      super(Component.nullToEmpty("GuiScanner"));
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      if (Module.mc.player != null) {
         this.radarx = Module.mc.getWindow().getGuiScaledWidth() / 8;
         this.radarx1 = Module.mc.getWindow().getGuiScaledWidth() * 5 / 8;
         this.radary = Module.mc.getWindow().getGuiScaledHeight() / 2 - (this.radarx1 - this.radarx) / 2;
         this.radary1 = Module.mc.getWindow().getGuiScaledHeight() / 2 + (this.radarx1 - this.radarx) / 2;
         this.centerx = (this.radarx + this.radarx1) / 2;
         this.centery = (this.radary + this.radary1) / 2;
         this.consolex = (int)(Module.mc.getWindow().getGuiScaledWidth() * 5.5F / 8.0F);
         this.consolex1 = Module.mc.getWindow().getGuiScaledWidth() - 50;
         this.consoley = this.radary;
         this.consoley1 = this.radary1 - 50;
         PoseStack matrices = Render2DEngine.toPoseStack(context.pose());
         Render2DEngine.drawRectDumbWay(matrices, this.consolex, this.consoley, this.consolex1, this.consoley1, new Color(-150205428, true));
         Render2DEngine.drawRectDumbWay(
            matrices, this.consolex, this.consoley1 + 3, this.consolex1, this.consoley1 + 17, new Color(-150205428, true)
         );
         FontRenderers.monsterrat
            .drawString(matrices, "cursor pos: " + this.hoverx * 64 + "x  " + this.hovery * 64 + "z", this.consolex + 4, this.consoley1 + 6, -1);
         if (!track) {
            Render2DEngine.drawRectDumbWay(
               matrices, this.consolex, this.consoley1 + 20, this.consolex1, this.consoley1 + 35, new Color(-150205428, true)
            );
            FontRenderers.monsterrat.drawString(matrices, "tracker off", this.consolex + 4, this.consoley1 + 26, -1);
         } else {
            Render2DEngine.drawRectDumbWay(
               matrices, this.consolex, this.consoley1 + 20, this.consolex1, this.consoley1 + 35, new Color(-144810402, true)
            );
            FontRenderers.monsterrat.drawString(matrices, "tracker on", this.consolex + 4, this.consoley1 + 26, -1);
         }

         Render2DEngine.drawRectDumbWay(
            matrices, this.consolex, this.consoley1 + 38, this.consolex1, this.consoley1 + 53, new Color(-150205428, true)
         );
         FontRenderers.monsterrat.drawString(matrices, "clear console", this.consolex + 4, this.consoley1 + 42, -1);
         Render2DEngine.drawRectDumbWay(matrices, this.radarx, this.radary, this.radarx1, this.radary1, new Color(-535489259, true));
         Render2DEngine.drawRectDumbWay(
            matrices, this.centerx - 1.0F, this.centery - 1.0F, this.centerx + 1.0F, this.centery + 1.0F, new Color(16712451)
         );
         Render2DEngine.drawRectDumbWay(
            matrices,
            (float)(Module.mc.player.getX() / 16.0 / 4.0 + this.centerx),
            (float)(Module.mc.player.getZ() / 16.0 / 4.0 + this.centery),
            (float)(Module.mc.player.getX() / 16.0 / 4.0 + (this.radarx1 - this.radarx) / 300.0F + this.centerx),
            (float)(Module.mc.player.getZ() / 16.0 / 4.0 + (this.radary1 - this.radary) / 300.0F + this.centery),
            new Color(4863)
         );
         if (mouseX > this.radarx && mouseX < this.radarx1 && mouseY > this.radary && mouseY < this.radary1) {
            this.hoverx = mouseX - this.centerx;
            this.hovery = mouseY - this.centery;
         }

         Render2DEngine.addWindow(matrices, this.consolex, this.consoley, this.consolex1, this.consoley1 - 10, 1.0);

         for (int i = 0; i < this.consoleout.size(); i++) {
            FontRenderers.monsterrat.drawString(matrices, this.consoleout.get(i), this.consolex + 4, this.consoley + 6 + i * 11 + this.wheely, -1);
         }

         Render2DEngine.popWindow();
         FontRenderers.monsterrat.drawString(matrices, "X+", this.radarx1 + 5, this.centery, -1);
         FontRenderers.monsterrat.drawString(matrices, "X-", this.radarx - 15, this.centery, -1);
         FontRenderers.monsterrat.drawString(matrices, "Y+", this.centerx, this.radary1 + 5, -1);
         FontRenderers.monsterrat.drawString(matrices, "Y-", this.centerx, this.radary - 8, -1);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      double mouseX = event.x();
      double mouseY = event.y();
      if (mouseX > this.radarx && mouseX < this.radarx1 && mouseY > this.radary && mouseY < this.radary1) {
         busy = true;
         this.searchx = (int)(mouseX - this.centerx);
         this.searchy = (int)(mouseY - this.centery);
         this.consoleout.add("Selected pos " + this.searchx * 65 + "x " + this.searchy * 64 + "z ");
      }

      if (mouseX > this.consolex && mouseX < this.consolex1 && mouseY > this.consoley1 + 20 && mouseY < this.consoley1 + 36) {
         track = !track;
      }

      if (mouseX > this.consolex && mouseX < this.consolex1 && mouseY > this.consoley1 + 38 && mouseY < this.consoley1 + 53) {
         this.consoleout.clear();
      }

      return super.mouseClicked(event, doubleClick);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.wheely += (int)(verticalAmount * 5.0);
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }
}
