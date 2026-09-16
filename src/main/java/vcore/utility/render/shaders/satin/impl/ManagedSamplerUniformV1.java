package vcore.utility.render.shaders.satin.impl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class ManagedSamplerUniformV1 extends ManagedSamplerUniformBase {
   public ManagedSamplerUniformV1(String name) {
      super(name);
   }

   @Override
   public void set(AbstractTexture texture) {
      this.set((Object)texture);
   }

   @Override
   public void set(RenderTarget textureFbo) {
      this.set((Object)textureFbo);
   }

   @Override
   public void set(int textureName) {
      this.set(Integer.valueOf(textureName));
   }

   @Override
   protected void set(Object value) {
      SamplerAccess[] targets = this.targets;
      if (targets.length > 0 && this.cachedValue != value) {
         for (SamplerAccess target : targets) {
            ((ShaderInstance)target).setSampler(this.name, value);
         }

         this.cachedValue = value;
      }
   }
}
