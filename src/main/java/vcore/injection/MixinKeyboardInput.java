package vcore.injection;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.Vcore;
import vcore.events.impl.EventKeyboardInput;
import vcore.features.modules.Module;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
   @Unique
   private boolean vcore$clearMovementInput;

   @Inject(
      method = "tick",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/KeyboardInput;shiftKeyDown:Z", shift = Shift.BEFORE),
      cancellable = true
   )
   private void onSneak(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventKeyboardInput event = new EventKeyboardInput((Input)(Object)this);
         Vcore.EVENT_BUS.post(event);
         this.vcore$clearMovementInput = event.shouldClearMovementInput();
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "tick", at = @At("RETURN"))
   private void onTickReturn(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
      if (this.vcore$clearMovementInput) {
         EventKeyboardInput.clearMovementInput((Input)(Object)this);
         this.vcore$clearMovementInput = false;
      }
   }
}
