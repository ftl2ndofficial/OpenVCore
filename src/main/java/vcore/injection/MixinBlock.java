package vcore.injection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.IManager;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.misc.XRay;

@Mixin(Block.class)
public abstract class MixinBlock {
   @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
   private static void shouldDrawSideHook(
      BlockState state, BlockState neighborState, Direction side, CallbackInfoReturnable<Boolean> cir
   ) {
      if (ModuleManager.xray.shouldUseWallHack()) {
         cir.setReturnValue(XRay.isCheckableOre(state.getBlock()));
      }
   }

   @Inject(method = "getSpeedFactor", at = @At("HEAD"), cancellable = true)
   public void getVelocityMultiplierHook(CallbackInfoReturnable<Float> cir) {
      if (ModuleManager.noSlow.isEnabled()) {
         if (ModuleManager.noSlow.soulSand.getValue() && (Object)this == Blocks.SOUL_SAND) {
            cir.setReturnValue(Blocks.DIRT.getSpeedFactor());
         }

         if (ModuleManager.noSlow.honey.getValue() && (Object)this == Blocks.HONEY_BLOCK) {
            cir.setReturnValue(Blocks.DIRT.getSpeedFactor());
         }
      }
   }

   @Inject(method = "getFriction", at = @At("HEAD"), cancellable = true)
   public void getSlipperinessHook(CallbackInfoReturnable<Float> cir) {
      if (ModuleManager.noSlow.isEnabled()) {
         if (ModuleManager.noSlow.slime.getValue() && (Object)this == Blocks.SLIME_BLOCK) {
            cir.setReturnValue(Blocks.DIRT.getFriction());
         }

         if (ModuleManager.noSlow.ice.getValue()
            && ((Object)this == Blocks.ICE || (Object)this == Blocks.PACKED_ICE || (Object)this == Blocks.BLUE_ICE || (Object)this == Blocks.FROSTED_ICE)
            && !IManager.mc.options.keyJump.isDown()) {
            cir.setReturnValue(Blocks.DIRT.getFriction());
         }
      }
   }
}
