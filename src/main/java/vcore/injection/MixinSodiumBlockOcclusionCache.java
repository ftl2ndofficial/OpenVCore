package vcore.injection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.misc.XRay;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
public class MixinSodiumBlockOcclusionCache {
   @Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true, require = 0)
   void shouldDrawSideHook(BlockState state, BlockGetter view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
      if (ModuleManager.xray.shouldUseWallHack()) {
         cir.setReturnValue(XRay.isCheckableOre(state.getBlock()));
      }
   }
}
