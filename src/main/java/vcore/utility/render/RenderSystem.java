package vcore.utility.render;

import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class RenderSystem extends com.mojang.blaze3d.systems.RenderSystem {
   private static final float[] shaderColor = new float[]{1.0F, 1.0F, 1.0F, 1.0F};

   public static void enableBlend() {
      GL11.glEnable(GL11.GL_BLEND);
   }

   public static void disableBlend() {
      GL11.glDisable(GL11.GL_BLEND);
   }

   public static void enableDepthTest() {
      GL11.glEnable(GL11.GL_DEPTH_TEST);
   }

   public static void disableDepthTest() {
      GL11.glDisable(GL11.GL_DEPTH_TEST);
   }

   public static void defaultBlendFunc() {
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
   }

   public enum SourceFactor {
      SRC_ALPHA(GL11.GL_SRC_ALPHA),
      ONE(GL11.GL_ONE),
      ZERO(GL11.GL_ZERO);
      public final int value;
      SourceFactor(int value) { this.value = value; }
   }

   public enum DestFactor {
      ONE(GL11.GL_ONE),
      ONE_MINUS_SRC_ALPHA(GL11.GL_ONE_MINUS_SRC_ALPHA),
      ZERO(GL11.GL_ZERO),
      SRC_ALPHA(GL11.GL_SRC_ALPHA);
      public final int value;
      DestFactor(int value) { this.value = value; }
   }

   public static void blendFunc(int src, int dst) {
      GL11.glBlendFunc(src, dst);
   }

   public static void blendFunc(SourceFactor src, DestFactor dst) {
      GL11.glBlendFunc(src.value, dst.value);
   }

   public static void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
      org.lwjgl.opengl.GL14.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
   }

   public static void colorMask(boolean r, boolean g, boolean b, boolean a) {
      GL11.glColorMask(r, g, b, a);
   }

   public static void clearColor(float r, float g, float b, float a) {
      GL11.glClearColor(r, g, b, a);
   }

   public static void clear(int mask, boolean checkError) {
      GL11.glClear(mask);
   }

   public static void depthMask(boolean mask) {
      GL11.glDepthMask(mask);
   }

   public static void disableCull() {
      GL11.glDisable(GL11.GL_CULL_FACE);
   }

   public static void enableCull() {
      GL11.glEnable(GL11.GL_CULL_FACE);
   }

   public static void setShaderColor(float r, float g, float b, float a) {
      shaderColor[0] = r;
      shaderColor[1] = g;
      shaderColor[2] = b;
      shaderColor[3] = a;
   }

   public static float[] getShaderColor() {
      return shaderColor;
   }

   public static void lineWidth(float width) {
      GL11.glLineWidth(width);
   }

   public static void enableScissor(int x, int y, int width, int height) {
      GL11.glEnable(GL11.GL_SCISSOR_TEST);
      GL11.glScissor(x, y, width, height);
   }

   public static void disableScissor() {
      GL11.glDisable(GL11.GL_SCISSOR_TEST);
   }

   public static void texParameter(int target, int pname, int param) {
      GL11.glTexParameteri(target, pname, param);
   }

   private static final org.joml.Matrix4f projectionMatrix = new org.joml.Matrix4f();

   public static void setProjectionMatrix(org.joml.Matrix4f matrix) {
      projectionMatrix.set(matrix);
   }

   public static org.joml.Matrix4f getProjectionMatrix() {
      return projectionMatrix;
   }

   public static org.joml.Matrix4f getModelViewMatrix() {
      return com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix();
   }

   public static void applyModelViewMatrix() {
   }

   public static void setShader(Object shader) {
   }

   public static void setShaderTexture(int unit, Identifier texture) {
      GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
      if (texture != null) {
         try {
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
            if (tex != null) {
               com.mojang.blaze3d.textures.GpuTexture gpuTex = null;
               try {
                  gpuTex = tex.getTexture();
               } catch (IllegalStateException ignored) {}
               if (gpuTex instanceof GlTexture glTex) {
                  GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTex.glId());
               }
            }
         } catch (Exception ignored) {}
      }
   }
}
