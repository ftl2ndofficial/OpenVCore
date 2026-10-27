package vcore.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Stack;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.Texture;
import vcore.utility.math.MathUtility;
import vcore.utility.render.RenderSystem.DestFactor;
import vcore.utility.render.RenderSystem.SourceFactor;
import vcore.utility.render.shaders.ArcShader;
import vcore.utility.render.shaders.BlurProgram;
import vcore.utility.render.shaders.CheckboxShader;
import vcore.utility.render.shaders.HudShader;
import vcore.utility.render.shaders.RectangleShader;
import vcore.utility.render.shaders.TextureColorProgram;

public class Render2DEngine {
   public static TextureColorProgram TEXTURE_COLOR_PROGRAM;
   public static HudShader HUD_SHADER;
   public static CheckboxShader CHECKBOX_SHADER;
   public static RectangleShader RECTANGLE_SHADER;
   public static ArcShader ARC_PROGRAM;
   public static BlurProgram BLUR_PROGRAM;
   public static HashMap<Integer, Render2DEngine.BlurredShadow> shadowCache = new HashMap<>();
   public static HashMap<Integer, Render2DEngine.BlurredShadow> shadowCache1 = new HashMap<>();
   static final Stack<Render2DEngine.Rectangle> clipStack = new Stack<>();
   private static float globalAlpha = 1.0F;
   private static boolean scissorActive = false;
   public static boolean renderingPostGui = false;
   private static final Int2LongOpenHashMap CURSOR_CACHE = new Int2LongOpenHashMap();
   private static int currentCursorType = -1;

   private static final int CORNER_STEPS_FAST = 5;
   private static final float[][] CORNER_SIN_FAST = new float[4][CORNER_STEPS_FAST + 1];
   private static final float[][] CORNER_COS_FAST = new float[4][CORNER_STEPS_FAST + 1];

   static {
      for (int i = 0; i < 4; i++) {
         for (int s = 0; s <= CORNER_STEPS_FAST; s++) {
            double deg = i * 90.0 + (90.0 * s / CORNER_STEPS_FAST);
            double rad = Math.toRadians(deg);
            CORNER_SIN_FAST[i][s] = (float)Math.sin(rad);
            CORNER_COS_FAST[i][s] = (float)Math.cos(rad);
         }
      }
   }

   public static boolean isScissorActive() {
      return scissorActive;
   }

   public static void setCursor(int standardCursorType) {
      if (GLFW.glfwGetPlatform() == 393219 || Module.mc == null || Module.mc.getWindow() == null) {
         return;
      }
      if (currentCursorType == standardCursorType) {
         return;
      }
      long cursorHandle = CURSOR_CACHE.get(standardCursorType);
      if (cursorHandle == 0L) {
         cursorHandle = GLFW.glfwCreateStandardCursor(standardCursorType);
         if (cursorHandle != 0L) {
            CURSOR_CACHE.put(standardCursorType, cursorHandle);
         }
      }
      if (cursorHandle != 0L) {
         GLFW.glfwSetCursor(Module.mc.getWindow().handle(), cursorHandle);
         currentCursorType = standardCursorType;
      }
   }

   public static void setGlobalAlpha(float alpha) {
      globalAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
   }

   private static float applyGlobalAlpha(float alpha) {
      return Mth.clamp(alpha * globalAlpha, 0.0F, 1.0F);
   }

   private static int applyGlobalAlphaToInt(int color) {
      if (globalAlpha >= 0.999F) {
         return color;
      }
      int origA = (color >>> 24) & 0xFF;
      int a = Mth.clamp(Math.round(origA * globalAlpha), 0, 255);
      return (color & 0x00FFFFFF) | (a << 24);
   }

   private static Color applyGlobalAlphaToColor(Color color) {
      if (globalAlpha >= 0.999F) {
         return color;
      }
      int a = Mth.clamp(Math.round(color.getAlpha() * globalAlpha), 0, 255);
      return a == color.getAlpha() ? color : new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
   }

   public static PoseStack toPoseStack(org.joml.Matrix3x2fStack stack) {
      PoseStack poseStack = new PoseStack();
      Matrix4f mat = poseStack.last().pose();
      mat.set(
         stack.m00, stack.m01, 0.0F, 0.0F,
         stack.m10, stack.m11, 0.0F, 0.0F,
         0.0F,      0.0F,      1.0F, 0.0F,
         stack.m20, stack.m21, 0.0F, 1.0F
      );
      return poseStack;
   }

   public static PoseStack toPoseStack(org.joml.Matrix3x2f stack) {
      PoseStack poseStack = new PoseStack();
      Matrix4f mat = poseStack.last().pose();
      mat.set(
         stack.m00, stack.m01, 0.0F, 0.0F,
         stack.m10, stack.m11, 0.0F, 0.0F,
         0.0F,      0.0F,      1.0F, 0.0F,
         stack.m20, stack.m21, 0.0F, 1.0F
      );
      return poseStack;
   }

   public static void addWindow(GuiGraphics context, Render2DEngine.Rectangle r1) {
      addWindow(toPoseStack(context.pose()), r1);
      int minX = (int)Math.min(r1.x, r1.x1);
      int maxX = (int)Math.max(r1.x, r1.x1);
      int minY = (int)Math.min(r1.y, r1.y1);
      int maxY = (int)Math.max(r1.y, r1.y1);
      context.enableScissor(minX, minY, maxX, maxY);
   }

   public static void addWindow(GuiGraphics context, float x, float y, float x1, float y1) {
      addWindow(toPoseStack(context.pose()), x, y, x1, y1, 1.0);
      int minX = (int)Math.min(x, x1);
      int maxX = (int)Math.max(x, x1);
      int minY = (int)Math.min(y, y1);
      int maxY = (int)Math.max(y, y1);
      context.enableScissor(minX, minY, maxX, maxY);
   }

   public static void popWindow(GuiGraphics context) {
      popWindow();
      context.disableScissor();
   }

