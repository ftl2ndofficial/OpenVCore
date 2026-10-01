package vcore.utility.render.shaders.satin.api.managed.uniform;

import org.joml.Matrix4f;

public interface UniformMat4 {
   void set(Matrix4f var1);

   default void setFromArray(float[] var1) {
      this.set(new Matrix4f().set(var1));
   }
}
