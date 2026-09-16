package vcore.utility.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL30;

public class MSAAFramebuffer extends RenderTarget {
   public static final int MAX_SAMPLES = GL30.glGetInteger(36183);
   private static final Map<Integer, MSAAFramebuffer> INSTANCES = new HashMap<>();
   private final int samples;
   private int rboColor;
   private int rboDepth;

   public MSAAFramebuffer(int samples) {
      super(true);
      this.samples = samples;
      this.setClearColor(1.0F, 1.0F, 1.0F, 0.0F);
   }

   public static MSAAFramebuffer getInstance(int samples) {
      return INSTANCES.computeIfAbsent(samples, x -> new MSAAFramebuffer(samples));
   }

   public static void use(boolean fancy, Runnable drawAction) {
      use(Math.min(fancy ? 16 : 4, MAX_SAMPLES), Minecraft.getInstance().getMainRenderTarget(), drawAction);
   }

   public static void use(int samples, @NotNull RenderTarget mainBuffer, @NotNull Runnable drawAction) {
      RenderSystem.assertOnRenderThreadOrInit();
      MSAAFramebuffer msaaBuffer = getInstance(samples);
      msaaBuffer.resize(mainBuffer.width, mainBuffer.height, false);
      GlStateManager._glBindFramebuffer(36008, mainBuffer.frameBufferId);
      GlStateManager._glBindFramebuffer(36009, msaaBuffer.frameBufferId);
      GlStateManager._glBlitFrameBuffer(
         0, 0, msaaBuffer.width, msaaBuffer.height, 0, 0, msaaBuffer.width, msaaBuffer.height, 16384, 9729
      );
      msaaBuffer.bindWrite(true);
      drawAction.run();
      msaaBuffer.unbindWrite();
      GlStateManager._glBindFramebuffer(36008, msaaBuffer.frameBufferId);
      GlStateManager._glBindFramebuffer(36009, mainBuffer.frameBufferId);
      GlStateManager._glBlitFrameBuffer(
         0, 0, msaaBuffer.width, msaaBuffer.height, 0, 0, msaaBuffer.width, msaaBuffer.height, 16384, 9729
      );
      msaaBuffer.clear(false);
      mainBuffer.bindWrite(false);
   }

   public void resize(int width, int height, boolean getError) {
      if (this.width != width || this.height != height) {
         super.resize(width, height, getError);
      }
   }

   public void createBuffers(int width, int height, boolean getError) {
      RenderSystem.assertOnRenderThreadOrInit();
      this.viewWidth = width;
      this.viewHeight = height;
      this.width = width;
      this.height = height;
      this.frameBufferId = GlStateManager.glGenFramebuffers();
      GlStateManager._glBindFramebuffer(36160, this.frameBufferId);
      this.rboColor = GlStateManager.glGenRenderbuffers();
      GlStateManager._glBindRenderbuffer(36161, this.rboColor);
      GL30.glRenderbufferStorageMultisample(36161, this.samples, 32856, width, height);
      GlStateManager._glBindRenderbuffer(36161, 0);
      this.rboDepth = GlStateManager.glGenRenderbuffers();
      GlStateManager._glBindRenderbuffer(36161, this.rboDepth);
      GL30.glRenderbufferStorageMultisample(36161, this.samples, 6402, width, height);
      GlStateManager._glBindRenderbuffer(36161, 0);
      GL30.glFramebufferRenderbuffer(36160, 36064, 36161, this.rboColor);
      GL30.glFramebufferRenderbuffer(36160, 36096, 36161, this.rboDepth);
      this.colorTextureId = Minecraft.getInstance().getMainRenderTarget().getColorTextureId();
      this.depthBufferId = Minecraft.getInstance().getMainRenderTarget().getDepthTextureId();
      this.checkStatus();
      this.clear(getError);
      this.unbindRead();
   }

   public void destroyBuffers() {
      RenderSystem.assertOnRenderThreadOrInit();
      this.unbindRead();
      this.unbindWrite();
      if (this.frameBufferId > -1) {
         GlStateManager._glBindFramebuffer(36160, 0);
         GlStateManager._glDeleteFramebuffers(this.frameBufferId);
         this.frameBufferId = -1;
      }

      if (this.rboColor > -1) {
         GlStateManager._glDeleteRenderbuffers(this.rboColor);
         this.rboColor = -1;
      }

      if (this.rboDepth > -1) {
         GlStateManager._glDeleteRenderbuffers(this.rboDepth);
         this.rboDepth = -1;
      }

      this.colorTextureId = -1;
      this.depthBufferId = -1;
      this.width = -1;
      this.height = -1;
   }
}
