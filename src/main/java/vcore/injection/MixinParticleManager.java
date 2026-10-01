package vcore.injection;

import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.FireworkParticles.OverlayParticle;
import net.minecraft.client.particle.FireworkParticles.Starter;
import net.minecraft.client.particle.ElderGuardianParticle;
import net.minecraft.client.particle.HugeExplosionParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.NoRender;

@Mixin(ParticleEngine.class)
public class MixinParticleManager {
   @Inject(at = @At("HEAD"), method = "add(Lnet/minecraft/client/particle/Particle;)V", cancellable = true)
   public void addParticleHook(Particle p, CallbackInfo e) {
      NoRender nR = ModuleManager.noRender;
      if (nR.isEnabled()) {
         if (nR.noWeather.getValue()) {
            String particleName = p.getClass().getSimpleName();
            if (particleName.contains("Rain") || particleName.contains("Splash")) {
               e.cancel();
            }
         }

         if (nR.elderGuardian.getValue() && p instanceof ElderGuardianParticle) {
            e.cancel();
         }

         if (nR.explosions.getValue() && p instanceof HugeExplosionParticle) {
            e.cancel();
         }

         if (nR.campFire.getValue() && p instanceof CampfireSmokeParticle) {
            e.cancel();
         }

         if (nR.breakParticles.getValue() && p instanceof TerrainParticle) {
            e.cancel();
         }

         if (nR.fireworks.getValue() && (p instanceof Starter || p instanceof OverlayParticle)) {
            e.cancel();
         }
      }
   }
}
