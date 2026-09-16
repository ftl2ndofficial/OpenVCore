package vcore.injection.accesors;

import com.mojang.blaze3d.shaders.Uniform;
import java.util.Map;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderInstance.class)
public interface ShaderProgramAccessor {
   @Accessor("uniformMap")
   Map<String, Uniform> getUniformsHook();
}
