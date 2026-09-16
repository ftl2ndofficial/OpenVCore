package vcore.utility.render.shaders.satin.impl;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.impl.client.rendering.FabricShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import vcore.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import vcore.utility.render.shaders.satin.api.managed.uniform.SamplerUniform;

public final class ResettableManagedCoreShader extends ResettableManagedShaderBase<ShaderInstance> implements ManagedCoreShader {
   private final Consumer<ManagedCoreShader> initCallback;
   private final VertexFormat vertexFormat;
   private final Map<String, ManagedSamplerUniformV1> managedSamplers = new HashMap<>();

   public ResettableManagedCoreShader(ResourceLocation location, VertexFormat vertexFormat, Consumer<ManagedCoreShader> initCallback) {
      super(location);
      this.vertexFormat = vertexFormat;
      this.initCallback = initCallback;
   }

   protected ShaderInstance parseShader(ResourceProvider resourceManager, Minecraft mc, ResourceLocation location) throws IOException {
      return new FabricShaderProgram(resourceManager, this.getLocation(), this.vertexFormat);
   }

   @Override
   public void setup(int newWidth, int newHeight) {
      Preconditions.checkNotNull(this.shader);

      for (ManagedUniformBase uniform : this.getManagedUniforms()) {
         this.setupUniform(uniform, this.shader);
      }

      this.initCallback.accept(this);
   }

   @Override
   public ShaderInstance getProgram() {
      return this.shader;
   }

   protected boolean setupUniform(ManagedUniformBase uniform, ShaderInstance shader) {
      return uniform.findUniformTarget(shader);
   }

   @Override
   public SamplerUniform findSampler(String samplerName) {
      return this.manageUniform(this.managedSamplers, ManagedSamplerUniformV1::new, samplerName, "sampler");
   }

   @Override
   protected void logInitError(IOException e) {
      LogUtils.getLogger().error("Could not create shader program {}", this.getLocation(), e);
   }
}
