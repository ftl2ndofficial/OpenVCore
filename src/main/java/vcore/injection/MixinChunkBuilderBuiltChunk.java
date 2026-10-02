package vcore.injection;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.utility.render.chunk.ChunkAnimations;

@Mixin(RenderSection.class)
public class MixinChunkBuilderBuiltChunk {
   @Inject(method = "setSectionNode", at = @At("TAIL"))
   private void onSetSectionNode(long sectionNode, CallbackInfo ci) {
      int x = SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode));
      int y = SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode));
      int z = SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode));
      ChunkAnimations.INSTANCE.setPosition((RenderSection)(Object)this, new BlockPos(x, y, z));
   }

   @Inject(method = "setSectionMesh", at = @At("TAIL"))
   private void onSetSectionMesh(net.minecraft.client.renderer.chunk.SectionMesh mesh, CallbackInfo ci) {
      RenderSection section = (RenderSection)(Object)this;
      BlockPos origin = section.getRenderOrigin();
      if (origin != null) {
         ChunkAnimations.INSTANCE.setPosition(section, origin);
      }
   }
}
