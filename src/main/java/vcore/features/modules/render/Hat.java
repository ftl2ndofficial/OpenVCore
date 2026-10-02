package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.render.Render2DEngine;

public class Hat extends Module {
   public Setting<Boolean> autoOffset = new Setting<>("AutoOffset", false);
   public Setting<Boolean> syncColor = new Setting<>("SyncColor", true);
   public Setting<ColorSetting> color = new Setting<>(
      "Color", new ColorSetting(new Color(10, 255, 135, 255).getRGB()).withoutAlpha(), v -> !this.syncColor.getValue()
   );

   public Hat() {
      super("Hat", "Renders a cosmetic hat above players.", Module.Category.RENDER);
   }

   public void renderHat(PoseStack stack, SubmitNodeCollector submitNodeCollector) {
      if (mc.player == null || submitNodeCollector == null) {
         return;
      }

      float radius = 0.6F;
      int steps = 72;
      double angleStep = (Math.PI * 2) / steps;
      Color customHatColor = this.color.getValue().getColorObject();
      int apexA = 240;
      int baseA = 160;

      submitNodeCollector.submitCustomGeometry(stack, RenderTypes.debugTriangleFan(), (pose, consumer) -> {
         Color apexColor = this.getHatColor(0.0F, customHatColor);
         consumer.addVertex(pose, 0.0F, 0.35F, 0.0F).setColor(apexColor.getRed(), apexColor.getGreen(), apexColor.getBlue(), apexA);
         for (int i = 0; i <= steps; i++) {
            float x = (float)(Math.cos(i * angleStep) * radius);
            float z = (float)(Math.sin(i * angleStep) * radius);
            Color baseColor = this.getHatColor((float)i / steps, customHatColor);
            consumer.addVertex(pose, x, 0.0F, z).setColor(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), baseA);
         }
      });
   }

   public static float getYOffset(Entity entity) {
      float offset = -0.41F;
      if (!Boolean.TRUE.equals(ModuleManager.hat.autoOffset.getValue())) {
         return offset;
      }

      if (entity instanceof LivingEntity livingEntity && !livingEntity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
         offset -= 0.071F;
      }

      return offset;
   }

   private Color getHatColor(float fraction, Color fallback) {
      if (!this.syncColor.getValue()) {
         return fallback;
      }

      HudEditor.Theme theme = HudEditor.getCurrentTheme();
      float normalized = fraction - (float)Math.floor(fraction);
      if (normalized < 0.0F) {
         normalized++;
      }

      double count = normalized * 360.0F;
      return Render2DEngine.TwoColoreffect(theme.color1, theme.color2, 15.0, count);
   }
}
