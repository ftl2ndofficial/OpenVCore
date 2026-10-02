package vcore.injection.accesors;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlCommandEncoder.class)
public interface IGlCommandEncoder {
   @Accessor("lastPipeline")
   void setLastPipeline(RenderPipeline pipeline);

   @Accessor("lastProgram")
   void setLastProgram(GlProgram program);
}