   public static void addWindow(PoseStack stack, Render2DEngine.Rectangle r1) {
      Matrix4f matrix = stack.last().pose();
      Vector4f coord = new Vector4f(r1.x, r1.y, 0.0F, 1.0F);
      Vector4f end = new Vector4f(r1.x1, r1.y1, 0.0F, 1.0F);
      coord.mulTranspose(matrix);
      end.mulTranspose(matrix);
      float x = coord.x();
      float y = coord.y();
      float endX = end.x();
      float endY = end.y();
      Render2DEngine.Rectangle r = new Render2DEngine.Rectangle(x, y, endX, endY);
      if (clipStack.empty()) {
         clipStack.push(r);
         beginScissor(r.x, r.y, r.x1, r.y1);
      } else {
         Render2DEngine.Rectangle lastClip = clipStack.peek();
         float lsx = lastClip.x;
         float lsy = lastClip.y;
         float lstx = lastClip.x1;
         float lsty = lastClip.y1;
         float nsx = Mth.clamp(r.x, lsx, lstx);
         float nsy = Mth.clamp(r.y, lsy, lsty);
         float nstx = Mth.clamp(r.x1, nsx, lstx);
         float nsty = Mth.clamp(r.y1, nsy, lsty);
         clipStack.push(new Render2DEngine.Rectangle(nsx, nsy, nstx, nsty));
         beginScissor(nsx, nsy, nstx, nsty);
      }
   }

   public static void popWindow() {
      clipStack.pop();
      if (clipStack.empty()) {
         endScissor();
      } else {
         Render2DEngine.Rectangle r = clipStack.peek();
         beginScissor(r.x, r.y, r.x1, r.y1);
      }
   }

   public static void beginScissor(double x, double y, double endX, double endY) {
      double width = endX - x;
      double height = endY - y;
      width = Math.max(0.0, width);
      height = Math.max(0.0, height);
      float d = (float)Render3DEngine.getScaleFactor();
      int ay = (int)((Module.mc.getWindow().getGuiScaledHeight() - (y + height)) * d);
      scissorActive = true;
      RenderSystem.enableScissor((int)(x * d), ay, (int)(width * d), (int)(height * d));
   }

   public static void endScissor() {
      scissorActive = false;
      RenderSystem.disableScissor();
   }

   public static void addWindow(PoseStack stack, float x, float y, float x1, float y1, double animation_factor) {
      float h = y + y1;
      float h2 = (float)(h * (1.0 - MathUtility.clamp(animation_factor, 0.0, 1.0025F)));
      float x3 = x;
      float y3 = y + h2;
      float x4 = x1;
      float y4 = y1 - h2;
      if (x4 < x3) {
         x4 = x3;
      }

      if (y4 < y3) {
         y4 = y3;
      }

      addWindow(stack, new Render2DEngine.Rectangle(x3, y3, x4, y4));
   }

   public static void horizontalGradient(PoseStack matrices, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
      Matrix4f matrix = matrices.last().pose();
      setupRender();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      int start = applyGlobalAlphaToInt(startColor.getRGB());
      int end = applyGlobalAlphaToInt(endColor.getRGB());
      buffer.addVertex(matrix, x1, y1, 0.0F).setColor(start);
      buffer.addVertex(matrix, x1, y2, 0.0F).setColor(start);
      buffer.addVertex(matrix, x2, y2, 0.0F).setColor(end);
      buffer.addVertex(matrix, x2, y1, 0.0F).setColor(end);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      endRender();
   }

   public static void verticalGradient(PoseStack matrices, float left, float top, float right, float bottom, Color startColor, Color endColor) {
      Matrix4f matrix = matrices.last().pose();
      setupRender();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      int start = applyGlobalAlphaToInt(startColor.getRGB());
      int end = applyGlobalAlphaToInt(endColor.getRGB());
      buffer.addVertex(matrix, left, top, 0.0F).setColor(start);
      buffer.addVertex(matrix, left, bottom, 0.0F).setColor(end);
      buffer.addVertex(matrix, right, bottom, 0.0F).setColor(end);
      buffer.addVertex(matrix, right, top, 0.0F).setColor(start);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      endRender();
   }

   public static void drawRect(PoseStack matrices, float x, float y, float width, float height, Color c) {
      Matrix4f matrix = matrices.last().pose();
      setupRender();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      int color = applyGlobalAlphaToInt(c.getRGB());
      buffer.addVertex(matrix, x, y + height, 0.0F).setColor(color);
      buffer.addVertex(matrix, x + width, y + height, 0.0F).setColor(color);
      buffer.addVertex(matrix, x + width, y, 0.0F).setColor(color);
      buffer.addVertex(matrix, x, y, 0.0F).setColor(color);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      endRender();
   }

   public static void drawGradientRect(
      PoseStack matrices, float x, float y, float width, float height, Color bottomLeft, Color bottomRight, Color topRight, Color topLeft
   ) {
      Matrix4f matrix = matrices.last().pose();
      setupRender();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      setRectPoints(buffer, matrix, x, y, x + width, y + height, bottomLeft, bottomRight, topRight, topLeft);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      endRender();
   }

   public static void drawClickGuiRect(PoseStack matrices, float x, float y, float width, float height, Color c) {
      drawClickGuiRound(matrices, x, y, width, height, 0.0F, c);
   }

   public static void drawRectWithOutline(PoseStack matrices, float x, float y, float width, float height, Color c, Color c2) {
      Matrix4f matrix = matrices.last().pose();
      setupRender();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      int fill = applyGlobalAlphaToInt(c.getRGB());
      buffer.addVertex(matrix, x, y + height, 0.0F).setColor(fill);
      buffer.addVertex(matrix, x + width, y + height, 0.0F).setColor(fill);
      buffer.addVertex(matrix, x + width, y, 0.0F).setColor(fill);
      buffer.addVertex(matrix, x, y, 0.0F).setColor(fill);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      buffer = Tesselator.getInstance().begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      int outline = applyGlobalAlphaToInt(c2.getRGB());
      buffer.addVertex(matrix, x, y + height, 0.0F).setColor(outline);
      buffer.addVertex(matrix, x + width, y + height, 0.0F).setColor(outline);
      buffer.addVertex(matrix, x + width, y, 0.0F).setColor(outline);
      buffer.addVertex(matrix, x, y, 0.0F).setColor(outline);
      buffer.addVertex(matrix, x, y + height, 0.0F).setColor(outline);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      endRender();
   }

   public static void drawRectDumbWay(PoseStack matrices, float x, float y, float x1, float y1, Color c1) {
      Matrix4f matrix = matrices.last().pose();
      setupRender();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      int color = applyGlobalAlphaToInt(c1.getRGB());
      buffer.addVertex(matrix, x, y1, 0.0F).setColor(color);
      buffer.addVertex(matrix, x1, y1, 0.0F).setColor(color);
      buffer.addVertex(matrix, x1, y, 0.0F).setColor(color);
      buffer.addVertex(matrix, x, y, 0.0F).setColor(color);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      endRender();
   }

