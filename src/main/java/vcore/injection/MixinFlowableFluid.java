package vcore.injection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.client.ModuleManager;

@Mixin(FlowingFluid.class)
public abstract class MixinFlowableFluid {
   @Shadow
   protected abstract boolean isSolidFace(BlockGetter var1, BlockPos var2, Direction var3);

   @Inject(method = "getFlow", at = @At("HEAD"), cancellable = true)
   private void getVelocityHook(BlockGetter world, BlockPos pos, FluidState state, CallbackInfoReturnable<Vec3> cir) {
      if (ModuleManager.noPush.isEnabled() && ModuleManager.noPush.water.getValue()) {
         double d = 0.0;
         double e = 0.0;
         MutableBlockPos mutable = new MutableBlockPos();
         Vec3 vec3d = new Vec3(d, 0.0, e);
         if ((Boolean)state.getValue(FlowingFluid.FALLING)) {
            for (Direction direction2 : Plane.HORIZONTAL) {
               mutable.setWithOffset(pos, direction2);
               if (this.isSolidFace(world, mutable, direction2) || this.isSolidFace(world, mutable.above(), direction2)) {
                  vec3d = vec3d.normalize().add(0.0, -6.0, 0.0);
                  break;
               }
            }
         }

         cir.setReturnValue(vec3d.normalize());
      }
   }
}
