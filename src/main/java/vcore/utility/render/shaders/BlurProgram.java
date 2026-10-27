package vcore.utility.render.shaders;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.awt.Color;
import java.nio.ByteBuffer;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import vcore.features.modules.Module;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.WindowResizeCallback;
import vcore.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import vcore.utility.render.shaders.satin.api.managed.ShaderEffectManager;
import vcore.utility.render.shaders.satin.api.managed.uniform.SamplerUniform;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform1f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform2f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform4f;

public class BlurProgram {
   private static boolean frameCaptured = false;
   private Uniform2f uSize;
   private Uniform2f uLocation;
   private Uniform1f radius;
   private Uniform2f inputResolution;
   private Uniform1f opacity;
   private Uniform1f quality;
   private Uniform4f color1;
   private SamplerUniform sampler;
   private int readFbo = 0;
   private int fbo = 0;
   private int textureId = 0;
   private int fboWidth = 0;
   private int fboHeight = 0;
   private int attachedMainTexId = 0;
   public static final ManagedCoreShader BLUR = ShaderEffectManager.getInstance()
      .manageCoreShader(Identifier.fromNamespaceAndPath("vcore", "blur"), DefaultVertexFormat.POSITION);

   public BlurProgram() {
      this.setup();
   }

   public static void beginFrame() {
      frameCaptured = false;
   }

   private void initFbo(int width, int height) {
      if (width <= 0 || height <= 0) return;
      if (this.readFbo == 0) {
         this.readFbo = GL30.glGenFramebuffers();
      }
      if (this.fbo == 0) {
         this.fbo = GL30.glGenFramebuffers();
         this.textureId = GL11.glGenTextures();
      }
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureId);
      GlStateManager._bindTexture(this.textureId);
      GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

      int prevDrawFbo = GlStateManager.getFrameBuffer(GL30.GL_DRAW_FRAMEBUFFER);
      GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.fbo);
      GlStateManager._glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.textureId, 0);
      GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
      GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);

      this.fboWidth = width;
      this.fboHeight = height;
      this.attachedMainTexId = 0;
      frameCaptured = false;
   }

   public void setParameters(float x, float y, float width, float height, float r, Color c1, float blurStrenth, float blurOpacity) {
      int w = Module.mc.getWindow().getWidth();
      int h = Module.mc.getWindow().getHeight();
      if (this.fbo == 0 || this.fboWidth != w || this.fboHeight != h) {
         this.initFbo(w, h);
      }

      float i = (float)Module.mc.getWindow().getGuiScale();
      this.radius.set(r * i);
      this.uLocation.set(x * i, -y * i + Module.mc.getWindow().getGuiScaledHeight() * i - height * i);
      this.uSize.set(width * i, height * i);
      this.opacity.set(blurOpacity);
      this.quality.set(blurStrenth);
      this.color1.set(c1.getRed() / 255.0F, c1.getGreen() / 255.0F, c1.getBlue() / 255.0F, 1.0F);
   }

   public void use() {
      int w = Module.mc.getWindow().getWidth();
      int h = Module.mc.getWindow().getHeight();
      if (this.fbo == 0 || this.fboWidth != w || this.fboHeight != h) {
         this.initFbo(w, h);
      }

      if (!frameCaptured) {
         frameCaptured = true;
         RenderTarget target = Module.mc.getMainRenderTarget();
         int mainTexId = 0;
         Object colorTexObj = target != null ? target.getColorTexture() : null;
         if (colorTexObj instanceof com.mojang.blaze3d.opengl.GlTexture glTex) {
            mainTexId = glTex.glId();
         }

         if (mainTexId > 0 && this.readFbo != 0 && this.fbo != 0) {
            int prevReadFbo = GlStateManager.getFrameBuffer(GL30.GL_READ_FRAMEBUFFER);
            int prevDrawFbo = GlStateManager.getFrameBuffer(GL30.GL_DRAW_FRAMEBUFFER);
            boolean scissor = Render2DEngine.isScissorActive();
            if (scissor) {
               GlStateManager._disableScissorTest();
            }

            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.readFbo);
            if (this.attachedMainTexId != mainTexId) {
               GlStateManager._glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, mainTexId, 0);
               GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
               this.attachedMainTexId = mainTexId;
            }

            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.fbo);
            GlStateManager._glBlitFrameBuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);

            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            if (scissor) {
               GlStateManager._enableScissorTest();
            }
         }

         this.inputResolution.set(w, h);
      }

      this.sampler.set(this.textureId);
      BLUR.use();
   }

   protected void setup() {
      this.inputResolution = BLUR.findUniform2f("InputResolution");
      this.opacity = BLUR.findUniform1f("Opacity");
      this.quality = BLUR.findUniform1f("Quality");
      this.color1 = BLUR.findUniform4f("color1");
      this.uSize = BLUR.findUniform2f("uSize");
      this.uLocation = BLUR.findUniform2f("uLocation");
      this.radius = BLUR.findUniform1f("radius");
      this.sampler = BLUR.findSampler("InputSampler");
      WindowResizeCallback.EVENT.register((WindowResizeCallback)(client, window) -> {
         if (this.fbo != 0) {
            this.initFbo(window.getWidth(), window.getHeight());
         }
      });
   }
}
