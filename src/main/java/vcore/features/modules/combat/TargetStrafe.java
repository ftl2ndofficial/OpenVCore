package vcore.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventMove;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.player.MovementUtility;

public class TargetStrafe extends Module {
   public static boolean strafesCheck;
   private final Setting<TargetStrafe.Mode> mode = new Setting<>("Mode", TargetStrafe.Mode.Collision);
   public final Setting<Boolean> jump = new Setting<>("Jump", true, v -> this.mode.is(TargetStrafe.Mode.Default));
   private final Setting<Float> defaultDistance = new Setting<>("Distance", 2.4F, 0.1F, 6.0F, v -> this.mode.is(TargetStrafe.Mode.Default));
   private final Setting<Float> defaultSpeed = new Setting<>("Speed", 0.4F, 0.05F, 1.0F, v -> this.mode.is(TargetStrafe.Mode.Default));
   private final Setting<Boolean> damageBoost = new Setting<>("DamageBoost", false, v -> this.mode.is(TargetStrafe.Mode.Default));
   private final Setting<Float> boostSpeed = new Setting<>(
      "BoostSpeed", 0.5F, 0.1F, 1.5F, v -> this.mode.is(TargetStrafe.Mode.Default) && this.damageBoost.getValue()
   );
   private final Setting<Float> collisionSpeed = new Setting<>("Speed", 0.08F, 0.01F, 0.1F, v -> this.mode.is(TargetStrafe.Mode.Collision));
   private final Setting<Float> collisionDistance = new Setting<>("Distance", 1.2F, 1.0F, 6.0F, v -> this.mode.is(TargetStrafe.Mode.Collision));
   private boolean clockwise = true;
   private boolean hasQueuedMotion;
   private double queuedMotionX;
   private double queuedMotionZ;
   private int jumpTicks;

   public TargetStrafe() {
      super("TargetStrafe", "Strafes around target.", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.hasQueuedMotion = false;
      strafesCheck = false;
   }

   @EventHandler
   public void onSync(EventSync event) {
      if (event != null) {
         if (fullNullCheck()) {
            this.hasQueuedMotion = false;
            strafesCheck = false;
         } else {
            LocalPlayer player = mc.player;
            ClientLevel world = mc.level;
            if (player != null && world != null) {
               this.jumpTicks = Math.max(0, this.jumpTicks - 1);
               this.hasQueuedMotion = false;
               strafesCheck = false;
               if (this.mode.is(TargetStrafe.Mode.Collision)) {
                  this.handleCollisionMode(player);
               } else {
                  this.handleDefaultMode(player, world);
               }
            } else {
               this.hasQueuedMotion = false;
               strafesCheck = false;
            }
         }
      }
   }

   @EventHandler
   public void onMove(EventMove event) {
      if (!this.mode.not(TargetStrafe.Mode.Default) && this.hasQueuedMotion) {
         event.setX(this.queuedMotionX);
         event.setZ(this.queuedMotionZ);
         event.cancel();
         this.hasQueuedMotion = false;
      }
   }

   private void handleCollisionMode(LocalPlayer player) {
      if (MovementUtility.isMoving()) {
         if (Aura.target instanceof LivingEntity target) {
            if (!(player.distanceTo(target) >= this.collisionDistance.getValue())) {
               Vec3 direction = target.position().subtract(player.position()).normalize();
               double speed = this.collisionSpeed.getValue().floatValue();
               player.setDeltaMovement(player.getDeltaMovement().x + direction.x * speed, player.getDeltaMovement().y, player.getDeltaMovement().z + direction.z * speed);
            }
         }
      }
   }

   private void handleDefaultMode(LocalPlayer player, ClientLevel world) {
      if (Aura.target instanceof LivingEntity target && target.isAlive()) {
         if (this.jump.getValue() && player.onGround()) {
            mc.options.keyJump.setDown(false);
            player.jumpFromGround();
         }

         float maxDistance = ModuleManager.aura.attackRange.getValue() + ModuleManager.aura.aimRange.getValue();
         double distanceToTarget = player.distanceTo(target);
         if (!(distanceToTarget > maxDistance)) {
            mc.options.keyUp.setDown(false);
            float speed = this.defaultSpeed.getValue();
            if (this.damageBoost.getValue() && player.hurtTime > 0 && player.isAlive()) {
               speed += this.boostSpeed.getValue();
            }

            float clampDist = (float)Mth.clamp(distanceToTarget, 0.01F, maxDistance);
            double baseAngle = Math.atan2(player.getZ() - target.getZ(), player.getX() - target.getX());
            float angleStep = Mth.clamp(speed / clampDist, 0.01F, 1.0F);
            double orbitAngle = baseAngle + (this.clockwise ? angleStep : -angleStep);
            double orbitX = target.getX() + this.defaultDistance.getValue().floatValue() * Math.cos(orbitAngle);
            double orbitZ = target.getZ() + this.defaultDistance.getValue().floatValue() * Math.sin(orbitAngle);
            if (this.isUnsafeAreaAround(orbitX, orbitZ, player, world)) {
               this.clockwise = !this.clockwise;
               orbitAngle = baseAngle + (this.clockwise ? angleStep : -angleStep);
               orbitX = target.getX() + this.defaultDistance.getValue().floatValue() * Math.cos(orbitAngle);
               orbitZ = target.getZ() + this.defaultDistance.getValue().floatValue() * Math.sin(orbitAngle);
            }

            strafesCheck = true;
            double angleTo = Math.toRadians(this.calculateAngleToTarget(orbitX, orbitZ, player));
            this.queuedMotionX = speed * -Math.sin(angleTo);
            this.queuedMotionZ = speed * Math.cos(angleTo);
            this.hasQueuedMotion = !Double.isNaN(this.queuedMotionX) && !Double.isNaN(this.queuedMotionZ);
         }
      }
   }

   private boolean isUnsafeAreaAround(double x, double z, LocalPlayer player, ClientLevel world) {
      if (!player.horizontalCollision && (!mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown() || this.jumpTicks > 0)) {
         MutableBlockPos mutable = new MutableBlockPos();
         int top = (int)(player.getY() + 4.0);

         for (int y = top; y >= world.getMinY(); y--) {
            mutable.set(x, y, z);
            BlockState state = world.getBlockState(mutable);
            Block block = state.getBlock();
            if (block == Blocks.LAVA || block == Blocks.FIRE || block == Blocks.COBWEB) {
               return true;
            }

            if (!state.isAir()) {
               return false;
            }
         }

         return this.isVoidAboveVoid(x, z, player, world);
      } else {
         this.jumpTicks = 10;
         return true;
      }
   }

   private boolean isVoidAboveVoid(double x, double z, LocalPlayer player, ClientLevel world) {
      MutableBlockPos mutable = new MutableBlockPos();

      for (int y = (int)player.getY(); y > world.getMinY(); y--) {
         mutable.set(x, y, z);
         if (!world.getBlockState(mutable).isAir()) {
            return false;
         }
      }

      return true;
   }

   private float calculateAngleToTarget(double x, double z, LocalPlayer player) {
      double diffX = x - player.getX();
      double diffZ = z - player.getZ();
      return (float)(Math.atan2(diffZ, diffX) * 180.0 / Math.PI - 90.0);
   }

   private enum Mode {
      Default,
      Collision;
   }
}
