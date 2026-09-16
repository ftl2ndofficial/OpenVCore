package vcore.utility.render.shaders.satin.impl;

import com.mojang.blaze3d.shaders.Uniform;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform1f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform1i;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform2f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform2i;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform3f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform3i;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform4f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform4i;
import vcore.utility.render.shaders.satin.api.managed.uniform.UniformMat4;

public final class ManagedUniform
   extends ManagedUniformBase
   implements Uniform1i,
   Uniform2i,
   Uniform3i,
   Uniform4i,
   Uniform1f,
   Uniform2f,
   Uniform3f,
   Uniform4f,
   UniformMat4 {
   private static final Uniform[] NO_TARGETS = new Uniform[0];
   private final int count;
   private Uniform[] targets = NO_TARGETS;
   private int i0;
   private int i1;
   private int i2;
   private int i3;
   private float f0;
   private float f1;
   private float f2;
   private float f3;
   private boolean firstUpload = true;

   public ManagedUniform(String name, int count) {
      super(name);
      this.count = count;
   }

   @Override
   public boolean findUniformTargets(List<PostPass> shaders) {
      List<Uniform> list = new ArrayList<>();

      for (PostPass shader : shaders) {
         Uniform uniform = shader.getEffect().getUniform(this.name);
         if (uniform != null) {
            if (uniform.getCount() != this.count) {
               throw new IllegalStateException("Mismatched number of values, expected " + this.count + " but JSON definition declares " + uniform.getCount());
            }

            list.add(uniform);
         }
      }

      if (list.size() > 0) {
         this.targets = list.toArray(new Uniform[0]);
         this.syncCurrentValues();
         return true;
      } else {
         this.targets = NO_TARGETS;
         return false;
      }
   }

   @Override
   public boolean findUniformTarget(ShaderInstance shader) {
      Uniform uniform = shader.getUniform(this.name);
      if (uniform != null) {
         this.targets = new Uniform[]{uniform};
         this.syncCurrentValues();
         return true;
      } else {
         this.targets = NO_TARGETS;
         return false;
      }
   }

   private void syncCurrentValues() {
      if (!this.firstUpload) {
         for (Uniform target : this.targets) {
            if (target.getIntBuffer() != null) {
               target.setSafe(this.i0, this.i1, this.i2, this.i3);
            } else {
               assert target.getFloatBuffer() != null;
               target.setSafe(this.f0, this.f1, this.f2, this.f3);
            }
         }
      }
   }

   @Override
   public void set(int value) {
      Uniform[] targets = this.targets;
      int nbTargets = targets.length;
      if (nbTargets > 0 && (this.firstUpload || this.i0 != value)) {
         for (Uniform target : targets) {
            target.set(value);
         }

         this.i0 = value;
         this.firstUpload = false;
      }
   }

   @Override
   public void set(int value0, int value1) {
      Uniform[] targets = this.targets;
      int nbTargets = targets.length;
      if (nbTargets > 0 && (this.firstUpload || this.i0 != value0 || this.i1 != value1)) {
         for (Uniform target : targets) {
            target.set(value0, value1);
         }

         this.i0 = value0;
         this.i1 = value1;
         this.firstUpload = false;
      }
   }

   @Override
   public void set(int value0, int value1, int value2) {
      Uniform[] targets = this.targets;
      int nbTargets = targets.length;
      if (nbTargets > 0 && (this.firstUpload || this.i0 != value0 || this.i1 != value1 || this.i2 != value2)) {
         for (Uniform target : targets) {
            target.set(value0, value1, value2);
         }

         this.i0 = value0;
         this.i1 = value1;
         this.i2 = value2;
         this.firstUpload = false;
      }
   }

   @Override
   public void set(int value0, int value1, int value2, int value3) {
      Uniform[] targets = this.targets;
      int nbTargets = targets.length;
      if (nbTargets > 0 && (this.firstUpload || this.i0 != value0 || this.i1 != value1 || this.i2 != value2 || this.i3 != value3)) {
         for (Uniform target : targets) {
            target.set(value0, value1, value2, value3);
         }

         this.i0 = value0;
         this.i1 = value1;
         this.i2 = value2;
         this.i3 = value3;
         this.firstUpload = false;
      }
   }

   @Override
   public void set(float value) {
      Uniform[] targets = this.targets;
      int nbTargets = targets.length;
      if (nbTargets > 0 && (this.firstUpload || this.f0 != value)) {
         for (Uniform target : targets) {
            target.set(value);
         }

         this.f0 = value;
         this.firstUpload = false;
      }
   }

   @Override
   public void set(float value0, float value1) {
      Uniform[] targets = this.targets;
      int nbTargets = targets.length;
      if (nbTargets > 0 && (this.firstUpload || this.f0 != value0 || this.f1 != value1)) {
         for (Uniform target : targets) {
            target.set(value0, value1);
         }

         this.f0 = value0;
         this.f1 = value1;
         this.firstUpload = false;
      }
   }

   @Override
   public void set(Vector2f value) {
      this.set(value.x(), value.y());
   }

   @Override
   public void set(float value0, float value1, float value2) {
      Uniform[] targets = this.targets;
      int nbTargets = targets.length;
      if (nbTargets > 0 && (this.firstUpload || this.f0 != value0 || this.f1 != value1 || this.f2 != value2)) {
         for (Uniform target : targets) {
            target.set(value0, value1, value2);
         }

         this.f0 = value0;
         this.f1 = value1;
         this.f2 = value2;
         this.firstUpload = false;
      }
   }

   @Override
   public void set(Vector3f value) {
      this.set(value.x(), value.y(), value.z());
   }

   @Override
   public void set(float value0, float value1, float value2, float value3) {
      Uniform[] targets = this.targets;
      int nbTargets = targets.length;
      if (nbTargets > 0 && (this.firstUpload || this.f0 != value0 || this.f1 != value1 || this.f2 != value2 || this.f3 != value3)) {
         for (Uniform target : targets) {
            target.setMat2x2(value0, value1, value2, value3);
         }

         this.f0 = value0;
         this.f1 = value1;
         this.f2 = value2;
         this.f3 = value3;
         this.firstUpload = false;
      }
   }

   @Override
   public void set(Vector4f value) {
      this.set(value.x(), value.y(), value.z(), value.w());
   }

   @Override
   public void set(Matrix4f value) {
      Uniform[] targets = this.targets;
      int nbTargets = targets.length;
      if (nbTargets > 0) {
         for (Uniform target : targets) {
            target.set(value);
         }
      }
   }

   @Override
   public void setFromArray(float[] values) {
      if (this.count != values.length) {
         throw new IllegalArgumentException("Mismatched values size, expected " + this.count + " but got " + values.length);
      }

      Uniform[] targets = this.targets;
      int nbTargets = targets.length;
      if (nbTargets > 0) {
         for (Uniform target : targets) {
            target.set(values);
         }
      }
   }
}
