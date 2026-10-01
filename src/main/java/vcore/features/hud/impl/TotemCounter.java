package vcore.features.hud.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vcore.events.impl.TotemPopEvent;
import vcore.features.hud.HudElement;
import vcore.gui.font.FontRenderers;
import vcore.setting.impl.PositionSetting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.RenderSystem;

public class TotemCounter extends HudElement {
   private float angle;

   public TotemCounter(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("TotemCounter", 0, 0, position, activeElementsSupplier);
   }

   @Override
   public void render(GuiGraphics context) {
      super.render(context);
      int count = this.getItemCount(Items.TOTEM_OF_UNDYING);
      if (count != 0) {
         int width = mc.getWindow().getGuiScaledWidth();
         int height = mc.getWindow().getGuiScaledHeight();
         int u = mc.player.getMaxAirSupply();
         int v = Math.min(mc.player.getAirSupply(), u);
         int adjustment = !mc.player.isUnderWater() && v >= u ? 0 : 10;
         float factor = Math.abs(this.angle < 0.0F ? this.angle / 15.0F : 0.0F);
         int center = width / 2;
         int y = height - 55 - adjustment;
         int x = center - 8;
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         context.renderItem(Items.TOTEM_OF_UNDYING.getDefaultInstance(), x, y);
         context.renderItemDecorations(mc.font, Items.TOTEM_OF_UNDYING.getDefaultInstance(), x, y);
         RenderSystem.setShaderColor(1.0F, 1.0F - factor, 1.0F - factor, 1.0F);
         if (factor > 0.0F) {
            PoseStack matrices = Render2DEngine.toPoseStack(context.pose());
            Render2DEngine.drawBlurredShadow(
               matrices, x - 6, y - 5, 11.0F, 11.0F, 8, Render2DEngine.injectAlpha(new Color(16711680), (int)(255.0F * factor))
            );
         }

         String sCount = count + "";
         float textX = center - FontRenderers.sf_bold.getStringWidth(sCount) / 2.0F;
         float textY = y - 6;
         FontRenderers.sf_bold.drawString(context.pose(), sCount, textX, textY, -1);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public void onTotemPop(TotemPopEvent e) {
      if (e.getEntity() == mc.player) {
         this.angle = -15.0F;
      }
   }

   @Override
   public void tick() {
      if (this.angle < 0.0F) {
         this.angle++;
      }
   }

   public int getItemCount(Item item) {
      if (mc.player == null) {
         return 0;
      }

      int n = 0;
      int n2 = 44;

      for (int i = 0; i <= n2; i++) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (itemStack.getItem() == item) {
            n += itemStack.getCount();
         }
      }

      return n;
   }
}
