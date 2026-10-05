package vcore.injection;

import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vcore.core.manager.client.ModuleManager;
import vcore.utility.render.chunk.ChunkAnimations;

@Mixin(LevelRenderer.class)
public class MixinLevelRendererChunkAnimations {
   @Redirect(
      method = "prepareChunkRenders",
      at = @At(
         value = "NEW",
         target = "(Lorg/joml/Matrix4fc;IIIFII)Lnet/minecraft/client/renderer/DynamicUniforms$ChunkSectionInfo;"
      ),
      require = 0
   )
   private DynamicUniforms.ChunkSectionInfo redirectChunkSectionInfo(
      Matrix4fc modelView,
      int x,
      int y,
      int z,
      float visibility,
      int textureAtlasWidth,
      int textureAtlasHeight
   ) {
      if (ModuleManager.chunkAnimation != null && ModuleManager.chunkAnimation.isEnabled()) {
         Vec3 offset = ChunkAnimations.INSTANCE.getOffset(new BlockPos(x, y, z));
         if (offset != Vec3.ZERO) {
            Matrix4f animatedMatrix = new Matrix4f(modelView).translate((float)offset.x, (float)offset.y, (float)offset.z);
            return new DynamicUniforms.ChunkSectionInfo(animatedMatrix, x, y, z, visibility, textureAtlasWidth, textureAtlasHeight);
         }
      }
      return new DynamicUniforms.ChunkSectionInfo(modelView, x, y, z, visibility, textureAtlasWidth, textureAtlasHeight);
   }
}
