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

      float outerRadius = 0.52F;
      float ridgeRadius = 0.44F;
      float coneHeight = 0.26F;
      float rimHeight = 0.03F;
      float tipHeight = 0.29F;
      float tipRadius = 0.05F;
      int steps = 48;
      double angleStep = (Math.PI * 2) / steps;
      Color customHatColor = this.color.getValue().getColorObject();
      int apexA = 245;
      int bodyA = 175;
      int rimA = 240;
      int underA = 140;

      submitNodeCollector.submitCustomGeometry(stack, RenderTypes.dragonRays(), (pose, consumer) -> {
         for (int i = 0; i < steps; i++) {
            double a1 = i * angleStep;
            double a2 = (i + 1) * angleStep;

            float x1_out = (float)(Math.cos(a1) * outerRadius);
            float z1_out = (float)(Math.sin(a1) * outerRadius);
            float x2_out = (float)(Math.cos(a2) * outerRadius);
            float z2_out = (float)(Math.sin(a2) * outerRadius);

            float x1_ridge = (float)(Math.cos(a1) * ridgeRadius);
            float z1_ridge = (float)(Math.sin(a1) * ridgeRadius);
            float x2_ridge = (float)(Math.cos(a2) * ridgeRadius);
            float z2_ridge = (float)(Math.sin(a2) * ridgeRadius);

            float x1_tip = (float)(Math.cos(a1) * tipRadius);
            float z1_tip = (float)(Math.sin(a1) * tipRadius);
            float x2_tip = (float)(Math.cos(a2) * tipRadius);
            float z2_tip = (float)(Math.sin(a2) * tipRadius);

            Color colOuter1 = this.getHatColor((float)i / steps, customHatColor);
            Color colOuter2 = this.getHatColor((float)(i + 1) / steps, customHatColor);
            Color colMid = this.getHatColor((i + 0.5F) / steps, customHatColor);

            // 1. Defined Brim Rim Band (outer edge to ridge)
            consumer.addVertex(pose, x1_out, 0.0F, z1_out).setColor(colOuter1.getRed(), colOuter1.getGreen(), colOuter1.getBlue(), rimA);
            consumer.addVertex(pose, x2_out, 0.0F, z2_out).setColor(colOuter2.getRed(), colOuter2.getGreen(), colOuter2.getBlue(), rimA);
            consumer.addVertex(pose, x1_ridge, rimHeight, z1_ridge).setColor(colOuter1.getRed(), colOuter1.getGreen(), colOuter1.getBlue(), rimA);

            consumer.addVertex(pose, x2_out, 0.0F, z2_out).setColor(colOuter2.getRed(), colOuter2.getGreen(), colOuter2.getBlue(), rimA);
            consumer.addVertex(pose, x2_ridge, rimHeight, z2_ridge).setColor(colOuter2.getRed(), colOuter2.getGreen(), colOuter2.getBlue(), rimA);
            consumer.addVertex(pose, x1_ridge, rimHeight, z1_ridge).setColor(colOuter1.getRed(), colOuter1.getGreen(), colOuter1.getBlue(), rimA);

            // 2. Main Conical Body (ridge to tip ring)
            consumer.addVertex(pose, x1_ridge, rimHeight, z1_ridge).setColor(colOuter1.getRed(), colOuter1.getGreen(), colOuter1.getBlue(), bodyA);
            consumer.addVertex(pose, x2_ridge, rimHeight, z2_ridge).setColor(colOuter2.getRed(), colOuter2.getGreen(), colOuter2.getBlue(), bodyA);
            consumer.addVertex(pose, x1_tip, coneHeight, z1_tip).setColor(colMid.getRed(), colMid.getGreen(), colMid.getBlue(), apexA);

            consumer.addVertex(pose, x2_ridge, rimHeight, z2_ridge).setColor(colOuter2.getRed(), colOuter2.getGreen(), colOuter2.getBlue(), bodyA);
            consumer.addVertex(pose, x2_tip, coneHeight, z2_tip).setColor(colMid.getRed(), colMid.getGreen(), colMid.getBlue(), apexA);
            consumer.addVertex(pose, x1_tip, coneHeight, z1_tip).setColor(colMid.getRed(), colMid.getGreen(), colMid.getBlue(), apexA);

            // 3. Top Crown Bead (tip ring to central peak)
            consumer.addVertex(pose, 0.0F, tipHeight, 0.0F).setColor(colMid.getRed(), colMid.getGreen(), colMid.getBlue(), 255);
            consumer.addVertex(pose, x1_tip, coneHeight, z1_tip).setColor(colMid.getRed(), colMid.getGreen(), colMid.getBlue(), apexA);
            consumer.addVertex(pose, x2_tip, coneHeight, z2_tip).setColor(colMid.getRed(), colMid.getGreen(), colMid.getBlue(), apexA);

            // 4. Underside (interior lining visible from below)
            consumer.addVertex(pose, 0.0F, coneHeight * 0.9F, 0.0F).setColor(colMid.getRed(), colMid.getGreen(), colMid.getBlue(), underA);
            consumer.addVertex(pose, x2_out, 0.0F, z2_out).setColor(colOuter2.getRed(), colOuter2.getGreen(), colOuter2.getBlue(), underA);
            consumer.addVertex(pose, x1_out, 0.0F, z1_out).setColor(colOuter1.getRed(), colOuter1.getGreen(), colOuter1.getBlue(), underA);
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
