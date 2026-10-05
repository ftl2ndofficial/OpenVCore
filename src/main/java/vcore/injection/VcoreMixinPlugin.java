package vcore.injection;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class VcoreMixinPlugin implements IMixinConfigPlugin {
   private static final String MIXIN_WORLD_RENDERER_CHUNK_ANIM = "MixinWorldRendererChunkAnimations";
   private static final String MIXIN_LEVEL_RENDERER_CHUNK_ANIM = "MixinLevelRendererChunkAnimations";
   private static final String MIXIN_SODIUM_DEFAULT_CHUNK_RENDERER = "MixinSodiumDefaultChunkRenderer";
   private static final String MIXIN_SODIUM_RENDER_SECTION_MANAGER_CHUNK_ANIM = "MixinSodiumRenderSectionManagerChunkAnimations";
   private static final String MIXIN_SODIUM_BLOCK_OCCLUSION_CACHE = "MixinSodiumBlockOcclusionCache";

   public void onLoad(String mixinPackage) {
      if (FabricLoader.getInstance().isModLoaded("sodium")) {
         System.out.println("[Vcore] Sodium mod detected! ChunkAnimation module will not work with Sodium.");
      }
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      boolean sodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");
      if (mixinClassName.endsWith(MIXIN_WORLD_RENDERER_CHUNK_ANIM) || mixinClassName.endsWith(MIXIN_LEVEL_RENDERER_CHUNK_ANIM)) {
         return !sodiumLoaded;
      } else if (mixinClassName.endsWith(MIXIN_SODIUM_DEFAULT_CHUNK_RENDERER)) {
         return false;
      } else if (mixinClassName.endsWith(MIXIN_SODIUM_BLOCK_OCCLUSION_CACHE)) {
         return false;
      } else if (mixinClassName.endsWith(MIXIN_SODIUM_RENDER_SECTION_MANAGER_CHUNK_ANIM)) {
         return sodiumLoaded;
      } else {
         return true;
      }
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
