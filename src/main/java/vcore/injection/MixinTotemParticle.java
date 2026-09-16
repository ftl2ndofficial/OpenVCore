package vcore.injection;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TotemParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(TotemParticle.class)
public abstract class MixinTotemParticle extends SimpleAnimatedParticle {
   protected MixinTotemParticle(ClientLevel world, double x, double y, double z, SpriteSet spriteProvider, float upwardsAcceleration) {
      super(world, x, y, z, spriteProvider, upwardsAcceleration);
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void injectPopEffectAlpha(
      ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet spriteProvider, CallbackInfo ci
   ) {
      if (ModuleManager.popEffect.shouldHideVanillaTotemParticle()) {
         this.alpha = 0.0F;
      }
   }
}
