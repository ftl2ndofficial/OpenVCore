package vcore.features.hud.impl;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import vcore.features.hud.HudElement;
import vcore.gui.font.FontRenderers;
import vcore.setting.impl.PositionSetting;
import vcore.utility.math.MathUtility;

public class ArmorHud extends HudElement {
   public ArmorHud(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("ArmorHud", 60, 25, position, activeElementsSupplier);
   }

   @Override
   public void render(GuiGraphics context) {
      if (mc.player != null) {
         int u = mc.player.getMaxAirSupply();
         int v = Math.min(mc.player.getAirSupply(), u);
         int y = mc.getWindow().getGuiScaledHeight() - 55 - (!mc.player.isUnderWater() && v >= u ? 0 : 10);
         int i = 0;

         for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlotGroup.ARMOR) {
            ItemStack is = mc.player.getItemBySlot(slot);
            i++;
            if (!is.isEmpty()) {
               int x = mc.getWindow().getGuiScaledWidth() / 2 - 90 + (9 - i) * 20 + 2;
               context.renderItem(is, x, y);
               context.renderItemDecorations(mc.font, is, x, y);
               String s = is.getCount() > 1 ? is.getCount() + "" : "";
               float countX = x + 19 - 2 - FontRenderers.sf_bold.getStringWidth(s);
               float countY = y + 6;
               FontRenderers.sf_bold.drawString(context.pose(), s, countX, countY, 16777215);
               float green = (float)(is.getMaxDamage() - is.getDamageValue()) / is.getMaxDamage();
               float red = 1.0F - green;
               int dmg = 100 - (int)(red * 100.0F);
               int color = new Color((int)MathUtility.clamp(red * 255.0F, 0.0F, 255.0F), (int)MathUtility.clamp(green * 255.0F, 0.0F, 255.0F), 0).getRGB();
               String dmgStr = dmg + "";
               float dmgX = x + 8 - FontRenderers.sf_bold.getStringWidth(dmgStr) / 2.0F;
               float dmgY = y - 6;
               FontRenderers.sf_bold.drawString(context.pose(), dmgStr, dmgX, dmgY, color);
            }
         }
      }
   }
}
