package vcore.features.modules.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventSync;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.math.MathUtility;
import vcore.utility.math.PredictUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public final class AimBot extends Module {
   private final Setting<AimBot.Mode> mode = new Setting<>("Mode", AimBot.Mode.BowAim);
   private final Setting<AimBot.Rotation> rotation = new Setting<>("Rotation", AimBot.Rotation.Silent, v -> this.mode.getValue() != AimBot.Mode.AimAssist);
   private final Setting<Float> aimRange = new Setting<>("Range", 20.0F, 1.0F, 30.0F, v -> this.mode.getValue() != AimBot.Mode.AimAssist);
   private final Setting<Integer> aimStrength = new Setting<>("AimStrength", 30, 1, 100, v -> this.mode.getValue() == AimBot.Mode.AimAssist);
   private final Setting<Integer> aimSmooth = new Setting<>("AimSmooth", 45, 1, 180, v -> this.mode.getValue() == AimBot.Mode.AimAssist);
   private final Setting<Integer> aimtime = new Setting<>("AimTime", 2, 1, 10, v -> this.mode.getValue() == AimBot.Mode.AimAssist);
   private final Setting<Boolean> ignoreWalls = new Setting<>(
      "IgnoreWalls", true, v -> this.mode.getValue() == AimBot.Mode.CSAim || this.mode.is(AimBot.Mode.AimAssist)
   );
   private final Setting<Boolean> ignoreTeam = new Setting<>(
      "IgnoreTeam", true, v -> this.mode.getValue() == AimBot.Mode.CSAim || this.mode.is(AimBot.Mode.AimAssist)
   );
   private final Setting<Integer> reactionTime = new Setting<>(
      "ReactionTime", 80, 1, 500, v -> this.mode.getValue() == AimBot.Mode.AimAssist && !this.ignoreWalls.getValue()
   );
   private final Setting<Boolean> ignoreInvisible = new Setting<>("IgnoreInvis", false, v -> this.mode.is(AimBot.Mode.AimAssist));
   private final Setting<Float> rotYawRandom = new Setting<>("YawRandom", 0.0F, 0.0F, 3.0F, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private final Setting<Float> rotPitchRandom = new Setting<>("PitchRandom", 0.0F, 0.0F, 3.0F, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private final Setting<Float> predict = new Setting<>("AimPredict", 0.5F, 0.5F, 8.0F, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private final Setting<Integer> delay = new Setting<>("Shoot delay", 5, 0, 10, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private final Setting<Integer> fov = new Setting<>("FOV", 65, 10, 360, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private final Setting<Integer> predictTicks = new Setting<>("PredictTicks", 2, 0, 20, v -> this.mode.getValue() == AimBot.Mode.BowAim);
   private final Setting<AimBot.Bone> part = new Setting<>("Bone", AimBot.Bone.Head, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private Entity target;
   private float rotationYaw;
   private float rotationPitch;
   private float assistAcceleration;
   private int aimTicks = 0;
   private Timer visibleTime = new Timer();

   public AimBot() {
      super("AimBot", "Smooth legit aiming.", Module.Category.COMBAT);
   }

   @EventHandler
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (this.mode.getValue() == AimBot.Mode.BowAim) {
         if (!(mc.player.getUseItem().getItem() instanceof BowItem)) {
            return;
         }

         Player nearestTarget = Managers.COMBAT.getTargetByFOV(128.0F);
         if (nearestTarget == null) {
            return;
         }

         float currentDuration = (mc.player.getUseItem().getUseDuration(mc.player) - mc.player.getTicksUsingItem()) / 20.0F;
         currentDuration = (currentDuration * currentDuration + currentDuration * 2.0F) / 3.0F;
         if (currentDuration >= 1.0F) {
            currentDuration = 1.0F;
         }

         float pitch = (float)(-Math.toDegrees(this.calculateArc(nearestTarget, currentDuration * 3.0F)));
         if (Float.isNaN(pitch)) {
            return;
         }

         Player predictedEntity = PredictUtility.predictPlayer(nearestTarget, this.predictTicks.getValue());
         double iX = predictedEntity.getX() - predictedEntity.xo;
         double iZ = predictedEntity.getZ() - predictedEntity.zo;
         double distance = mc.player.distanceTo(predictedEntity);
         distance -= distance % 2.0;
         iX = distance / 2.0 * iX * (mc.player.isSprinting() ? 1.3 : 1.1);
         iZ = distance / 2.0 * iZ * (mc.player.isSprinting() ? 1.3 : 1.1);
         this.rotationYaw = (float)Math.toDegrees(Math.atan2(predictedEntity.getZ() + iZ - mc.player.getZ(), predictedEntity.getX() + iX - mc.player.getX()))
            - 90.0F;
         this.rotationPitch = pitch;
      } else if (this.mode.getValue() == AimBot.Mode.CSAim) {
         this.calcThread();
      } else {
         if (mc.hitResult.getType() == Type.ENTITY) {
            this.aimTicks++;
         } else {
            this.aimTicks = 0;
         }

         if (this.aimTicks >= this.aimtime.getValue()) {
            this.assistAcceleration = 0.0F;
            return;
         }

         Player nearestTarget = Managers.COMBAT.getNearestTarget(5.0F);
         this.assistAcceleration = this.assistAcceleration + this.aimStrength.getValue().intValue() / 10000.0F;
         if (nearestTarget != null) {
            if (!mc.player.hasLineOfSight(nearestTarget) && !this.ignoreWalls.getValue()) {
               this.visibleTime.reset();
            }

            if (!this.visibleTime.passedMs(this.reactionTime.getValue().intValue())) {
               this.rotationYaw = Float.NaN;
               return;
            }

            if (Float.isNaN(this.rotationYaw)) {
               this.rotationYaw = mc.player.getYRot();
            }

            float delta_yaw = Mth.wrapDegrees(
               (float)Mth.wrapDegrees(
                     Math.toDegrees(Math.atan2(nearestTarget.getEyePosition().z - mc.player.getZ(), nearestTarget.getEyePosition().x - mc.player.getX())) - 90.0
                  )
                  - this.rotationYaw
            );
            if (delta_yaw > 180.0F) {
               delta_yaw -= 180.0F;
            }

            float deltaYaw = Mth.clamp(Mth.abs(delta_yaw), -this.aimSmooth.getValue(), this.aimSmooth.getValue().intValue());
            float newYaw = this.rotationYaw + (delta_yaw > 0.0F ? deltaYaw : -deltaYaw);
            double gcdFix = Math.pow((Double)mc.options.sensitivity().get() * 0.6 + 0.2, 3.0) * 1.2;
            this.rotationYaw = (float)(newYaw - (newYaw - this.rotationYaw) % gcdFix);
         } else {
            this.rotationYaw = Float.NaN;
         }
      }

      if (!Float.isNaN(this.rotationYaw)) {
         ModuleManager.moveFix.fixRotation = this.rotationYaw;
      }
   }

   @EventHandler
   public void onSync(EventSync event) {
      if (!this.mode.is(AimBot.Mode.AimAssist)) {
         if (this.mode.is(AimBot.Mode.CSAim)) {
            if (this.target != null && (mc.player.hasLineOfSight(this.target) || this.ignoreWalls.getValue())) {
               if (mc.player.tickCount % this.delay.getValue() == 0) {
                  event.addPostAction(
                     () -> this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()))
                  );
               }
            } else {
               this.rotationYaw = mc.player.getYRot();
               this.rotationPitch = mc.player.getXRot();
            }
         }

         if ((this.target != null || this.mode.getValue() == AimBot.Mode.BowAim && mc.player.getUseItem().getItem() instanceof BowItem)
            && this.rotation.getValue() == AimBot.Rotation.Silent) {
            mc.player.setYRot(this.rotationYaw);
            mc.player.setXRot(this.rotationPitch);
         }
      }
   }

   @Override
   public void onEnable() {
      this.target = null;
      this.rotationYaw = mc.player.getYRot();
      this.rotationPitch = mc.player.getXRot();
   }

   @Override
   public void onRender3D(PoseStack stack) {
      if (this.mode.getValue() == AimBot.Mode.AimAssist) {
         if (!Float.isNaN(this.rotationYaw)) {
            mc.player.setYRot((float)Render2DEngine.interpolate(mc.player.getYRot(), this.rotationYaw, this.assistAcceleration));
         }
      } else {
         if (this.target != null && (mc.player.hasLineOfSight(this.target) || this.ignoreWalls.getValue())) {
            if (this.rotation.getValue() == AimBot.Rotation.Client) {
               mc.player.setYRot((float)Render2DEngine.interpolate(mc.player.yRotO, this.rotationYaw, Render3DEngine.getTickDelta()));
               mc.player.setXRot((float)Render2DEngine.interpolate(mc.player.xRotO, this.rotationPitch, Render3DEngine.getTickDelta()));
            }
         } else if (this.mode.getValue() == AimBot.Mode.CSAim) {
            this.rotationYaw = mc.player.getYRot();
            this.rotationPitch = mc.player.getXRot();
         }

         if (this.rotation.getValue() == AimBot.Rotation.Client
            && this.mode.getValue() == AimBot.Mode.BowAim
            && mc.player.getUseItem().getItem() instanceof BowItem) {
            mc.player.setYRot((float)Render2DEngine.interpolate(mc.player.yRotO, this.rotationYaw, Render3DEngine.getTickDelta()));
            mc.player.setXRot((float)Render2DEngine.interpolate(mc.player.xRotO, this.rotationPitch, Render3DEngine.getTickDelta()));
         }
      }
   }

   private float calculateArc(@NotNull Player target, double duration) {
      double yArc = target.getY() + target.getEyeHeight(target.getPose()) - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
      double dX = target.getX() - mc.player.getX();
      double dZ = target.getZ() - mc.player.getZ();
      double dirRoot = Math.sqrt(dX * dX + dZ * dZ);
      return this.calculateArc(duration, dirRoot, yArc);
   }

   private float calculateArc(double d, double dr, double y) {
      y = 2.0 * y * (d * d);
      y = 0.05F * (0.05F * (dr * dr) + y);
      y = Math.sqrt(d * d * d * d - y);
      d = d * d - y;
      y = Math.atan2(d * d + y, 0.05F * dr);
      d = Math.atan2(d, 0.05F * dr);
      return (float)Math.min(y, d);
   }

   private void calcThread() {
      if (this.target == null) {
         this.findTarget();
      } else if (this.skipEntity(this.target)) {
         this.target = null;
      } else {
         Vec3 targetVec = this.getResolvedPos(this.target).add(0.0, this.part.getValue().getH(), 0.0);
         if (targetVec != null) {
            float delta_yaw = Mth.wrapDegrees(
               (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(targetVec.z - mc.player.getZ(), targetVec.x - mc.player.getX())) - 90.0)
                  - this.rotationYaw
            );
            float delta_pitch = (float)(
                  -Math.toDegrees(
                     Math.atan2(
                        targetVec.y - (mc.player.position().y + mc.player.getEyeHeight(mc.player.getPose())),
                        Math.sqrt(Math.pow(targetVec.x - mc.player.getX(), 2.0) + Math.pow(targetVec.z - mc.player.getZ(), 2.0))
                     )
                  )
               )
               - this.rotationPitch;
            if (delta_yaw > 180.0F) {
               delta_yaw -= 180.0F;
            }

            float deltaYaw = Mth.clamp(Mth.abs(delta_yaw), MathUtility.random(-40.0F, -60.0F), MathUtility.random(40.0F, 60.0F));
            float newYaw = this.rotationYaw
               + (delta_yaw > 0.0F ? deltaYaw : -deltaYaw)
               + MathUtility.random(-this.rotYawRandom.getValue(), this.rotYawRandom.getValue());
            float newPitch = Mth.clamp(
                  this.rotationPitch + Mth.clamp(delta_pitch, MathUtility.random(-10.0F, -20.0F), MathUtility.random(10.0F, 20.0F)), -90.0F, 90.0F
               )
               + MathUtility.random(-this.rotPitchRandom.getValue(), this.rotPitchRandom.getValue());
            double gcdFix = Math.pow((Double)mc.options.sensitivity().get() * 0.6 + 0.2, 3.0) * 8.0 * 0.15F;
            this.rotationYaw = (float)(newYaw - (newYaw - this.rotationYaw) % gcdFix);
            this.rotationPitch = (float)(newPitch - (newPitch - this.rotationPitch) % gcdFix);
         }
      }
   }

   public void findTarget() {
      List<Entity> first_stage = new CopyOnWriteArrayList<>();

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (!this.skipEntity(entity)) {
            first_stage.add(entity);
         }
      }

      float best_fov = this.fov.getValue().intValue();
      Entity best_entity = null;

      for (Entity ent : first_stage) {
         float temp_fov = Math.abs(
            (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(ent.getZ() - mc.player.getZ(), ent.getX() - mc.player.getX())) - 90.0)
               - Mth.wrapDegrees(mc.player.getYRot())
         );
         if (temp_fov < best_fov) {
            best_entity = ent;
            best_fov = temp_fov;
         }
      }

      this.target = best_entity;
   }

   private boolean skipEntity(Entity entity) {
      if (entity instanceof LivingEntity ent) {
         if (ent.isDeadOrDying()) {
            return true;
         }

         if (!entity.isAlive()) {
            return true;
         }

         if (entity instanceof ArmorStand) {
            return true;
         }

         if (ModuleManager.antiBot.isEnabled() && ModuleManager.antiBot.mode.getValue() == AntiBot.Mode.Matrix && AntiBot.isBot(entity)) {
            return true;
         }

         if (entity instanceof Player pl) {
            if (entity == mc.player) {
               return true;
            } else if (entity.isInvisible() && this.ignoreInvisible.getValue()) {
               return true;
            } else if (Managers.FRIEND.isFriend(pl)) {
               return true;
            } else if (Math.abs(this.getYawToEntityNew(entity)) > this.fov.getValue().intValue()) {
               return true;
            } else {
               return pl.getTeamColor() == mc.player.getTeamColor() && this.ignoreTeam.getValue() && mc.player.getTeamColor() != 16777215
                  ? true
                  : mc.player.distanceToSqr(this.getResolvedPos(entity)) > this.aimRange.getPow2Value();
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public float getYawToEntityNew(@NotNull Entity entity) {
      return this.getYawBetween(mc.player.getYRot(), mc.player.getX(), mc.player.getZ(), entity.getX(), entity.getZ());
   }

   public float getYawBetween(float yaw, double srcX, double srcZ, double destX, double destZ) {
      double xDist = destX - srcX;
      double zDist = destZ - srcZ;
      float yaw1 = (float)(StrictMath.atan2(zDist, xDist) * 180.0 / Math.PI) - 90.0F;
      return yaw + Mth.wrapDegrees(yaw1 - yaw);
   }

   private Vec3 getResolvedPos(@NotNull Entity pl) {
      return new Vec3(
         pl.getX() + (pl.getX() - pl.xo) * this.predict.getValue().floatValue(),
         pl.getY(),
         pl.getZ() + (pl.getZ() - pl.zo) * this.predict.getValue().floatValue()
      );
   }

   private enum Bone {
      Head(1.7F),
      Neck(1.5F),
      Torso(1.0F),
      Tights(0.8F),
      Feet(0.25F);

      private final float h;

      Bone(float h) {
         this.h = h;
      }

      public float getH() {
         return this.h;
      }
   }

   private enum Mode {
      CSAim,
      AimAssist,
      BowAim;
   }

   private enum Rotation {
      Client,
      Silent;
   }
}
