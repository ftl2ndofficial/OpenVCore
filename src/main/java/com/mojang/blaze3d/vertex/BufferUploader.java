package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import vcore.utility.render.Render3DEngine;

public class BufferUploader {
   public static int activeCustomProgram = 0;
   private static java.lang.reflect.Field lastPipelineField = null;
   private static java.lang.reflect.Field lastProgramField = null;
   private static boolean reflectionInitialized = false;

   public static void invalidateCommandEncoder() {
      try {
         GpuDevice device = RenderSystem.getDevice();
         if (device != null) {
            CommandEncoder encoder = device.createCommandEncoder();
            if (encoder instanceof vcore.injection.accesors.IGlCommandEncoder accessor) {
               accessor.setLastPipeline(null);
               accessor.setLastProgram(null);
               return;
            }
            if (!reflectionInitialized) {
               try {
                  lastPipelineField = encoder.getClass().getDeclaredField("lastPipeline");
                  lastPipelineField.setAccessible(true);
                  lastProgramField = encoder.getClass().getDeclaredField("lastProgram");
                  lastProgramField.setAccessible(true);
               } catch (Throwable ignored) {}
               reflectionInitialized = true;
            }
            if (lastPipelineField != null) {
               lastPipelineField.set(encoder, null);
            }
            if (lastProgramField != null) {
               lastProgramField.set(encoder, null);
            }
         }
      } catch (Throwable ignored) {
      }
   }

   private static int vao = 0;
   private static int vbo = 0;
   private static int ebo = 0;
   private static int eboCapacity = 0;
   private static int fallbackProgram = 0;
   private static int uModelViewMat = -1;
   private static int uProjMat = -1;
   private static int uUseTexture = -1;
   private static int uSampler0 = -1;
   private static int uColorModulator = -1;
   private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
   private static final Matrix4f IDENTITY_MAT = new Matrix4f().identity();
   private static final Matrix4f cachedOrthoMat = new Matrix4f();
   private static float cachedOrthoW = -1.0F;
   private static float cachedOrthoH = -1.0F;

   public static void reset() {
   }

   public static void invalidate() {
   }

   private static void initBuffers() {
      if (vao == 0) {
         vao = GL30.glGenVertexArrays();
         vbo = GL15.glGenBuffers();
         ebo = GL15.glGenBuffers();
      }
      if (fallbackProgram == 0) {
         initFallbackShader();
      }
   }

   private static void ensureQuadIndices(int requiredIndexCount) {
      if (ebo == 0) {
         ebo = GL15.glGenBuffers();
      }
      if (requiredIndexCount > eboCapacity) {
         int newCapacity = Math.max(requiredIndexCount, Math.max(eboCapacity * 2, 6000));
         int quadCount = (newCapacity + 5) / 6;
         newCapacity = quadCount * 6;
         java.nio.IntBuffer indices = BufferUtils.createIntBuffer(newCapacity);
         for (int i = 0; i < quadCount; i++) {
            int v = i * 4;
            indices.put(v);
            indices.put(v + 1);
            indices.put(v + 2);
            indices.put(v + 2);
            indices.put(v + 3);
            indices.put(v);
         }
         indices.flip();
         GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
         GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
         eboCapacity = newCapacity;
      }
   }

