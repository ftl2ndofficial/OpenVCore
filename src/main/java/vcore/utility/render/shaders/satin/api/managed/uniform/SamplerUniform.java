package vcore.utility.render.shaders.satin.api.managed.uniform;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.texture.AbstractTexture;

public interface SamplerUniform {
   void set(AbstractTexture var1);

   void set(RenderTarget var1);

   void set(int var1);
}
