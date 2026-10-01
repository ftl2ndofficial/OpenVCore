package vcore.utility.render.shaders.satin.api.managed;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import vcore.utility.render.shaders.satin.impl.ReloadableShaderEffectManager;

public interface ShaderEffectManager {
   static ShaderEffectManager getInstance() {
      return ReloadableShaderEffectManager.INSTANCE;
   }



   ManagedCoreShader manageCoreShader(Identifier var1);

   ManagedCoreShader manageCoreShader(Identifier var1, VertexFormat var2);

   ManagedCoreShader manageCoreShader(Identifier var1, VertexFormat var2, Consumer<ManagedCoreShader> var3);
}
