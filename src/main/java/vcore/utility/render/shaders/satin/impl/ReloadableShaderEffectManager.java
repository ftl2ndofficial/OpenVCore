package vcore.utility.render.shaders.satin.impl;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import vcore.utility.render.WindowResizeCallback;
import vcore.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import vcore.utility.render.shaders.satin.api.managed.ManagedShaderEffect;
import vcore.utility.render.shaders.satin.api.managed.ShaderEffectManager;

public final class ReloadableShaderEffectManager implements ShaderEffectManager {
   public static final ReloadableShaderEffectManager INSTANCE = new ReloadableShaderEffectManager();
   private final Set<ResettableManagedShaderBase<?>> managedShaders = new ReferenceOpenHashSet();

   public ReloadableShaderEffectManager() {
      WindowResizeCallback.EVENT
         .register((WindowResizeCallback)(client, window) -> this.onResolutionChanged(window.getWidth(), window.getHeight()));
   }

   @Override
   public ManagedShaderEffect manage(ResourceLocation location) {
      return this.manage(location, s -> {});
   }

   @Override
   public ManagedShaderEffect manage(ResourceLocation location, Consumer<ManagedShaderEffect> initCallback) {
      ResettableManagedShaderEffect ret = new ResettableManagedShaderEffect(location, initCallback);
      this.managedShaders.add(ret);
      return ret;
   }

   @Override
   public ManagedCoreShader manageCoreShader(ResourceLocation location) {
      return this.manageCoreShader(location, DefaultVertexFormat.NEW_ENTITY);
   }

   @Override
   public ManagedCoreShader manageCoreShader(ResourceLocation location, VertexFormat vertexFormat) {
      return this.manageCoreShader(location, vertexFormat, s -> {});
   }

   @Override
   public ManagedCoreShader manageCoreShader(ResourceLocation location, VertexFormat vertexFormat, Consumer<ManagedCoreShader> initCallback) {
      ResettableManagedCoreShader ret = new ResettableManagedCoreShader(location, vertexFormat, initCallback);
      this.managedShaders.add(ret);
      return ret;
   }

   public void reload(ResourceProvider shaderResources) {
      for (ResettableManagedShaderBase<?> ss : this.managedShaders) {
         ss.initializeOrLog(shaderResources);
      }
   }

   public void onResolutionChanged(int newWidth, int newHeight) {
      this.runShaderSetup(newWidth, newHeight);
   }

   private void runShaderSetup(int newWidth, int newHeight) {
      if (!this.managedShaders.isEmpty()) {
         for (ResettableManagedShaderBase<?> ss : this.managedShaders) {
            if (ss.isInitialized()) {
               ss.setup(newWidth, newHeight);
            }
         }
      }
   }
}
