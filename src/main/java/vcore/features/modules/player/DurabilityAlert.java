package vcore.features.modules.player;

import vcore.utility.render.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;
import vcore.core.Managers;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.render.TextureStorage;

public class DurabilityAlert extends Module {
   private final Setting<Boolean> friends = new Setting<>("Friend message", false);
   private final Setting<Integer> percent = new Setting<>("Percent", 20, 1, 100);
   private static final Color WARNING_COLOR = new Color(255, 92, 92);
   private static final int ICON_RENDER_SIZE = 50;
   private static final int ICON_TEXTURE_SIZE = 200;
   private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
   private boolean need_alert = false;
   private final Timer timer = new Timer();

   public DurabilityAlert() {
      super("DurabilityAlert", "Alerts when armor is low.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (this.friends.getValue()) {
         for (Player player : mc.level.players()) {
            if (Managers.FRIEND.isFriend(player) && player != mc.player) {
               for (EquipmentSlot slot : ARMOR_SLOTS) {
                  ItemStack stack = player.getItemBySlot(slot);
                  if (!stack.isEmpty() && stack.has(DataComponents.EQUIPPABLE) && getDurability(stack) < this.percent.getValue() && this.timer.passedMs(30000L)) {
                     mc.player.connection.sendCommand("msg " + player.getName().getString() + " Your armor is about to break!");
                     this.timer.reset();
                  }
               }
            }
         }
      }

      boolean flag = false;

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack stack = mc.player.getItemBySlot(slot);
         if (!stack.isEmpty() && stack.has(DataComponents.EQUIPPABLE) && getDurability(stack) < this.percent.getValue()) {
            this.need_alert = true;
            flag = true;
         }
      }

      if (!flag && this.need_alert) {
         this.need_alert = false;
      }
   }

   @Override
   public void onRender2D(GuiGraphics context) {
      if (this.need_alert) {
         FontRenderers.sf_bold
            .drawCenteredString(
               context.pose(),
               "Armor about to break!",
               mc.getWindow().getGuiScaledWidth() / 2.0F,
               mc.getWindow().getGuiScaledHeight() / 3.0F - 60.0F,
               WARNING_COLOR.getRGB()
            );
         RenderSystem.setShaderColor(WARNING_COLOR.getRed() / 255.0F, WARNING_COLOR.getGreen() / 255.0F, WARNING_COLOR.getBlue() / 255.0F, 1.0F);
         RenderSystem.setShaderTexture(0, TextureStorage.brokenShield);
         context.blit(
            RenderPipelines.GUI_TEXTURED,
            TextureStorage.brokenShield,
            (int)(mc.getWindow().getGuiScaledWidth() / 2.0F - 25.0F),
            (int)(mc.getWindow().getGuiScaledHeight() / 3.0F - 120.0F),
            0.0F,
            0.0F,
            50,
            50,
            200,
            200
         );
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public static int getDurability(ItemStack stack) {
      return (int)((stack.getMaxDamage() - stack.getDamageValue()) / Math.max(0.1, stack.getMaxDamage()) * 100.0);
   }
}