   private static void initFallbackShader() {
      String vSource = """
         #version 150
         in vec3 Position;
         in vec4 Color;
         in vec2 UV0;
         uniform mat4 ModelViewMat;
         uniform mat4 ProjMat;
         uniform vec4 ColorModulator;
         out vec4 vertexColor;
         out vec2 texCoord0;
         void main() {
             gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
             vertexColor = Color * ColorModulator;
             texCoord0 = UV0;
         }
         """;

      String fSource = """
         #version 150
         uniform sampler2D Sampler0;
         uniform int UseTexture;
         in vec4 vertexColor;
         in vec2 texCoord0;
         out vec4 fragColor;
         void main() {
             vec4 color = vertexColor;
             if (UseTexture != 0) {
                 color *= texture(Sampler0, texCoord0);
             }
             if (color.a < 0.005) discard;
             fragColor = color;
         }
         """;

      int vs = compileShader(GL20.GL_VERTEX_SHADER, vSource);
      int fs = compileShader(GL20.GL_FRAGMENT_SHADER, fSource);
      fallbackProgram = GL20.glCreateProgram();
      GL20.glAttachShader(fallbackProgram, vs);
      GL20.glAttachShader(fallbackProgram, fs);
      GL20.glBindAttribLocation(fallbackProgram, 0, "Position");
      GL20.glBindAttribLocation(fallbackProgram, 1, "Color");
      GL20.glBindAttribLocation(fallbackProgram, 2, "UV0");
      GL20.glLinkProgram(fallbackProgram);
      GL20.glDeleteShader(vs);
      GL20.glDeleteShader(fs);

      uModelViewMat = GL20.glGetUniformLocation(fallbackProgram, "ModelViewMat");
      uProjMat = GL20.glGetUniformLocation(fallbackProgram, "ProjMat");
      uUseTexture = GL20.glGetUniformLocation(fallbackProgram, "UseTexture");
      uSampler0 = GL20.glGetUniformLocation(fallbackProgram, "Sampler0");
      uColorModulator = GL20.glGetUniformLocation(fallbackProgram, "ColorModulator");
   }

   private static int compileShader(int type, String source) {
      int shader = GL20.glCreateShader(type);
      GL20.glShaderSource(shader, source);
      GL20.glCompileShader(shader);
      return shader;
   }

