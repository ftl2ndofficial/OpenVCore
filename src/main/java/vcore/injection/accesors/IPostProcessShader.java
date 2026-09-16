package vcore.injection.accesors;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostPass.class)
public interface IPostProcessShader {
   @Mutable
   @Accessor("inTarget")
   void setInput(RenderTarget var1);

   @Mutable
   @Accessor("outTarget")
   void setOutput(RenderTarget var1);
}
