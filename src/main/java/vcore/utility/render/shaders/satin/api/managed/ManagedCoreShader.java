package vcore.utility.render.shaders.satin.api.managed;

import net.minecraft.client.renderer.ShaderInstance;
import vcore.utility.render.shaders.satin.api.managed.uniform.UniformFinder;

public interface ManagedCoreShader extends UniformFinder {
   ShaderInstance getProgram();

   void release();
}
