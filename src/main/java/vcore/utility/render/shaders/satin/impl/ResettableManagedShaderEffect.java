package vcore.utility.render.shaders.satin.impl;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import vcore.injection.accesors.AccessiblePassesShaderEffect;
import vcore.utility.render.shaders.satin.api.managed.ManagedFramebuffer;
import vcore.utility.render.shaders.satin.api.managed.ManagedShaderEffect;
import vcore.utility.render.shaders.satin.api.managed.uniform.SamplerUniformV2;

public final class ResettableManagedShaderEffect extends ResettableManagedShaderBase<PostChain> implements ManagedShaderEffect {
   private final Consumer<ManagedShaderEffect> initCallback;
   private final Map<String, FramebufferWrapper> managedTargets;
   private final Map<String, ManagedSamplerUniformV2> managedSamplers = new HashMap<>();

   public ResettableManagedShaderEffect(ResourceLocation location, Consumer<ManagedShaderEffect> initCallback) {
      super(location);
      this.initCallback = initCallback;
      this.managedTargets = new HashMap<>();
   }

   @Override
   public PostChain getShaderEffect() {
      return this.getShaderOrLog();
   }

   protected PostChain parseShader(ResourceProvider resourceFactory, Minecraft mc, ResourceLocation location) throws IOException {
      return new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), location);
   }

   @Override
   public void setup(int windowWidth, int windowHeight) {
      Preconditions.checkNotNull(this.shader);
      this.shader.resize(windowWidth, windowHeight);

      for (ManagedUniformBase uniform : this.getManagedUniforms()) {
         this.setupUniform(uniform, this.shader);
      }

      for (FramebufferWrapper buf : this.managedTargets.values()) {
         buf.findTarget(this.shader);
      }

      this.initCallback.accept(this);
   }

   @Override
   public void render(float tickDelta) {
      PostChain sg = this.getShaderEffect();
      if (sg != null) {
         RenderSystem.disableBlend();
         RenderSystem.disableDepthTest();
         RenderSystem.resetTextureMatrix();
         sg.process(tickDelta);
         Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
         RenderSystem.disableBlend();
         RenderSystem.blendFunc(770, 771);
         RenderSystem.enableDepthTest();
      }
   }

   @Override
   public ManagedFramebuffer getTarget(String name) {
      return this.managedTargets.computeIfAbsent(name, n -> {
         FramebufferWrapper ret = new FramebufferWrapper(n);
         if (this.shader != null) {
            ret.findTarget(this.shader);
         }

         return ret;
      });
   }

   @Override
   public void setUniformValue(String uniformName, int value) {
      this.findUniform1i(uniformName).set(value);
   }

   @Override
   public void setUniformValue(String uniformName, float value) {
      this.findUniform1f(uniformName).set(value);
   }

   @Override
   public void setUniformValue(String uniformName, float value0, float value1) {
      this.findUniform2f(uniformName).set(value0, value1);
   }

   @Override
   public void setUniformValue(String uniformName, float value0, float value1, float value2) {
      this.findUniform3f(uniformName).set(value0, value1, value2);
   }

   @Override
   public void setUniformValue(String uniformName, float value0, float value1, float value2, float value3) {
      this.findUniform4f(uniformName).set(value0, value1, value2, value3);
   }

   @Override
   public SamplerUniformV2 findSampler(String samplerName) {
      return this.manageUniform(this.managedSamplers, ManagedSamplerUniformV2::new, samplerName, "sampler");
   }

   protected boolean setupUniform(ManagedUniformBase uniform, PostChain shader) {
      return uniform.findUniformTargets(((AccessiblePassesShaderEffect)shader).getPasses());
   }

   @Override
   protected void logInitError(IOException e) {
      LogUtils.getLogger().error("Could not create screen shader {}", this.getLocation(), e);
   }

   private PostChain getShaderOrLog() {
      if (!this.isInitialized() && !this.isErrored()) {
         this.initializeOrLog(Minecraft.getInstance().getResourceManager());
      }

      return this.shader;
   }
}
