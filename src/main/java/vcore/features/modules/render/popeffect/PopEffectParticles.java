package vcore.features.modules.render.popeffect;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class PopEffectParticles {
   public static final SimpleParticleType REVIVE = FabricParticleTypes.simple();
   public static final SimpleParticleType REVIVE_SPARK = FabricParticleTypes.simple();

   private PopEffectParticles() {
   }

   public static void register() {
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("vcore", "revive"), REVIVE);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("vcore", "revive_spark"), REVIVE_SPARK);
      ParticleFactoryRegistry.getInstance().register(REVIVE, PopEffectReviveParticle.Factory::new);
      ParticleFactoryRegistry.getInstance().register(REVIVE_SPARK, PopEffectReviveSparkParticle.Factory::new);
   }
}
