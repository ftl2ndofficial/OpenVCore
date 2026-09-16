package vcore.utility.render.shaders.satin.api.managed;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import vcore.utility.render.shaders.satin.impl.ReloadableShaderEffectManager;

public interface ShaderEffectManager {
   static ShaderEffectManager getInstance() {
      return ReloadableShaderEffectManager.INSTANCE;
   }

   ManagedShaderEffect manage(ResourceLocation var1);

   ManagedShaderEffect manage(ResourceLocation var1, Consumer<ManagedShaderEffect> var2);

   ManagedCoreShader manageCoreShader(ResourceLocation var1);

   ManagedCoreShader manageCoreShader(ResourceLocation var1, VertexFormat var2);

   ManagedCoreShader manageCoreShader(ResourceLocation var1, VertexFormat var2, Consumer<ManagedCoreShader> var3);
}
