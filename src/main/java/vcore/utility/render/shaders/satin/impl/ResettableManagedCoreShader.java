package vcore.utility.render.shaders.satin.impl;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;
import vcore.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import vcore.utility.render.shaders.satin.api.managed.uniform.*;

public final class ResettableManagedCoreShader implements ManagedCoreShader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Matrix4f IDENTITY_MAT = new Matrix4f().identity();
   private final Identifier location;
   private final VertexFormat vertexFormat;
   private final Consumer<ManagedCoreShader> initCallback;
   private int programId = 0;
   private int uModelViewMat = -1;
   private int uProjMat = -1;
   private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
   private final Matrix4f cachedProjMat = new Matrix4f();
   private float cachedW = -1.0F;
   private float cachedH = -1.0F;
   private boolean modelViewUploaded = false;
   private final Map<String, Integer> uniformLocationCache = new HashMap<>();

   public ResettableManagedCoreShader(Identifier location, VertexFormat vertexFormat, Consumer<ManagedCoreShader> initCallback) {
      this.location = location;
      this.vertexFormat = vertexFormat;
      this.initCallback = initCallback;
   }

   public synchronized void ensureInitialized() {
      if (this.programId == 0) {
         this.initProgram();
      }
   }

   private void initProgram() {
      try {
         String fshPath = "/assets/" + this.location.getNamespace() + "/shaders/core/" + this.location.getPath() + ".fsh";
         String vshPath = "/assets/" + this.location.getNamespace() + "/shaders/core/" + this.location.getPath() + ".vsh";

         String vshSource = readResource(vshPath);
         if (vshSource == null) {
            vshSource = """
               #version 150
               in vec3 Position;
               in vec2 UV0;
               in vec4 Color;
               uniform mat4 ModelViewMat;
               uniform mat4 ProjMat;
               out vec2 texCoord;
               out vec2 texCoord0;
               out vec4 vertexColor;
               void main() {
                   gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                   texCoord = UV0;
                   texCoord0 = UV0;
                   vertexColor = Color;
               }
               """;
         }

         String fshSource = readResource(fshPath);
         if (fshSource == null) {
            LOGGER.error("Shader fragment source not found: {}", fshPath);
            return;
         }

         int vs = compileShader(GL20.GL_VERTEX_SHADER, vshSource);
         int fs = compileShader(GL20.GL_FRAGMENT_SHADER, fshSource);

         this.programId = GL20.glCreateProgram();
         GL20.glAttachShader(this.programId, vs);
         GL20.glAttachShader(this.programId, fs);

         GL20.glBindAttribLocation(this.programId, 0, "Position");
         GL20.glBindAttribLocation(this.programId, 1, "Color");
         GL20.glBindAttribLocation(this.programId, 2, "UV0");

         GL20.glLinkProgram(this.programId);
         if (GL20.glGetProgrami(this.programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Failed to link shader {}: {}", this.location, GL20.glGetProgramInfoLog(this.programId));
         }

         GL20.glDeleteShader(vs);
         GL20.glDeleteShader(fs);

         this.uModelViewMat = GL20.glGetUniformLocation(this.programId, "ModelViewMat");
         this.uProjMat = GL20.glGetUniformLocation(this.programId, "ProjMat");

         if (this.initCallback != null) {
            this.initCallback.accept(this);
         }
      } catch (Exception e) {
         LOGGER.error("Error compiling shader {}: {}", this.location, e.getMessage());
      }
   }

   private static String readResource(String path) {
      try (InputStream in = ResettableManagedCoreShader.class.getResourceAsStream(path)) {
         if (in == null) return null;
         return new String(in.readAllBytes(), StandardCharsets.UTF_8);
      } catch (Exception e) {
         return null;
      }
   }

   private static int compileShader(int type, String source) {
      int shader = GL20.glCreateShader(type);
      GL20.glShaderSource(shader, source);
      GL20.glCompileShader(shader);
      if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
         LOGGER.error("Shader compile error:\n{}", GL20.glGetShaderInfoLog(shader));
      }
      return shader;
   }

   @Override
   public int getProgram() {
      this.ensureInitialized();
      return this.programId;
   }

   @Override
   public void use() {
      this.ensureInitialized();
      if (this.programId == 0) return;
      com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(this.programId);
      BufferUploader.activeCustomProgram = this.programId;

      Minecraft mc = Minecraft.getInstance();
      float w = mc != null && mc.getWindow() != null ? (float) mc.getWindow().getGuiScaledWidth() : 1920.0F;
      float h = mc != null && mc.getWindow() != null ? (float) mc.getWindow().getGuiScaledHeight() : 1080.0F;

      if (this.uModelViewMat != -1 && !this.modelViewUploaded) {
         this.matrixBuffer.clear();
         IDENTITY_MAT.get(this.matrixBuffer);
         GL20.glUniformMatrix4fv(this.uModelViewMat, false, this.matrixBuffer);
         this.modelViewUploaded = true;
      }
      if (this.uProjMat != -1 && (w != this.cachedW || h != this.cachedH)) {
         this.cachedProjMat.setOrtho(0.0F, w, h, 0.0F, -1000.0F, 1000.0F);
         this.cachedW = w;
         this.cachedH = h;
         this.matrixBuffer.clear();
         this.cachedProjMat.get(this.matrixBuffer);
         GL20.glUniformMatrix4fv(this.uProjMat, false, this.matrixBuffer);
      }
   }

   @Override
   public void release() {
      com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(0);
      BufferUploader.activeCustomProgram = 0;
      BufferUploader.invalidateCommandEncoder();
      try {
         org.lwjgl.opengl.GL33.glBindSampler(0, 0);
      } catch (Throwable ignored) {}
   }

   private int getUniformLoc(String name) {
      this.ensureInitialized();
      if (this.programId == 0) return -1;
      return this.uniformLocationCache.computeIfAbsent(name, n -> GL20.glGetUniformLocation(this.programId, n));
   }

   @Override
   public Uniform1i findUniform1i(String name) {
      return val -> {
         int loc = getUniformLoc(name);
         if (loc != -1) {
            com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(this.programId);
            GL20.glUniform1i(loc, val);
         }
      };
   }

   @Override
   public Uniform2i findUniform2i(String name) {
      return (x, y) -> {
         int loc = getUniformLoc(name);
         if (loc != -1) {
            com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(this.programId);
            GL20.glUniform2i(loc, x, y);
         }
      };
   }

   @Override
   public Uniform3i findUniform3i(String name) {
      return (x, y, z) -> {
         int loc = getUniformLoc(name);
         if (loc != -1) {
            com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(this.programId);
            GL20.glUniform3i(loc, x, y, z);
         }
      };
   }

   @Override
   public Uniform4i findUniform4i(String name) {
      return (x, y, z, w) -> {
         int loc = getUniformLoc(name);
         if (loc != -1) {
            com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(this.programId);
            GL20.glUniform4i(loc, x, y, z, w);
         }
      };
   }

   @Override
   public Uniform1f findUniform1f(String name) {
      return val -> {
         int loc = getUniformLoc(name);
         if (loc != -1) {
            com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(this.programId);
            GL20.glUniform1f(loc, val);
         }
      };
   }

   @Override
   public Uniform2f findUniform2f(String name) {
      return (x, y) -> {
         int loc = getUniformLoc(name);
         if (loc != -1) {
            com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(this.programId);
            GL20.glUniform2f(loc, x, y);
         }
      };
   }

   @Override
   public Uniform3f findUniform3f(String name) {
      return (x, y, z) -> {
         int loc = getUniformLoc(name);
         if (loc != -1) {
            com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(this.programId);
            GL20.glUniform3f(loc, x, y, z);
         }
      };
   }

   @Override
   public Uniform4f findUniform4f(String name) {
      return (x, y, z, w) -> {
         int loc = getUniformLoc(name);
         if (loc != -1) {
            com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(this.programId);
            GL20.glUniform4f(loc, x, y, z, w);
         }
      };
   }

   @Override
   public UniformMat4 findUniformMat4(String name) {
      return matrix -> {
         int loc = getUniformLoc(name);
         if (loc != -1) {
            com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(this.programId);
            FloatBuffer fb = BufferUtils.createFloatBuffer(16);
            matrix.get(fb);
            GL20.glUniformMatrix4fv(loc, false, fb);
         }
      };
   }

   @Override
   public SamplerUniform findSampler(String name) {
      return new SamplerUniform() {
         @Override
         public void set(net.minecraft.client.renderer.texture.AbstractTexture var1) {
         }

         @Override
         public void set(com.mojang.blaze3d.pipeline.RenderTarget var1) {
         }

         @Override
         public void set(int textureId) {
            int loc = getUniformLoc(name);
            if (loc != -1) {
               com.mojang.blaze3d.opengl.GlStateManager._glUseProgram(ResettableManagedCoreShader.this.programId);
               GL20.glActiveTexture(GL20.GL_TEXTURE0);
               try {
                  org.lwjgl.opengl.GL33.glBindSampler(0, 0);
               } catch (Throwable ignored) {
               }
               GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
               com.mojang.blaze3d.opengl.GlStateManager._bindTexture(textureId);
               GL20.glUniform1i(loc, 0);
            }
         }
      };
   }
}
