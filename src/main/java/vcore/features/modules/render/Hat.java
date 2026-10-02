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

      float radius = 0.48F;
      float height = 0.28F;
      int steps = 48;
      double angleStep = (Math.PI * 2) / steps;
      Color customHatColor = this.color.getValue().getColorObject();
      int apexA = 240;
      int baseA = 160;

      submitNodeCollector.submitCustomGeometry(stack, RenderTypes.dragonRays(), (pose, consumer) -> {
         for (int i = 0; i < steps; i++) {
            float x1 = (float)(Math.cos(i * angleStep) * radius);
            float z1 = (float)(Math.sin(i * angleStep) * radius);
            float x2 = (float)(Math.cos((i + 1) * angleStep) * radius);
            float z2 = (float)(Math.sin((i + 1) * angleStep) * radius);

            Color apexColor = this.getHatColor((i + 0.5F) / steps, customHatColor);
            Color baseColor1 = this.getHatColor((float)i / steps, customHatColor);
            Color baseColor2 = this.getHatColor((float)(i + 1) / steps, customHatColor);

            // Exterior face
            consumer.addVertex(pose, 0.0F, height, 0.0F).setColor(apexColor.getRed(), apexColor.getGreen(), apexColor.getBlue(), apexA);
            consumer.addVertex(pose, x1, 0.0F, z1).setColor(baseColor1.getRed(), baseColor1.getGreen(), baseColor1.getBlue(), baseA);
            consumer.addVertex(pose, x2, 0.0F, z2).setColor(baseColor2.getRed(), baseColor2.getGreen(), baseColor2.getBlue(), baseA);

            // Interior face (visible from underneath)
            consumer.addVertex(pose, 0.0F, height, 0.0F).setColor(apexColor.getRed(), apexColor.getGreen(), apexColor.getBlue(), apexA);
            consumer.addVertex(pose, x2, 0.0F, z2).setColor(baseColor2.getRed(), baseColor2.getGreen(), baseColor2.getBlue(), baseA);
            consumer.addVertex(pose, x1, 0.0F, z1).setColor(baseColor1.getRed(), baseColor1.getGreen(), baseColor1.getBlue(), baseA);
         }
      });
   }

   public static float getYOffset(Entity entity) {
      float offset = -0.50F;
      if (!Boolean.TRUE.equals(ModuleManager.hat.autoOffset.getValue())) {
         return offset;
      }

      if (entity instanceof LivingEntity livingEntity && !livingEntity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
         offset -= 0.07F;
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
