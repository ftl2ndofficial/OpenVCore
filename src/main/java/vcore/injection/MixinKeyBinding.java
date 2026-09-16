package vcore.injection;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.InputBlocker;

@Mixin(KeyMapping.class)
public abstract class MixinKeyBinding {
   @Shadow
   public abstract boolean same(KeyMapping var1);

   @Shadow
   public abstract boolean isDown();

   @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
   private void pressHook(CallbackInfoReturnable<Boolean> cir) {
      try {
         KeyMapping self = (KeyMapping)(Object)this;
         Minecraft mc = Minecraft.getInstance();
         if (mc == null) {
            return;
         }

         if (InputBlocker.isBlocked()
            && (
               self.same(mc.options.keyUp)
                  || self.same(mc.options.keyDown)
                  || self.same(mc.options.keyLeft)
                  || self.same(mc.options.keyRight)
                  || self.same(mc.options.keyJump)
                  || self.same(mc.options.keyShift)
                  || self.same(mc.options.keySprint)
            )) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
         }

         if (InputBlocker.isUseBlocked() && self.same(mc.options.keyUse)) {
            cir.setReturnValue(false);
            cir.cancel();
         }
      } catch (Throwable var4) {
      }
   }
}
