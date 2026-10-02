package vcore.utility.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL33;

public class RenderSystem extends com.mojang.blaze3d.systems.RenderSystem {
   private static final float[] shaderColor = new float[]{1.0F, 1.0F, 1.0F, 1.0F};

   public static void enableBlend() {
      GlStateManager._enableBlend();
   }

   public static void disableBlend() {
      GlStateManager._disableBlend();
   }

   public static void enableDepthTest() {
      GlStateManager._enableDepthTest();
   }

   public static void disableDepthTest() {
      GlStateManager._disableDepthTest();
   }

   public static void depthFunc(int func) {
      GlStateManager._depthFunc(func);
   }

   public static void defaultBlendFunc() {
      GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
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
      GlStateManager._blendFuncSeparate(src, dst, src, dst);
   }

   public static void blendFunc(SourceFactor src, DestFactor dst) {
      GlStateManager._blendFuncSeparate(src.value, dst.value, src.value, dst.value);
   }

   public static void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
      GlStateManager._blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
   }

   public static void colorMask(boolean r, boolean g, boolean b, boolean a) {
      GlStateManager._colorMask(r, g, b, a);
   }

   public static void clearColor(float r, float g, float b, float a) {
      GL11.glClearColor(r, g, b, a);
   }

   public static void clear(int mask, boolean checkError) {
      GlStateManager._clear(mask);
   }

   public static void depthMask(boolean mask) {
      GlStateManager._depthMask(mask);
   }

   public static void disableCull() {
      GlStateManager._disableCull();
   }

   public static void enableCull() {
      GlStateManager._enableCull();
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
      // In OpenGL 3.2+ Core Profile and modern GPU drivers (e.g. NVIDIA),
      // glLineWidth with values != 1.0F is rejected with GL_INVALID_VALUE (1281):
      // 'Operation is not valid from a preview context.'
      // Blaze3D line rendering is handled via shaders and quads.
   }

   public static void enableScissor(int x, int y, int width, int height) {
      GlStateManager._enableScissorTest();
      GlStateManager._scissorBox(x, y, width, height);
   }

   public static void disableScissor() {
      GlStateManager._disableScissorTest();
   }

   public static void texParameter(int target, int pname, int param) {
      GlStateManager._texParameter(target, pname, param);
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
      GlStateManager._activeTexture(GL13.GL_TEXTURE0 + unit);
      if (texture != null) {
         try {
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
            if (tex != null) {
               com.mojang.blaze3d.textures.GpuTexture gpuTex = null;
               try {
                  gpuTex = tex.getTexture();
               } catch (IllegalStateException ignored) {}
               if (gpuTex instanceof GlTexture glTex) {
                  try {
                     GL33.glBindSampler(unit, 0);
                  } catch (Throwable ignored) {}
                  GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTex.glId());
                  GlStateManager._bindTexture(glTex.glId());
               }
            }
         } catch (Exception ignored) {}
      }
   }
}