   public static void setRectPoints(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float x1, float y1, Color c1, Color c2, Color c3, Color c4) {
      bufferBuilder.addVertex(matrix, x, y1, 0.0F).setColor(applyGlobalAlphaToInt(c1.getRGB()));
      bufferBuilder.addVertex(matrix, x1, y1, 0.0F).setColor(applyGlobalAlphaToInt(c2.getRGB()));
      bufferBuilder.addVertex(matrix, x1, y, 0.0F).setColor(applyGlobalAlphaToInt(c3.getRGB()));
      bufferBuilder.addVertex(matrix, x, y, 0.0F).setColor(applyGlobalAlphaToInt(c4.getRGB()));
   }

   public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y;
   }

   public static void drawBlurredShadow(PoseStack matrices, float x, float y, float width, float height, int blurRadius, Color color) {
   }

   public static void drawGradientBlurredShadow(
      PoseStack matrices, float x, float y, float width, float height, int blurRadius, Color color1, Color color2, Color color3, Color color4
   ) {
   }

   public static void drawGradientBlurredShadow1(
      PoseStack matrices, float x, float y, float width, float height, int blurRadius, Color color1, Color color2, Color color3, Color color4
   ) {
   }

   public static void registerBufferedImageTexture(Texture i, BufferedImage bi) {
      try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ImageIO.write(bi, "png", baos);
         byte[] bytes = baos.toByteArray();
         registerTexture(i, bytes);
      } catch (Exception var4) {
      }
   }

   private static void registerTexture(Texture i, byte[] content) {
      try {
         ByteBuffer data = BufferUtils.createByteBuffer(content.length).put(content);
         data.flip();
         DynamicTexture tex = new DynamicTexture(() -> "vcore_2d_tex", NativeImage.read(data));
         if (Module.mc.isSameThread()) {
            Module.mc.getTextureManager().register(i.getId(), tex);
         } else {
            Module.mc.execute(() -> Module.mc.getTextureManager().register(i.getId(), tex));
         }
      } catch (Exception var4) {
      }
   }

   public static void drawTexture(PoseStack matrices, Identifier texture, float x, float y, float width, float height) {
      drawTexture(matrices, texture, x, y, width, height, Color.WHITE);
   }

   public static void drawTexture(PoseStack matrices, Identifier texture, float x, float y, float width, float height, Color color) {
      if (texture == null) return;
      RenderSystem.setShaderTexture(0, texture);
      setupRender();
      renderGradientTexture(matrices, x, y, width, height, 0.0F, 0.0F, width, height, width, height, color, color, color, color);
      endRender();
   }

   public static void renderTexture(
      PoseStack matrices,
      double x0,
      double y0,
      double width,
      double height,
      float u,
      float v,
      double regionWidth,
      double regionHeight,
      double textureWidth,
      double textureHeight
   ) {
      double x1 = x0 + width;
      double y1 = y0 + height;
      double z = 0.0;
      Matrix4f matrix = matrices.last().pose();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      buffer.addVertex(matrix, (float)x0, (float)y1, (float)z).setUv(u / (float)textureWidth, (v + (float)regionHeight) / (float)textureHeight);
      buffer.addVertex(matrix, (float)x1, (float)y1, (float)z)
         .setUv((u + (float)regionWidth) / (float)textureWidth, (v + (float)regionHeight) / (float)textureHeight);
      buffer.addVertex(matrix, (float)x1, (float)y0, (float)z).setUv((u + (float)regionWidth) / (float)textureWidth, v / (float)textureHeight);
      buffer.addVertex(matrix, (float)x0, (float)y0, (float)z).setUv(u / (float)textureWidth, (v + 0.0F) / (float)textureHeight);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
   }

   public static void renderGradientTexture(
      PoseStack matrices,
      double x0,
      double y0,
      double width,
      double height,
      float u,
      float v,
      double regionWidth,
      double regionHeight,
      double textureWidth,
      double textureHeight,
      Color c1,
      Color c2,
      Color c3,
      Color c4
   ) {
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      renderGradientTextureInternal(buffer, matrices, x0, y0, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight, c1, c2, c3, c4);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
   }

   public static void renderGradientTextureInternal(
      BufferBuilder buff,
      PoseStack matrices,
      double x0,
      double y0,
      double width,
      double height,
      float u,
      float v,
      double regionWidth,
      double regionHeight,
      double textureWidth,
      double textureHeight,
      Color c1,
      Color c2,
      Color c3,
      Color c4
   ) {
      double x1 = x0 + width;
      double y1 = y0 + height;
      double z = 0.0;
      Matrix4f matrix = matrices.last().pose();
      buff.addVertex(matrix, (float)x0, (float)y1, (float)z)
         .setUv(u / (float)textureWidth, (v + (float)regionHeight) / (float)textureHeight)
         .setColor(applyGlobalAlphaToInt(c1.getRGB()));
      buff.addVertex(matrix, (float)x1, (float)y1, (float)z)
         .setUv((u + (float)regionWidth) / (float)textureWidth, (v + (float)regionHeight) / (float)textureHeight)
         .setColor(applyGlobalAlphaToInt(c2.getRGB()));
      buff.addVertex(matrix, (float)x1, (float)y0, (float)z)
         .setUv((u + (float)regionWidth) / (float)textureWidth, v / (float)textureHeight)
         .setColor(applyGlobalAlphaToInt(c3.getRGB()));
      buff.addVertex(matrix, (float)x0, (float)y0, (float)z)
         .setUv(u / (float)textureWidth, (v + 0.0F) / (float)textureHeight)
         .setColor(applyGlobalAlphaToInt(c4.getRGB()));
   }

   public static void renderRoundedGradientRect(
      PoseStack matrices, Color color1, Color color2, Color color3, Color color4, float x, float y, float width, float height, float Radius
   ) {
      Matrix4f matrix = matrices.last().pose();
      RenderSystem.colorMask(false, false, false, true);
      RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
      RenderSystem.clear(16384, false);
      RenderSystem.colorMask(true, true, true, true);
      drawRound(matrices, x, y, width, height, Radius, color1);
      setupRender();
      RenderSystem.blendFunc(772, 773);
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertex(matrix, x, y + height, 0.0F).setColor(applyGlobalAlphaToInt(color1.getRGB()));
      bufferBuilder.addVertex(matrix, x + width, y + height, 0.0F).setColor(applyGlobalAlphaToInt(color2.getRGB()));
      bufferBuilder.addVertex(matrix, x + width, y, 0.0F).setColor(applyGlobalAlphaToInt(color3.getRGB()));
      bufferBuilder.addVertex(matrix, x, y, 0.0F).setColor(applyGlobalAlphaToInt(color4.getRGB()));
      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
      endRender();
   }

   public static void drawRound(PoseStack matrices, float x, float y, float width, float height, float radius, Color color) {
      renderRoundedQuad(matrices, color, x, y, width + x, height + y, radius, 4.0);
   }

   public static void renderRoundedTexture(
      PoseStack matrices,
      Identifier texture,
      float x,
      float y,
      float width,
      float height,
      float radius,
      float u0,
      float v0,
      float u1,
      float v1,
      float alpha
   ) {
      if (texture == null) return;
      RenderSystem.setShaderTexture(0, texture);
      org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_NEAREST);
      org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_NEAREST);
      try {
         org.lwjgl.opengl.GL33.glBindSampler(0, 0);
      } catch (Throwable ignored) {}
      setupRender();
      Matrix4f matrix = matrices.last().pose();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX_COLOR);

      float finalAlpha = applyGlobalAlpha(alpha);
      int a = (int)(MathUtility.clamp(finalAlpha, 0.0F, 1.0F) * 255.0F);
      float centerX = x + width * 0.5F;
      float centerY = y + height * 0.5F;
      float centerU = (u0 + u1) * 0.5F;
      float centerV = (v0 + v1) * 0.5F;

      bufferBuilder.addVertex(matrix, centerX, centerY, 0.0F).setUv(centerU, centerV).setColor(255, 255, 255, a);

      float maxRad = Math.min(width * 0.5F, height * 0.5F);
      float rad = Math.min(radius, Math.max(0.0F, maxRad));

      float toX = x + width;
      float toY = y + height;
      float fromX = x;
      float fromY = y;

      float[][] map = new float[][]{
         {toX - rad, toY - rad},
         {toX - rad, fromY + rad},
         {fromX + rad, fromY + rad},
         {fromX + rad, toY - rad}
      };

      float firstX = 0.0F;
      float firstY = 0.0F;
      float firstU = 0.0F;
      float firstV = 0.0F;
      boolean firstRecorded = false;

      for (int i = 0; i < 4; i++) {
         float[] current = map[i];
         for (int s = 0; s <= CORNER_STEPS_FAST; s++) {
            float vx = current[0] + CORNER_SIN_FAST[i][s] * rad;
            float vy = current[1] + CORNER_COS_FAST[i][s] * rad;
            float u = u0 + ((vx - fromX) / width) * (u1 - u0);
            float v = v0 + ((vy - fromY) / height) * (v1 - v0);

            if (!firstRecorded) {
               firstX = vx;
               firstY = vy;
               firstU = u;
               firstV = v;
               firstRecorded = true;
            }

            bufferBuilder.addVertex(matrix, vx, vy, 0.0F).setUv(u, v).setColor(255, 255, 255, a);
         }
      }

      if (firstRecorded) {
         bufferBuilder.addVertex(matrix, firstX, firstY, 0.0F).setUv(firstU, firstV).setColor(255, 255, 255, a);
      }

      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
      endRender();
   }

   public static void drawRoundOutline(
      PoseStack matrices, float x, float y, float width, float height, float radius, Color color
   ) {
      if (color.getAlpha() <= 0) return;
      setupRender();
      Color c = applyGlobalAlphaToColor(color);
      Matrix4f matrix = matrices.last().pose();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      float maxRad = Math.min(width * 0.5F, height * 0.5F);
      float rad = Math.min(radius, Math.max(0.0F, maxRad));

      float toX = x + width;
      float toY = y + height;
      float fromX = x;
      float fromY = y;

      float[][] map = new float[][]{
         {toX - rad, toY - rad},
         {toX - rad, fromY + rad},
         {fromX + rad, fromY + rad},
         {fromX + rad, toY - rad}
      };

      float firstX = 0.0F;
      float firstY = 0.0F;
      boolean firstRecorded = false;
      int cr = c.getRed();
      int cg = c.getGreen();
      int cb = c.getBlue();
      int ca = c.getAlpha();

      for (int i = 0; i < 4; i++) {
         float[] current = map[i];
         for (int s = 0; s <= CORNER_STEPS_FAST; s++) {
            float vx = current[0] + CORNER_SIN_FAST[i][s] * rad;
            float vy = current[1] + CORNER_COS_FAST[i][s] * rad;

            if (!firstRecorded) {
               firstX = vx;
               firstY = vy;
               firstRecorded = true;
            }

            bufferBuilder.addVertex(matrix, vx, vy, 0.0F).setColor(cr, cg, cb, ca);
         }
      }

      if (firstRecorded) {
         bufferBuilder.addVertex(matrix, firstX, firstY, 0.0F).setColor(cr, cg, cb, ca);
      }

      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
      endRender();
   }

   public static void drawClickGuiRound(PoseStack matrices, float x, float y, float width, float height, float radius, Color color) {
      if (color.getAlpha() > 0) {
         drawRound(matrices, x, y, width, height, radius, color);
      }
   }

   public static void renderRoundedQuad(PoseStack matrices, Color c, double fromX, double fromY, double toX, double toY, double radius, double samples) {
      setupRender();
      renderRoundedQuadInternal(
         matrices.last().pose(),
         c.getRed() / 255.0F,
         c.getGreen() / 255.0F,
         c.getBlue() / 255.0F,
         c.getAlpha() / 255.0F,
         fromX,
         fromY,
         toX,
         toY,
         radius,
         samples
      );
      endRender();
   }

   public static void renderRoundedQuad2(
      PoseStack matrices, Color c, Color c2, Color c3, Color c4, double fromX, double fromY, double toX, double toY, double radius
   ) {
      setupRender();
      renderRoundedQuadInternal2(
         matrices.last().pose(),
         c.getRed() / 255.0F,
         c.getGreen() / 255.0F,
         c.getBlue() / 255.0F,
         c.getAlpha() / 255.0F,
         c2.getRed() / 255.0F,
         c2.getGreen() / 255.0F,
         c2.getBlue() / 255.0F,
         c2.getAlpha() / 255.0F,
         c3.getRed() / 255.0F,
         c3.getGreen() / 255.0F,
         c3.getBlue() / 255.0F,
         c3.getAlpha() / 255.0F,
         c4.getRed() / 255.0F,
         c4.getGreen() / 255.0F,
         c4.getBlue() / 255.0F,
         c4.getAlpha() / 255.0F,
         fromX,
         fromY,
         toX,
         toY,
         radius
      );
      endRender();
   }

   public static void renderRoundedQuadInternal(
      Matrix4f matrix, float cr, float cg, float cb, float ca, double fromX, double fromY, double toX, double toY, double radius, double samples
   ) {
      float alpha = ca * applyGlobalAlpha(1.0F);
      if (alpha <= 0.001F) return;

      double maxRad = Math.min((toX - fromX) * 0.5, (toY - fromY) * 0.5);
      float rad = (float)Math.min(radius, Math.max(0.0, maxRad));

      if (rad <= 0.05F) {
         BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         float x0 = (float)fromX;
         float y0 = (float)fromY;
         float x1 = (float)toX;
         float y1 = (float)toY;
         buffer.addVertex(matrix, x0, y1, 0.0F).setColor(cr, cg, cb, alpha);
         buffer.addVertex(matrix, x1, y1, 0.0F).setColor(cr, cg, cb, alpha);
         buffer.addVertex(matrix, x1, y0, 0.0F).setColor(cr, cg, cb, alpha);
         buffer.addVertex(matrix, x0, y0, 0.0F).setColor(cr, cg, cb, alpha);
         BufferUploader.drawWithShader(buffer.buildOrThrow());
         return;
      }

      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      float centerX = (float)((fromX + toX) * 0.5);
      float centerY = (float)((fromY + toY) * 0.5);
      bufferBuilder.addVertex(matrix, centerX, centerY, 0.0F).setColor(cr, cg, cb, alpha);

      float fx = (float)fromX;
      float fy = (float)fromY;
      float tx = (float)toX;
      float ty = (float)toY;

      float[] cx = new float[]{tx - rad, tx - rad, fx + rad, fx + rad};
      float[] cy = new float[]{ty - rad, fy + rad, fy + rad, ty - rad};

      float firstX = cx[0] + CORNER_SIN_FAST[0][0] * rad;
      float firstY = cy[0] + CORNER_COS_FAST[0][0] * rad;

      for (int i = 0; i < 4; i++) {
         float curX = cx[i];
         float curY = cy[i];
         float[] sinRow = CORNER_SIN_FAST[i];
         float[] cosRow = CORNER_COS_FAST[i];
         for (int s = 0; s <= CORNER_STEPS_FAST; s++) {
            float vx = curX + sinRow[s] * rad;
            float vy = curY + cosRow[s] * rad;
            bufferBuilder.addVertex(matrix, vx, vy, 0.0F).setColor(cr, cg, cb, alpha);
         }
      }

      bufferBuilder.addVertex(matrix, firstX, firstY, 0.0F).setColor(cr, cg, cb, alpha);
      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
   }

   public static void renderRoundedQuadInternal2(
      Matrix4f matrix,
      float cr,
      float cg,
      float cb,
      float ca,
      float cr1,
      float cg1,
      float cb1,
      float ca1,
      float cr2,
      float cg2,
      float cb2,
      float ca2,
      float cr3,
      float cg3,
      float cb3,
      float ca3,
      double fromX,
      double fromY,
      double toX,
      double toY,
      double radC1
   ) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      float alpha1 = ca1 * applyGlobalAlpha(1.0F);
      float alpha = ca * applyGlobalAlpha(1.0F);
      float alpha2 = ca2 * applyGlobalAlpha(1.0F);
      float alpha3 = ca3 * applyGlobalAlpha(1.0F);

      float centerX = (float)((fromX + toX) * 0.5);
      float centerY = (float)((fromY + toY) * 0.5);
      float centerR = (cr + cr1 + cr2 + cr3) * 0.25F;
      float centerG = (cg + cg1 + cg2 + cg3) * 0.25F;
      float centerB = (cb + cb1 + cb2 + cb3) * 0.25F;
      float centerA = (alpha + alpha1 + alpha2 + alpha3) * 0.25F;

      bufferBuilder.addVertex(matrix, centerX, centerY, 0.0F).setColor(centerR, centerG, centerB, centerA);

      double maxRad = Math.min((toX - fromX) * 0.5, (toY - fromY) * 0.5);
      float rad = (float)Math.min(radC1, Math.max(0.0, maxRad));

      float fx = (float)fromX;
      float fy = (float)fromY;
      float tx = (float)toX;
      float ty = (float)toY;

      float[] cx = new float[]{tx - rad, tx - rad, fx + rad, fx + rad};
      float[] cy = new float[]{ty - rad, fy + rad, fy + rad, ty - rad};

      float firstX = cx[0] + CORNER_SIN_FAST[0][0] * rad;
      float firstY = cy[0] + CORNER_COS_FAST[0][0] * rad;

      for (int i = 0; i < 4; i++) {
         float curX = cx[i];
         float curY = cy[i];
         float curR, curG, curB, curA;
         switch (i) {
            case 0 -> {
               curR = cr1; curG = cg1; curB = cb1; curA = alpha1;
            }
            case 1 -> {
               curR = cr; curG = cg; curB = cb; curA = alpha;
            }
            case 2 -> {
               curR = cr2; curG = cg2; curB = cb2; curA = alpha2;
            }
            default -> {
               curR = cr3; curG = cg3; curB = cb3; curA = alpha3;
            }
         }

         float[] sinRow = CORNER_SIN_FAST[i];
         float[] cosRow = CORNER_COS_FAST[i];
         for (int s = 0; s <= CORNER_STEPS_FAST; s++) {
            float vx = curX + sinRow[s] * rad;
            float vy = curY + cosRow[s] * rad;
            bufferBuilder.addVertex(matrix, vx, vy, 0.0F).setColor(curR, curG, curB, curA);
         }
      }

      bufferBuilder.addVertex(matrix, firstX, firstY, 0.0F).setColor(cr1, cg1, cb1, alpha1);
      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
   }

   public static void draw2DGradientRect(
      PoseStack matrices,
      float left,
      float top,
      float right,
      float bottom,
      Color leftBottomColor,
      Color leftTopColor,
      Color rightBottomColor,
      Color rightTopColor
   ) {
      Matrix4f matrix = matrices.last().pose();
      setupRender();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertex(matrix, right, top, 0.0F).setColor(applyGlobalAlphaToInt(rightTopColor.getRGB()));
      bufferBuilder.addVertex(matrix, left, top, 0.0F).setColor(applyGlobalAlphaToInt(leftTopColor.getRGB()));
      bufferBuilder.addVertex(matrix, left, bottom, 0.0F).setColor(applyGlobalAlphaToInt(leftBottomColor.getRGB()));
      bufferBuilder.addVertex(matrix, right, bottom, 0.0F).setColor(applyGlobalAlphaToInt(rightBottomColor.getRGB()));
      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
      endRender();
   }

   public static void setupRender() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, applyGlobalAlpha(1.0F));
   }

   public static void drawTracerPointer(
      PoseStack matrices, float x, float y, float size, float tracerWidth, float downHeight, boolean down, boolean glow, int color
   ) {
      drawNewArrow(matrices, x, y, size + 8.0F, new Color(color));
   }

   public static void drawNewArrow(PoseStack matrices, float x, float y, float size, Color color) {
      RenderSystem.setShaderTexture(0, TextureStorage.arrow);
      setupRender();
      Color arrowColor = applyGlobalAlphaToColor(color);
      RenderSystem.setShaderColor(arrowColor.getRed() / 255.0F, arrowColor.getGreen() / 255.0F, arrowColor.getBlue() / 255.0F, arrowColor.getAlpha() / 255.0F);
      RenderSystem.disableDepthTest();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      Matrix4f matrix = matrices.last().pose();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      bufferBuilder.addVertex(matrix, x - size / 2.0F, y + size, 0.0F).setUv(0.0F, 1.0F);
      bufferBuilder.addVertex(matrix, x + size / 2.0F, y + size, 0.0F).setUv(1.0F, 1.0F);
      bufferBuilder.addVertex(matrix, x + size / 2.0F, y, 0.0F).setUv(1.0F, 0.0F);
      bufferBuilder.addVertex(matrix, x - size / 2.0F, y, 0.0F).setUv(0.0F, 0.0F);
      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      endRender();
   }

   public static void drawDefaultArrow(
      PoseStack matrices, float x, float y, float size, float tracerWidth, float downHeight, boolean down, boolean glow, int color
   ) {
      if (glow) {
         drawBlurredShadow(matrices, x - size * tracerWidth, y, x + size * tracerWidth - (x - size * tracerWidth), size, 10, injectAlpha(new Color(color), 140));
      }

      matrices.pushPose();
      setupRender();
      Matrix4f matrix = matrices.last().pose();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      int baseColor = applyGlobalAlphaToInt(color);
      bufferBuilder.addVertex(matrix, x, y, 0.0F).setColor(baseColor);
      bufferBuilder.addVertex(matrix, x - size * tracerWidth, y + size, 0.0F).setColor(baseColor);
      bufferBuilder.addVertex(matrix, x, y + size - downHeight, 0.0F).setColor(baseColor);
      bufferBuilder.addVertex(matrix, x, y, 0.0F).setColor(baseColor);
      color = darker(new Color(color), 0.8F).getRGB();
      int darkerColor = applyGlobalAlphaToInt(color);
      bufferBuilder.addVertex(matrix, x, y, 0.0F).setColor(darkerColor);
      bufferBuilder.addVertex(matrix, x, y + size - downHeight, 0.0F).setColor(darkerColor);
      bufferBuilder.addVertex(matrix, x + size * tracerWidth, y + size, 0.0F).setColor(darkerColor);
      bufferBuilder.addVertex(matrix, x, y, 0.0F).setColor(darkerColor);
      if (down) {
         color = darker(new Color(color), 0.6F).getRGB();
         int downColor = applyGlobalAlphaToInt(color);
         bufferBuilder.addVertex(matrix, x - size * tracerWidth, y + size, 0.0F).setColor(downColor);
         bufferBuilder.addVertex(matrix, x + size * tracerWidth, y + size, 0.0F).setColor(downColor);
         bufferBuilder.addVertex(matrix, x, y + size - downHeight, 0.0F).setColor(downColor);
         bufferBuilder.addVertex(matrix, x - size * tracerWidth, y + size, 0.0F).setColor(downColor);
      }

      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
      endRender();
      matrices.popPose();
   }

   public static void endRender() {
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, applyGlobalAlpha(1.0F));
      com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(0);
      BufferUploader.activeCustomProgram = 0;
      BufferUploader.invalidateCommandEncoder();
      try {
         org.lwjgl.opengl.GL33.glBindSampler(0, 0);
      } catch (Throwable ignored) {}
   }

   public static void drawGradientRound(
      PoseStack ms, float v, float v1, float i, float i1, float v2, Color darker, Color darker1, Color darker2, Color darker3
   ) {
      renderRoundedQuad2(ms, darker, darker1, darker2, darker3, v, v1, v + i, v1 + i1, v2);
   }

   public static float scrollAnimate(float endPoint, float current, float speed) {
      boolean shouldContinueAnimation = endPoint > current;
      if (speed < 0.0F) {
         speed = 0.0F;
      } else if (speed > 1.0F) {
         speed = 1.0F;
      }

      float dif = Math.max(endPoint, current) - Math.min(endPoint, current);
      float factor = dif * speed;
      return current + (shouldContinueAnimation ? factor : -factor);
   }

   public static Color injectAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Mth.clamp(alpha, 0, 255));
   }

   public static Color TwoColoreffect(Color cl1, Color cl2, double speed, double count) {
      int angle = (int)((System.currentTimeMillis() / speed + count) % 360.0);
      angle = (angle >= 180 ? 360 - angle : angle) * 2;
      return interpolateColorC(cl1, cl2, angle / 360.0F);
   }

   public static Color rainbow(int delay, float saturation, float brightness) {
      double rainbow = Math.ceil((float)(System.currentTimeMillis() + delay) / 16.0F);
      rainbow %= 360.0;
      return Color.getHSBColor((float)(rainbow / 360.0), saturation, brightness);
   }

   public static Color fade(int speed, int index, Color color, float alpha) {
      float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      angle = (angle > 180 ? 360 - angle : angle) + 180;
      Color colorHSB = new Color(Color.HSBtoRGB(hsb[0], hsb[1], angle / 360.0F));
      return new Color(colorHSB.getRed(), colorHSB.getGreen(), colorHSB.getBlue(), Math.max(0, Math.min(255, (int)(alpha * 255.0F))));
   }

   public static Color applyOpacity(Color color, float opacity) {
      opacity = applyGlobalAlpha(Math.min(1.0F, Math.max(0.0F, opacity)));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(color.getAlpha() * opacity));
   }

   public static int applyOpacity(int color_int, float opacity) {
      opacity = applyGlobalAlpha(Math.min(1.0F, Math.max(0.0F, opacity)));
      int a = (int)(((color_int >>> 24) & 0xFF) * opacity);
      return (color_int & 0x00FFFFFF) | (Mth.clamp(a, 0, 255) << 24);
   }

   public static Color darker(Color color, float factor) {
      return new Color(
         Math.max((int)(color.getRed() * factor), 0),
         Math.max((int)(color.getGreen() * factor), 0),
         Math.max((int)(color.getBlue() * factor), 0),
         color.getAlpha()
      );
   }

   public static Color rainbow(int speed, int index, float saturation, float brightness, float opacity) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      float hue = angle / 360.0F;
      Color color = new Color(Color.HSBtoRGB(hue, saturation, brightness));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, (int)(opacity * 255.0F))));
   }

   public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      angle = (angle >= 180 ? 360 - angle : angle) * 2;
      return trueColor ? interpolateColorHue(start, end, angle / 360.0F) : interpolateColorC(start, end, angle / 360.0F);
   }

   public static Color interpolateColorC(Color color1, Color color2, float amount) {
      amount = Math.min(1.0F, Math.max(0.0F, amount));
      return new Color(
         interpolateInt(color1.getRed(), color2.getRed(), amount),
         interpolateInt(color1.getGreen(), color2.getGreen(), amount),
         interpolateInt(color1.getBlue(), color2.getBlue(), amount),
         interpolateInt(color1.getAlpha(), color2.getAlpha(), amount)
      );
   }

   public static Color interpolateColorHue(Color color1, Color color2, float amount) {
      amount = Math.min(1.0F, Math.max(0.0F, amount));
      float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
      float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);
      Color resultColor = Color.getHSBColor(
         interpolateFloat(color1HSB[0], color2HSB[0], amount),
         interpolateFloat(color1HSB[1], color2HSB[1], amount),
         interpolateFloat(color1HSB[2], color2HSB[2], amount)
      );
      return new Color(resultColor.getRed(), resultColor.getGreen(), resultColor.getBlue(), interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
   }

   public static double interpolate(double oldValue, double newValue, double interpolationValue) {
      return oldValue + (newValue - oldValue) * interpolationValue;
   }

   public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
      return (float)interpolate(oldValue, newValue, (float)interpolationValue);
   }

   public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
      return (int)interpolate(oldValue, newValue, (float)interpolationValue);
   }

   public static void drawArc(
      PoseStack matrices, float x, float y, float width, float height, float radius, float thickness, float start, float end, Color c1, Color c2
   ) {
      BufferBuilder bb = preShaderDraw(matrices, x - width / 2.0F, y - height / 2.0F, x + width / 2.0F, y + height / 2.0F);
      ARC_PROGRAM.setParameters(x, y, width, height, radius, thickness, start, end, c1, c2);
      ARC_PROGRAM.use();
      BufferUploader.drawWithShader(bb.buildOrThrow());
      endRender();
   }

   public static void drawRect(PoseStack matrices, float x, float y, float width, float height, float radius, float alpha) {
      BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
      RECTANGLE_SHADER.setParameters(x, y, width, height, radius, applyGlobalAlpha(alpha));
      RECTANGLE_SHADER.use();
      BufferUploader.drawWithShader(bb.buildOrThrow());
      endRender();
   }

   public static void drawRect(
      PoseStack matrices, float x, float y, float width, float height, float radius, float alpha, Color c1, Color c2, Color c3, Color c4
   ) {
      BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
      RECTANGLE_SHADER.setParameters(
         x,
         y,
         width,
         height,
         radius,
         applyGlobalAlpha(alpha),
         applyGlobalAlphaToColor(c1),
         applyGlobalAlphaToColor(c2),
         applyGlobalAlphaToColor(c3),
         applyGlobalAlphaToColor(c4)
      );
      RECTANGLE_SHADER.use();
      BufferUploader.drawWithShader(bb.buildOrThrow());
      endRender();
   }

   public static void drawCheckbox(
      PoseStack matrices, float x, float y, float width, float height, float radius, float thickness, float progress, Color color
   ) {
      Matrix4f matrix = matrices.last().pose();
      Vector4f topLeft = new Vector4f(x, y, 0.0F, 1.0F);
      Vector4f bottomRight = new Vector4f(x + width, y + height, 0.0F, 1.0F);
      topLeft.mulTranspose(matrix);
      bottomRight.mulTranspose(matrix);
      float transformedX = Math.min(topLeft.x(), bottomRight.x());
      float transformedY = Math.min(topLeft.y(), bottomRight.y());
      float transformedWidth = Math.abs(bottomRight.x() - topLeft.x());
      float transformedHeight = Math.abs(bottomRight.y() - topLeft.y());
      if (!(transformedWidth <= 0.01F) && !(transformedHeight <= 0.01F)) {
         float scale = Math.min(transformedWidth / width, transformedHeight / height);
         BufferBuilder bb = preShaderDraw(matrices, x - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F);
         CHECKBOX_SHADER.setParameters(
            transformedX,
            transformedY,
            transformedWidth,
            transformedHeight,
            radius * scale,
            thickness * scale,
            Mth.clamp(progress, 0.0F, 1.0F),
            applyGlobalAlphaToColor(color)
         );
         CHECKBOX_SHADER.use();
         BufferUploader.drawWithShader(bb.buildOrThrow());
         endRender();
      }
   }

   public static void drawHudBase(PoseStack matrices, float x, float y, float width, float height, float radius) {
      if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
         drawRoundedBlur(matrices, x, y, width, height, radius, applyGlobalAlphaToColor(HudEditor.getBlurColor()));
      } else {
         BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
         float alpha = applyGlobalAlpha(HudEditor.getAlpha());
         HUD_SHADER.setParameters(x, y, width, height, radius, alpha, alpha);
         HUD_SHADER.use();
         BufferUploader.drawWithShader(bb.buildOrThrow());
         endRender();
      }
   }

   public static void drawHudBase2(
      PoseStack matrices, float x, float y, float width, float height, float radius, float blurStrenth, float blurOpacity, float animationFactor
   ) {
      if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
         blurStrenth *= animationFactor;
         blurOpacity *= animationFactor;
         Color c = interpolateColorC(Color.BLACK, HudEditor.getBlurColor(), animationFactor);
         drawRoundedBlur(matrices, x, y, width, height, radius, applyGlobalAlphaToColor(c), blurStrenth, applyGlobalAlpha(blurOpacity));
      } else {
         BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
         float alpha = applyGlobalAlpha(HudEditor.getAlpha() * animationFactor);
         HUD_SHADER.setParameters(x, y, width, height, radius, alpha, alpha);
         HUD_SHADER.use();
         BufferUploader.drawWithShader(bb.buildOrThrow());
         endRender();
      }
   }

   public static void drawHudBase(PoseStack matrices, float x, float y, float width, float height, float radius, boolean hud) {
      BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
      float alpha = applyGlobalAlpha(HudEditor.getAlpha());
      HUD_SHADER.setParameters(x, y, width, height, radius, alpha, alpha);
      HUD_SHADER.use();
      BufferUploader.drawWithShader(bb.buildOrThrow());
      endRender();
   }

   public static void drawRoundedBlur(PoseStack matrices, float x, float y, float width, float height, float radius, Color c1) {
      drawRoundedBlur(matrices, x, y, width, height, radius, applyGlobalAlphaToColor(c1), 5.0F, applyGlobalAlpha(HudEditor.getBlurOpacity()));
   }

   public static void drawRoundedBlur(
      PoseStack matrices, float x, float y, float width, float height, float radius, Color c1, float blurStrenth, float blurOpacity
   ) {
      BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
      BLUR_PROGRAM.setParameters(x, y, width, height, radius, applyGlobalAlphaToColor(c1), blurStrenth, applyGlobalAlpha(blurOpacity));
      BLUR_PROGRAM.use();
      BufferUploader.drawWithShader(bb.buildOrThrow());
      endRender();
   }

   public static void drawHudBase(PoseStack matrices, float x, float y, float width, float height, float radius, float alpha) {
      BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
      float finalAlpha = applyGlobalAlpha(alpha);
      HUD_SHADER.setParameters(x, y, width, height, radius, finalAlpha, finalAlpha);
      HUD_SHADER.use();
      BufferUploader.drawWithShader(bb.buildOrThrow());
      endRender();
   }

   public static void drawGuiBase(PoseStack matrices, float x, float y, float width, float height, float radius, float opacity) {
      if (shouldBlurClickGuiComponents()) {
         drawRoundedBlur(matrices, x, y, width, height, radius, HudEditor.getBlurColor(), 5.0F * globalAlpha, opacity);
      } else {
         BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
         float finalOpacity = applyGlobalAlpha(opacity);
         float finalAlpha = applyGlobalAlpha(1.0F);
         HUD_SHADER.setParameters(x, y, width, height, radius, finalAlpha, finalOpacity);
         HUD_SHADER.use();
         BufferUploader.drawWithShader(bb.buildOrThrow());
         endRender();
      }
   }

   private static boolean shouldBlurClickGuiComponents() {
      return HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry);
   }

   public static BufferBuilder preShaderDraw(PoseStack matrices, float x, float y, float width, float height) {
      setupRender();
      Matrix4f matrix = matrices.last().pose();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      setRectanglePoints(buffer, matrix, x, y, x + width, y + height);
      return buffer;
   }

   public static void setRectanglePoints(BufferBuilder buffer, Matrix4f matrix, float x, float y, float x1, float y1) {
      buffer.addVertex(matrix, x, y, 0.0F);
      buffer.addVertex(matrix, x, y1, 0.0F);
      buffer.addVertex(matrix, x1, y1, 0.0F);
      buffer.addVertex(matrix, x1, y, 0.0F);
   }

   public static void drawLine(float x, float y, float x1, float y1, int color) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      int lineColor = applyGlobalAlphaToInt(color);
      bufferBuilder.addVertex(x, y, 0.0F).setColor(lineColor);
      bufferBuilder.addVertex(x1, y1, 0.0F).setColor(lineColor);
      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
   }

   public static boolean isDark(Color color) {
      return isDark(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
   }

   public static boolean isDark(float r, float g, float b) {
      return colorDistance(r, g, b, 0.0F, 0.0F, 0.0F) < colorDistance(r, g, b, 1.0F, 1.0F, 1.0F);
   }

   public static float colorDistance(float r1, float g1, float b1, float r2, float g2, float b2) {
      float a = r2 - r1;
      float b = g2 - g1;
      float c = b2 - b1;
      return (float)Math.sqrt(a * a + b * b + c * c);
   }

   public static void initShaders() {
      HUD_SHADER = new HudShader();
      TEXTURE_COLOR_PROGRAM = new TextureColorProgram();
      ARC_PROGRAM = new ArcShader();
      CHECKBOX_SHADER = new CheckboxShader();
      RECTANGLE_SHADER = new RectangleShader();
      BLUR_PROGRAM = new BlurProgram();
   }

   @NotNull
   public static Color getColor(@NotNull Color start, @NotNull Color end, float progress, boolean smooth) {
      if (!smooth) {
         return progress >= 0.95 ? end : start;
      }

      int rDiff = end.getRed() - start.getRed();
      int gDiff = end.getGreen() - start.getGreen();
      int bDiff = end.getBlue() - start.getBlue();
      int aDiff = end.getAlpha() - start.getAlpha();
      return new Color(
         fixColorValue(start.getRed() + (int)(rDiff * progress)),
         fixColorValue(start.getGreen() + (int)(gDiff * progress)),
         fixColorValue(start.getBlue() + (int)(bDiff * progress)),
         fixColorValue(start.getAlpha() + (int)(aDiff * progress))
      );
   }

   private static int fixColorValue(int colorVal) {
      return colorVal > 255 ? 255 : Math.max(colorVal, 0);
   }

   public static void endBuilding(BufferBuilder bb) {
      MeshData builtBuffer = bb.build();
      if (builtBuffer != null) {
         BufferUploader.drawWithShader(builtBuffer);
      }
   }

   public static class BlurredShadow {
      Texture id = new Texture("texture/remote/" + RandomStringUtils.randomAlphanumeric(16));

      public BlurredShadow(BufferedImage bufferedImage) {
         Render2DEngine.registerBufferedImageTexture(this.id, bufferedImage);
      }

      public void bind() {
         RenderSystem.setShaderTexture(0, this.id.getId());
      }
   }

   public record Rectangle(float x, float y, float x1, float y1) {
      public boolean contains(double x, double y) {
         return x >= this.x && x <= this.x1 && y >= this.y && y <= this.y1;
      }
   }
}