   public static int getMainRenderTargetFbo() {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null) return 0;
         RenderTarget target = mc.getMainRenderTarget();
         if (target == null) return 0;
         GpuDevice device = RenderSystem.getDevice();
         if (device instanceof GlDevice glDevice && target.getColorTextureView() instanceof GlTextureView colorView) {
            GpuTexture depthTex = target.useDepth ? target.getDepthTexture() : null;
            return colorView.getFbo(glDevice.directStateAccess(), depthTex);
         }
      } catch (Throwable ignored) {
      }
      return 0;
   }

   public static void draw(MeshData meshData) {
      drawWithShader(meshData);
   }

   public static void drawWithShader(MeshData meshData) {
      if (meshData == null) return;
      try {
         MeshData.DrawState state = meshData.drawState();
         int vertexCount = state.vertexCount();
         if (vertexCount <= 0) return;

         ByteBuffer buffer = meshData.vertexBuffer();
         if (buffer == null || buffer.remaining() == 0) return;

         initBuffers();

         Minecraft mc = Minecraft.getInstance();
         int targetFbo = getMainRenderTargetFbo();
         if (targetFbo != 0) {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, targetFbo);
         }

         if (mc != null && mc.getWindow() != null) {
            GlStateManager._viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
         }

         boolean isCustomActive = (activeCustomProgram != 0);
         boolean usedFallback = false;

         if (!isCustomActive) {
            GlStateManager._glUseProgram(fallbackProgram);
            usedFallback = true;

            Matrix4f modelView;
            Matrix4f proj;

            if (Render3DEngine.rendering3D) {
               modelView = Render3DEngine.lastModMat;
               proj = Render3DEngine.lastProjMat;
            } else {
               float w = mc != null && mc.getWindow() != null ? (float) mc.getWindow().getGuiScaledWidth() : 1920.0F;
               float h = mc != null && mc.getWindow() != null ? (float) mc.getWindow().getGuiScaledHeight() : 1080.0F;
               if (w != cachedOrthoW || h != cachedOrthoH) {
                  cachedOrthoMat.setOrtho(0.0F, w, h, 0.0F, -1000.0F, 1000.0F);
                  cachedOrthoW = w;
                  cachedOrthoH = h;
               }
               modelView = IDENTITY_MAT;
               proj = cachedOrthoMat;
            }

            matrixBuffer.clear();
            modelView.get(matrixBuffer);
            GL20.glUniformMatrix4fv(uModelViewMat, false, matrixBuffer);

            matrixBuffer.clear();
            proj.get(matrixBuffer);
            GL20.glUniformMatrix4fv(uProjMat, false, matrixBuffer);

            if (uColorModulator != -1) {
               float[] sc = vcore.utility.render.RenderSystem.getShaderColor();
               if (sc != null && sc.length >= 4) {
                  GL20.glUniform4f(uColorModulator, sc[0], sc[1], sc[2], sc[3]);
               } else {
                  GL20.glUniform4f(uColorModulator, 1.0F, 1.0F, 1.0F, 1.0F);
               }
            }

            if (uSampler0 != -1) {
               GL20.glUniform1i(uSampler0, 0);
               try {
                  GL33.glBindSampler(0, 0);
               } catch (Throwable ignored) {}
            }

            boolean hasTex = state.format().contains(VertexFormatElement.UV0);
            GL20.glUniform1i(uUseTexture, hasTex ? 1 : 0);
         }

         GlStateManager._glBindVertexArray(vao);
         GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
         GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STREAM_DRAW);

         VertexFormat format = state.format();
         int stride = format.getVertexSize();

         GL20.glVertexAttrib4f(1, 1.0F, 1.0F, 1.0F, 1.0F);
         GL20.glVertexAttrib2f(2, 0.0F, 0.0F);

         int posOffset = format.getOffset(VertexFormatElement.POSITION);
         if (posOffset != -1) {
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, posOffset);
         }
         int colorOffset = format.getOffset(VertexFormatElement.COLOR);
         if (colorOffset != -1) {
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 4, GL11.GL_UNSIGNED_BYTE, true, stride, colorOffset);
         }
         int uvOffset = format.getOffset(VertexFormatElement.UV0);
         if (uvOffset != -1) {
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride, uvOffset);
         }

         ByteBuffer indexBuffer = meshData.indexBuffer();
         if (indexBuffer != null && indexBuffer.remaining() > 0) {
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STREAM_DRAW);
            int glIndexType = state.indexType() == VertexFormat.IndexType.SHORT ? GL11.GL_UNSIGNED_SHORT : GL11.GL_UNSIGNED_INT;
            int glMode = state.mode() == VertexFormat.Mode.QUADS ? GL11.GL_TRIANGLES : getGlMode(state.mode());
            GL11.glDrawElements(glMode, state.indexCount(), glIndexType, 0L);
         } else if (state.mode() == VertexFormat.Mode.QUADS) {
            int quadCount = vertexCount / 4;
            if (quadCount > 0) {
               int indexCount = quadCount * 6;
               ensureQuadIndices(indexCount);
               GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
               GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0L);
            }
         } else {
            int glMode = getGlMode(state.mode());
            GL11.glDrawArrays(glMode, 0, vertexCount);
         }

         if (posOffset != -1) GL20.glDisableVertexAttribArray(0);
         if (colorOffset != -1) GL20.glDisableVertexAttribArray(1);
         if (uvOffset != -1) GL20.glDisableVertexAttribArray(2);
         GlStateManager._glBindVertexArray(0);
         GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

         if (usedFallback) {
            GlStateManager._glUseProgram(0);
         }

         GlStateManager._activeTexture(GL13.GL_TEXTURE0);
         invalidateCommandEncoder();
      } finally {
         meshData.close();
      }
   }

   private static int getGlMode(VertexFormat.Mode mode) {
      return switch (mode) {
         case LINES, DEBUG_LINES -> GL11.GL_LINES;
         case DEBUG_LINE_STRIP -> GL11.GL_LINE_STRIP;
         case TRIANGLES, QUADS -> GL11.GL_TRIANGLES;
         case TRIANGLE_STRIP -> GL11.GL_TRIANGLE_STRIP;
         case TRIANGLE_FAN -> GL11.GL_TRIANGLE_FAN;
         case POINTS -> GL11.GL_POINTS;
      };
   }
}
