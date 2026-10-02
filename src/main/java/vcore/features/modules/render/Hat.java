package vcore.features.modules.render;

import vcore.utility.render.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
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
   public static PoseStack matrixStack;

   public Hat() {
      super("Hat", "Renders a cosmetic hat above players.", Module.Category.RENDER);
   }

   public void renderHat(PoseStack stack) {
      if (mc.player != null) {
         Matrix4f modelMatrix = stack.last().pose();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         Tesselator tessellator = Tesselator.getInstance();
         BufferBuilder buffer = tessellator.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
         float radius = 0.6F;
         int steps = 72;
         double angleStep = (Math.PI * 2) / steps;
         Color customHatColor = this.color.getValue().getColorObject();
         int apexA = 255;
         int baseA = 120;

         for (int i = 0; i < steps; i++) {
            float x1 = (float)(Math.cos(i * angleStep) * radius);
            float z1 = (float)(Math.sin(i * angleStep) * radius);
            float x2 = (float)(Math.cos((i + 1) * angleStep) * radius);
            float z2 = (float)(Math.sin((i + 1) * angleStep) * radius);
            Color apexColor = this.getHatColor((i + 0.5F) / steps, customHatColor);
            Color baseColor1 = this.getHatColor((float)i / steps, customHatColor);
            Color baseColor2 = this.getHatColor((float)(i + 1) / steps, customHatColor);
            buffer.addVertex(modelMatrix, 0.0F, 0.3F, 0.0F).setColor(apexColor.getRed(), apexColor.getGreen(), apexColor.getBlue(), apexA);
            buffer.addVertex(modelMatrix, x1, 0.0F, z1).setColor(baseColor1.getRed(), baseColor1.getGreen(), baseColor1.getBlue(), baseA);
            buffer.addVertex(modelMatrix, x2, 0.0F, z2).setColor(baseColor2.getRed(), baseColor2.getGreen(), baseColor2.getBlue(), baseA);
         }

         BufferUploader.drawWithShader(buffer.buildOrThrow());
         buffer = tessellator.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

         for (int i = 0; i <= steps; i++) {
            float x = (float)(Math.cos(i * angleStep) * radius);
            float z = (float)(Math.sin(i * angleStep) * radius);
            Color rimColor = this.getHatColor((float)i / steps, customHatColor);
            buffer.addVertex(modelMatrix, x, 0.0F, z).setColor(rimColor.getRed(), rimColor.getGreen(), rimColor.getBlue(), apexA);
         }

         BufferUploader.drawWithShader(buffer.buildOrThrow());
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
         BufferUploader.invalidateCommandEncoder();
         try {
            org.lwjgl.opengl.GL33.glBindSampler(0, 0);
         } catch (Throwable ignored) {}
      }
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
