package vcore.features.hud.impl;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import vcore.features.hud.HudElement;
import vcore.features.modules.render.HudEditor;
import vcore.setting.impl.PositionSetting;

public class InventoryHud extends HudElement {
   public InventoryHud(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("InventoryHud", 60, 25, position, activeElementsSupplier);
   }

   @Override
   public void render(GuiGraphics context) {
      super.render(context);
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         float x = this.getPosX();
         float y = this.getPosY();
         float s = 0.9F;
         int slotSize = 18;
         int rows = 3;
         int cols = 9;
         int gap = Math.max(1, Math.round(2.0F * s));
         int bgAlpha = Math.round(200.0F * HudEditor.getAlpha()) & 0xFF;
         int bgColor = bgAlpha << 24;
         int cellSize = Math.round(slotSize * s);
         int totalW = cols * cellSize + (cols - 1) * gap;
         int totalH = rows * cellSize + (rows - 1) * gap;

         for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
               int itemX = Math.round(x + col * (cellSize + gap));
               int itemY = Math.round(y + row * (cellSize + gap));
               context.fill(itemX, itemY, itemX + cellSize, itemY + cellSize, bgColor);
            }
         }

         context.pose().pushMatrix();

         for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
               int slot = 9 + row * 9 + col;
               ItemStack stack = mc.player.getInventory().getItem(slot);
               if (!stack.isEmpty()) {
                  float itemX = x + col * (cellSize + gap);
                  float itemY = y + row * (cellSize + gap);
                  context.pose().pushMatrix();
                  context.pose().translate(itemX, itemY);
                  context.pose().scale(s, s);
                  context.renderItem(stack, 0, 0);
                  context.renderItemDecorations(mc.font, stack, 0, 0);
                  context.pose().popMatrix();
               }
            }
         }

         context.pose().popMatrix();
         this.setBounds((int)x, (int)y, totalW, totalH);
      }
   }
}
