package vcore.features.modules.render.popeffect;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import vcore.core.manager.client.ModuleManager;

@Environment(EnvType.CLIENT)
public final class PopEffectReviveParticle extends SingleQuadParticle {
   private final SpriteSet spriteProvider;
   private final double scaler;
   private final double rotX;
   private final double rotZ;
   private double rotY;
   private LivingEntity target;
   private Quaternionf quaternion = new Quaternionf(0.0F, -0.7F, 0.7F, 0.0F);

   private PopEffectReviveParticle(
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
      this.lifetime = 20;
      this.alpha = 0.0F;
      this.quadSize = 0.2F;
      this.scaler = velocityX;
      this.friction = 0.0F;
      this.rotX = this.random.nextIntBetweenInclusive(-180, 180);
      this.rotY = this.random.nextIntBetweenInclusive(-180, 180);
      this.rotZ = this.random.nextIntBetweenInclusive(-180, 180);
      if (clientWorld.getEntity((int)velocityY) instanceof LivingEntity livingEntity) {
         this.target = livingEntity;
      }

      if (this.random.nextBoolean()) {
         this.setColor(1.0F, 1.0F, 0.0F);
      } else {
         this.setColor(0.0F, 1.0F, 0.0F);
      }

      this.spriteProvider = spriteProvider;
      this.setSprite(this.spriteProvider.get(this.age, this.lifetime));
   }

   @Override
   public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
      this.extractRotatedQuad(particleTypeRenderState, camera, this.quaternion, partialTickTime);
   }

   @Override
   protected SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSprite(this.spriteProvider.get(this.age, this.lifetime));
      if (this.target != null) {
         this.setPos(this.target.getX(), this.target.getY() + this.target.getDimensions(this.target.getPose()).height() / 2.0F, this.target.getZ());
      }

      float opacity = getEffectOpacity();
      if (!(opacity <= 0.0F)) {
         this.rotY += 20.0;
         this.quaternion = euler(0.0F, 0.0F, (float)this.rotY);
         this.quaternion = euler((float)this.rotZ, (float)this.rotX, (float)(-this.rotZ)).mul(this.quaternion);
         this.quadSize = this.alpha / opacity * (float)this.scaler;
         this.alpha = Mth.clamp((float)Math.sqrt(Math.sin((double)this.age / this.lifetime * Math.PI)) / 1.2F, 0.0F, 1.0F) * opacity;
      }
   }

   @NotNull
   private static Quaternionf euler(float x, float y, float z) {
      Quaternionf qx = new Quaternionf().fromAxisAngleDeg(new Vector3f(1.0F, 0.0F, 0.0F), x);
      Quaternionf qy = new Quaternionf().fromAxisAngleDeg(new Vector3f(0.0F, 1.0F, 0.0F), y);
      Quaternionf qz = new Quaternionf().fromAxisAngleDeg(new Vector3f(0.0F, 0.0F, 1.0F), z);
      return qx.mul(qy).mul(qz);
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
         return new PopEffectReviveParticle(world, x, y, z, this.spriteProvider, velocityX, velocityY, velocityZ);
      }
   }
}
