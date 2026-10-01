package vcore.utility.render.shaders.satin.api.managed.uniform;

import org.joml.Vector2f;

public interface Uniform2f {
   void set(float var1, float var2);

   default void set(Vector2f var1) {
      this.set(var1.x, var1.y);
   }
}
