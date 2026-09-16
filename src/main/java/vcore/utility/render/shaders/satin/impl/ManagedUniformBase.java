package vcore.utility.render.shaders.satin.impl;

import java.util.List;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderInstance;

public abstract class ManagedUniformBase {
   protected final String name;

   public ManagedUniformBase(String name) {
      this.name = name;
   }

   public abstract boolean findUniformTargets(List<PostPass> var1);

   public abstract boolean findUniformTarget(ShaderInstance var1);

   public String getName() {
      return this.name;
   }
}
