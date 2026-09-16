package vcore.injection;

import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import vcore.utility.render.shaders.satin.impl.SamplerAccess;

@Mixin(ShaderInstance.class)
public abstract class MixinCoreShader implements SamplerAccess {
   @Shadow
   @Final
   private Map<String, Object> samplerMap;

   @Override
   public boolean hasSampler(String name) {
      return this.samplerMap.containsKey(name);
   }

   @Accessor("samplerNames")
   @Override
   public abstract List<String> getSamplerNames();

   @Accessor("samplerLocations")
   @Override
   public abstract List<Integer> getSamplerShaderLocs();
}
