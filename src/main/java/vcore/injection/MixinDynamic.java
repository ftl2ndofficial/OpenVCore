package vcore.injection;

import net.minecraft.client.DeltaTracker.Timer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;

@Mixin(Timer.class)
public class MixinDynamic {
   @Shadow
   private float deltaTicks;
   @Shadow
   private float deltaTickResidual;
   @Shadow
   private long lastMs;
   @Final
   @Shadow
   private float msPerTick;

   @Inject(method = "advanceGameTime(J)I", at = @At("HEAD"), cancellable = true)
   private void beginRenderTickHook(long timeMillis, CallbackInfoReturnable<Integer> cir) {
      float timerMultiplier = Vcore.TICK_TIMER * Vcore.FRAG_EFFECT_TIMER;
      if (timerMultiplier != 1.0F) {
         this.deltaTicks = (float)(timeMillis - this.lastMs) / this.msPerTick * timerMultiplier;
         this.lastMs = timeMillis;
         this.deltaTickResidual = this.deltaTickResidual + this.deltaTicks;
         int i = (int)this.deltaTickResidual;
         this.deltaTickResidual -= i;
         cir.setReturnValue(i);
      }
   }
}
