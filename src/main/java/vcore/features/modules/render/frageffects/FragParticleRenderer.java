package vcore.features.modules.render.frageffects;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import vcore.features.modules.Module;
import vcore.utility.Timer;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.RenderSystem;

public final class FragParticleRenderer {
   private final List<FragParticleRenderer.FragParticle> particles = new CopyOnWriteArrayList<>();
   private final Random random = new Random();

   private FragParticleRenderer() {
   }

   public static FragParticleRenderer create() {
      return new FragParticleRenderer();
   }

   public void spawnParticlesVoid(Vec3 mainPos, int lifeTimeMin, int lifeTimeMax, float maxFlight, int count, float gravity) {
      float maxXZMotion = maxFlight / (lifeTimeMin / 50.0F) * 1.12F;
      float maxYMotion = maxFlight / (lifeTimeMin / 50.0F) * 0.31F;

      for (int i = 0; i < count; i++) {
         this.particles
            .add(
               new FragParticleRenderer.FragParticle(
                  mainPos, FragEffectMath.lerp(lifeTimeMin, lifeTimeMax, this.random.nextFloat()), maxXZMotion, maxYMotion, gravity
               )
            );
      }
   }

   public void updateParticlesList() {
      if (!this.particles.isEmpty()) {
         this.particles.removeIf(FragParticleRenderer.FragParticle::removeIf);

         for (FragParticleRenderer.FragParticle particle : this.particles) {
            particle.updateMotion();
         }
      }
   }

   public void renderParticles(PoseStack matrices) {
      if (!this.particles.isEmpty() && Module.mc.gameRenderer != null) {
         List<FragParticleRenderer.ColoredPoint>[] pointsBySize = this.createPointBuckets(160);
         float tickDelta = Render3DEngine.getTickDelta();
         Vec3 cameraPos = Module.mc.gameRenderer.getMainCamera().position();

         for (FragParticleRenderer.FragParticle particle : this.particles) {
            Vec3 renderPos = particle.getRenderPos(tickDelta).subtract(cameraPos);
            pointsBySize[particle.getScaledBeginInt(160)]
               .add(new FragParticleRenderer.ColoredPoint(renderPos.x, renderPos.y, renderPos.z, particle.getColor()));
         }

         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         Matrix4f matrix = matrices.last().pose();
         float pointScale = 0.25F;

         for (List<FragParticleRenderer.ColoredPoint> points : pointsBySize) {
            if (!points.isEmpty()) {
               BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
               float radius = pointScale * 0.003F;

               for (FragParticleRenderer.ColoredPoint point : points) {
                  this.putPointQuad(buffer, matrix, point, radius);
               }

               BufferUploader.drawWithShader(buffer.buildOrThrow());
            }

            pointScale += 0.25F;
         }

         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         BufferUploader.invalidateCommandEncoder();
         try {
            org.lwjgl.opengl.GL33.glBindSampler(0, 0);
         } catch (Throwable ignored) {}
      }
   }

   public void clear() {
      this.particles.clear();
   }

   private List<FragParticleRenderer.ColoredPoint>[] createPointBuckets(int count) {
      List<FragParticleRenderer.ColoredPoint>[] buckets = new ArrayList[count];

      for (int i = 0; i < buckets.length; i++) {
         buckets[i] = new ArrayList<>();
      }

      return buckets;
   }

   private void putPointQuad(BufferBuilder buffer, Matrix4f matrix, FragParticleRenderer.ColoredPoint point, float radius) {
      float x = (float)point.x;
      float y = (float)point.y;
      float z = (float)point.z;
      buffer.addVertex(matrix, x - radius, y + radius, z).setColor(point.color);
      buffer.addVertex(matrix, x + radius, y + radius, z).setColor(point.color);
      buffer.addVertex(matrix, x + radius, y - radius, z).setColor(point.color);
      buffer.addVertex(matrix, x - radius, y - radius, z).setColor(point.color);
   }

   private record ColoredPoint(double x, double y, double z, int color) {
   }

   private static final class FragParticle {
      private static final Random RANDOM = new Random();
      private Vec3 pos;
      private Vec3 prevPos;
      private Vec3 motion;
      private final int maxTime;
      private final float gravity;
      private final Timer stopWatch = new Timer();
      private final int color = Color.HSBtoRGB(0.0F, 0.0F, (float)FragEffectMath.easeExpoInOut(RANDOM.nextFloat()));

      private FragParticle(Vec3 spawnPos, int maxTime, float motionXZSpeedMax, float motionYSpeedMax, float gravity) {
         this.pos = spawnPos;
         this.prevPos = spawnPos;
         this.maxTime = maxTime;
         this.motion = new Vec3(
            FragEffectMath.lerp(-motionXZSpeedMax, motionXZSpeedMax, RANDOM.nextFloat()),
            FragEffectMath.lerp(-motionYSpeedMax / 4.0F, motionYSpeedMax, RANDOM.nextFloat()),
            FragEffectMath.lerp(-motionXZSpeedMax, motionXZSpeedMax, RANDOM.nextFloat())
         );
         this.gravity = gravity;
      }

      private void updateMotion() {
         this.prevPos = this.pos;
         this.pos = this.pos.add(this.motion);
         this.motion = new Vec3(this.motion.x * 0.99F, (this.motion.y - this.gravity) * 0.994F, this.motion.z * 0.99F);
      }

      private Vec3 getRenderPos(float partialTicks) {
         return this.prevPos
            .add((this.pos.x - this.prevPos.x) * partialTicks, (this.pos.y - this.prevPos.y) * partialTicks, (this.pos.z - this.prevPos.z) * partialTicks);
      }

      private boolean removeIf() {
         return this.stopWatch.passedMs(this.maxTime);
      }

      private int getColor() {
         return FragEffectMath.multAlpha(this.color, 1.0F - Math.min((float)this.stopWatch.getPassedTimeMs() / this.maxTime, 1.0F));
      }

      private int getScaledBeginInt(int maxInt) {
         return Math.floorMod(this.hashCode(), maxInt);
      }
   }
}
