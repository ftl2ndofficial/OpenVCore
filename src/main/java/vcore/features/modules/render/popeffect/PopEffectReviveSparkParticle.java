package vcore.features.modules.render.popeffect;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vcore.core.manager.client.ModuleManager;

@Environment(EnvType.CLIENT)
public final class PopEffectReviveSparkParticle extends SingleQuadParticle {
   private LivingEntity target;
   private Vec3 targetDirection;

   private PopEffectReviveSparkParticle(
      @NotNull ClientLevel clientWorld,
      double x,
      double y,
      double z,
      @NotNull SpriteSet spriteProvider,
      double velocityX,
      double velocityY,
      double velocityZ
   ) {
      super(clientWorld, x, y, z, velocityX, velocityY, velocityZ, spriteProvider.first());
      this.lifetime = this.random.nextIntBetweenInclusive(30, 48);
      this.alpha = getEffectOpacity();
      this.quadSize = 0.1F;
      this.xd = this.random.nextIntBetweenInclusive(-10, 10) / 25.0F;
      this.yd = this.random.nextIntBetweenInclusive(-10, 10) / 25.0F;
      this.zd = this.random.nextIntBetweenInclusive(-10, 10) / 25.0F;
      this.friction = 1.0F;
      this.hasPhysics = true;
      this.setSpriteFromAge(spriteProvider);
      if (clientWorld.getEntity((int)velocityX) instanceof LivingEntity livingEntity) {
         this.target = livingEntity;
      }

      if (this.random.nextBoolean()) {
         this.setColor(0.0F, 1.0F, 0.0F);
      } else {
         this.setColor(1.0F, 1.0F, 0.0F);
      }
   }

   public void tick() {
      super.tick();
      if (this.target != null) {
         this.targetDirection = new Vec3(
               this.target.getX() - this.x,
               this.target.getY() + this.target.getDimensions(this.target.getPose()).height() / 2.0F - this.y,
               this.target.getZ() - this.z
            )
            .normalize();
      }

      if (this.age >= this.lifetime / 5.0F && this.age < this.lifetime / 4.0F) {
         this.friction = 0.0F;
      }

      if (this.target != null && this.age >= this.lifetime / 4.0F && this.target.position().distanceTo(new Vec3(this.x, this.y, this.z)) < 20.0) {
         this.alpha = Mth.clamp(1.0F - (this.age - this.lifetime / 1.5F) / 20.0F, 0.0F, 1.0F) * getEffectOpacity();
         this.friction = 1.0F;
         this.xd = this.targetDirection.x * ((this.age - 15) * 0.05F);
         this.yd = this.targetDirection.y * ((this.age - 15) * 0.05F);
         this.zd = this.targetDirection.z * ((this.age - 15) * 0.05F);
         if (this.target.position().distanceTo(new Vec3(this.x, this.y, this.z)) < 2.0) {
            this.alpha = 0.0F;
            this.removed = true;
         }
      }
   }

   @Override
   protected SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   private static float getEffectOpacity() {
      return ModuleManager.popEffect.getEffectOpacity();
   }

   @Environment(EnvType.CLIENT)
   public static final class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteProvider;

      public Factory(@NotNull SpriteSet spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      @Nullable
      public Particle createParticle(
         SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, net.minecraft.util.RandomSource random
      ) {
         return new PopEffectReviveSparkParticle(world, x, y, z, this.spriteProvider, velocityX, velocityY, velocityZ);
      }
   }
}
