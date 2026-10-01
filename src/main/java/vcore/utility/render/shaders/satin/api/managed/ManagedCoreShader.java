package vcore.utility.render.shaders.satin.api.managed;

import vcore.utility.render.shaders.satin.api.managed.uniform.UniformFinder;

public interface ManagedCoreShader extends UniformFinder {
   int getProgram();

   void use();

   void release();
}
