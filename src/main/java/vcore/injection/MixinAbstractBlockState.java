package vcore.injection;

import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.client.ModuleManager;

@Mixin(BlockStateBase.class)
public class MixinAbstractBlockState {
   @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
   public void getLuminanceHook(CallbackInfoReturnable<Integer> cir) {
      if (ModuleManager.xray.shouldUseWallHack()) {
         cir.setReturnValue(15);
      }
   }
}
