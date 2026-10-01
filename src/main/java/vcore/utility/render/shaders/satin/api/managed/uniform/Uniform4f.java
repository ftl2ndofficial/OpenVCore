package vcore.utility.render.shaders.satin.api.managed.uniform;

import org.joml.Vector4f;

public interface Uniform4f {
   void set(float var1, float var2, float var3, float var4);

   default void set(Vector4f var1) {
      this.set(var1.x, var1.y, var1.z, var1.w);
   }
}
