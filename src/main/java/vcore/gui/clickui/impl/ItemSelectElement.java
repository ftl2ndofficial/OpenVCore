package vcore.gui.clickui.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import vcore.core.Managers;
import vcore.core.manager.IManager;
import vcore.gui.clickui.AbstractElement;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.utility.render.Render2DEngine;

public class ItemSelectElement extends AbstractElement {
   private final Setting<ItemSelectSetting> setting;

   public ItemSelectElement(Setting setting) {
      super(setting);
      this.setting = setting;
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.drawGroupGuide(context, this.height);
      PoseStack matrixStack = Render2DEngine.toPoseStack(context.pose());
      FontRenderers.icons.drawString(matrixStack, "H", this.x + this.width - 14.0F, this.y + 6.0F, new Color(-1250068, true).getRGB());
      FontRenderers.sf_medium_mini
         .drawString(matrixStack, this.setting.getName(), this.getSettingNameX(), this.y + this.height / 2.0F - 1.0F, new Color(-1).getRGB());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      if (this.hovered) {
         IManager.mc.setScreen(new ItemSelectScreen(IManager.mc.screen, this.getItemSetting()));
         Managers.SOUND.playSwipeIn();
      }

      super.mouseClicked(mouseX, mouseY, button);
   }

   public Setting<ItemSelectSetting> getItemSetting() {
      return this.setting;
   }
}
