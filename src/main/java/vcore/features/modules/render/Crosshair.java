package vcore.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.TextureStorage;

public class Crosshair extends Module {
   private final Setting<Crosshair.Mode> mode = new Setting<>("Mode", Crosshair.Mode.Circle);
   private final Setting<Boolean> animated = new Setting<>("Animated", true, v -> this.mode.is(Crosshair.Mode.Default));
   private final Setting<Boolean> dot = new Setting<>("Dot", false, v -> this.mode.is(Crosshair.Mode.Default));
   private final Setting<Boolean> t = new Setting<>("T", false, v -> this.mode.is(Crosshair.Mode.Default));
   public final Setting<Boolean> syncColor = new Setting<>("SyncColor", true);
   public final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(255, 255, 255, 255).getRGB()), v -> !this.syncColor.getValue());
   private float xAnim;
   private float yAnim;
   private float prevProgress;

   public Crosshair() {
      super("Crosshair", "Custom crosshair overlay.", Module.Category.RENDER);
   }

   @Override
   public void onRender2D(GuiGraphics context) {
      if (mc.options.getCameraType().isFirstPerson()) {
         float midX = mc.getWindow().getGuiScaledWidth() / 2.0F;
         float midY = mc.getWindow().getGuiScaledHeight() / 2.0F;
         this.xAnim = midX;
         this.yAnim = midY;
         float progress = 360.0F * mc.player.getAttackStrengthScale(0.5F);
         progress = progress == 0.0F ? 360.0F : progress;
         switch ((Crosshair.Mode)this.mode.getValue()) {
            case Circle:
               Color c1;
               Color c2;
               if (this.syncColor.getValue()) {
                  HudEditor.Theme theme = HudEditor.getCurrentTheme();
                  c1 = theme.color1;
                  c2 = theme.color2;
               } else {
                  c1 = this.color.getValue().getColorObject();
                  c2 = this.color.getValue().getColorObject();
               }

               Render2DEngine.drawArc(
                  context.pose(),
                  this.xAnim - 25.0F,
                  this.yAnim - 25.0F,
                  50.0F,
                  50.0F,
                  0.025F,
                  0.12F,
                  0.0F,
                  Render2DEngine.interpolateFloat(this.prevProgress, progress, Render3DEngine.getTickDelta()),
                  c1,
                  c2
               );
               this.prevProgress = progress;
               break;
            case Dot:
               Color dotColor;
               if (this.syncColor.getValue()) {
                  HudEditor.Theme theme = HudEditor.getCurrentTheme();
                  dotColor = theme.color1;
               } else {
                  dotColor = this.color.getValue().getColorObject();
               }

               context.pose().pushPose();
               context.pose().translate(this.xAnim + 4.0F, this.yAnim + 4.0F, 0.0F);
               RenderSystem.enableBlend();
               RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
               RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
               BufferBuilder bufferBuilder = Tesselator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
               RenderSystem.setShaderTexture(0, TextureStorage.firefly);
               Matrix4f posMatrix = context.pose().last().pose();
               bufferBuilder.addVertex(posMatrix, 0.0F, -8.0F, 0.0F).setUv(0.0F, 1.0F).setColor(dotColor.getRGB());
               bufferBuilder.addVertex(posMatrix, -8.0F, -8.0F, 0.0F).setUv(1.0F, 1.0F).setColor(dotColor.getRGB());
               bufferBuilder.addVertex(posMatrix, -8.0F, 0.0F, 0.0F).setUv(1.0F, 0.0F).setColor(dotColor.getRGB());
               bufferBuilder.addVertex(posMatrix, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F).setColor(dotColor.getRGB());
               BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableBlend();
               context.pose().popPose();
               break;
            case Default:
               Color drawColor;
               if (this.syncColor.getValue()) {
                  HudEditor.Theme theme = HudEditor.getCurrentTheme();
                  drawColor = theme.color1;
               } else {
                  drawColor = this.color.getValue().getColorObject();
               }

               float offset = this.animated.getValue()
                  ? -3.0F + Render2DEngine.interpolateFloat(this.prevProgress, progress, Render3DEngine.getTickDelta()) / 100.0F
                  : 0.0F;
               this.prevProgress = progress;
               if (!this.t.getValue()) {
                  Render2DEngine.drawRect(context.pose(), this.xAnim - 1.0F, this.yAnim - 6.0F + offset, 2.0F, 4.0F, Color.BLACK);
                  Render2DEngine.drawRect(context.pose(), this.xAnim - 0.5F, this.yAnim - 5.5F + offset, 1.0F, 3.0F, drawColor);
               }

               Render2DEngine.drawRect(context.pose(), this.xAnim - 1.0F, this.yAnim + 2.0F - offset, 2.0F, 4.0F, Color.BLACK);
               Render2DEngine.drawRect(context.pose(), this.xAnim - 0.5F, this.yAnim + 2.5F - offset, 1.0F, 3.0F, drawColor);
               Render2DEngine.drawRect(context.pose(), this.xAnim - 6.0F + offset, this.yAnim - 1.0F, 4.0F, 2.0F, Color.BLACK);
               Render2DEngine.drawRect(context.pose(), this.xAnim - 5.5F + offset, this.yAnim - 0.5F, 3.0F, 1.0F, drawColor);
               Render2DEngine.drawRect(context.pose(), this.xAnim + 2.0F - offset, this.yAnim - 1.0F, 4.0F, 2.0F, Color.BLACK);
               Render2DEngine.drawRect(context.pose(), this.xAnim + 2.5F - offset, this.yAnim - 0.5F, 3.0F, 1.0F, drawColor);
               if (this.dot.getValue()) {
                  Render2DEngine.drawRect(context.pose(), this.xAnim - 1.0F, this.yAnim - 1.0F, 2.0F, 2.0F, Color.BLACK);
                  Render2DEngine.drawRect(context.pose(), this.xAnim - 0.5F, this.yAnim - 0.5F, 1.0F, 1.0F, drawColor);
               }
         }
      }
   }

   public float getAnimatedPosX() {
      return this.xAnim == 0.0F ? mc.getWindow().getGuiScaledWidth() / 2.0F : this.xAnim;
   }

   public float getAnimatedPosY() {
      return this.yAnim == 0.0F ? mc.getWindow().getGuiScaledHeight() / 2.0F : this.yAnim;
   }

   private enum Mode {
      Circle,
      Dot,
      Default;
   }
}
