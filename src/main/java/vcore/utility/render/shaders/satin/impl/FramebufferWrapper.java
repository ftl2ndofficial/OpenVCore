package vcore.utility.render.shaders.satin.impl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import vcore.utility.render.shaders.satin.api.managed.ManagedFramebuffer;

public final class FramebufferWrapper implements ManagedFramebuffer {
   private final String name;
   private RenderTarget wrapped;

   FramebufferWrapper(String name) {
      this.name = name;
   }

   void findTarget(PostChain shaderEffect) {
      if (shaderEffect == null) {
         this.wrapped = null;
      } else {
         this.wrapped = shaderEffect.getTempTarget(this.name);
         if (this.wrapped == null) {
            LogUtils.getLogger().warn("No target framebuffer found with name {} in shader {}", this.name, shaderEffect.getName());
         }
      }
   }

   public String getName() {
      return this.name;
   }

   @Override
   public RenderTarget getFramebuffer() {
      return this.wrapped;
   }

   @Override
   public void beginWrite(boolean updateViewport) {
      if (this.wrapped != null) {
         this.wrapped.bindWrite(updateViewport);
      }
   }

   @Override
   public void draw() {
      Window window = Minecraft.getInstance().getWindow();
      this.draw(window.getWidth(), window.getHeight(), true);
   }

   @Override
   public void draw(int width, int height, boolean disableBlend) {
      if (this.wrapped != null) {
         this.wrapped.blitToScreen(width, height, disableBlend);
      }
   }

   @Override
   public void clear() {
      this.clear(Minecraft.ON_OSX);
   }

   @Override
   public void clear(boolean swallowErrors) {
      if (this.wrapped != null) {
         this.wrapped.clear(swallowErrors);
      }
   }
}
