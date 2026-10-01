package vcore.core.manager.player;

import java.util.ArrayDeque;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.IManager;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventFixVelocity;
import vcore.events.impl.EventKeyboardInput;
import vcore.events.impl.EventPlayerJump;
import vcore.events.impl.EventPlayerTravel;
import vcore.events.impl.EventPostSync;
import vcore.events.impl.EventSync;
import vcore.events.impl.EventTick;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.injection.accesors.IClientPlayerEntity;
import vcore.utility.Timer;
import vcore.utility.math.MathUtility;
import vcore.utility.world.ExplosionUtility;

public class PlayerManager implements IManager {
   public float yaw;
   public float pitch;
   public float lastYaw;
   public float lastPitch;
   public float currentPlayerSpeed;
   public float averagePlayerSpeed;
   public int ticksElytraFlying;
   public int serverSideSlot;
   public final Timer switchTimer = new Timer();
   private final ArrayDeque<Float> speedResult = new ArrayDeque<>(20);
   public float bodyYaw;
   public float prevBodyYaw;
   public boolean inInventory;

   @EventHandler(priority = 200)
   public void onSync(EventSync event) {
      if (!Module.fullNullCheck()) {
         this.yaw = mc.player.getYRot();
         this.pitch = mc.player.getXRot();
         this.lastYaw = ((IClientPlayerEntity)mc.player).getLastYaw();
         this.lastPitch = ((IClientPlayerEntity)mc.player).getLastPitch();
         if (mc.screen == null) {
            this.inInventory = false;
         }

         if (mc.player.isFallFlying() && mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            this.ticksElytraFlying++;
         } else {
            this.ticksElytraFlying = 0;
         }
      }
   }

   @EventHandler
   public void onTick(EventTick e) {
      this.currentPlayerSpeed = (float)Math.hypot(mc.player.getX() - mc.player.xo, mc.player.getZ() - mc.player.zo);
      if (this.speedResult.size() > 20) {
         this.speedResult.poll();
      }

      this.speedResult.add(this.currentPlayerSpeed);
      float average = 0.0F;

      for (Float value : this.speedResult) {
         average += MathUtility.clamp(value, 0.0F, 20.0F);
      }

      this.averagePlayerSpeed = average / this.speedResult.size();
   }

   @EventHandler(priority = -200)
   public void postSync(EventPostSync event) {
      if (mc.player != null) {
         this.prevBodyYaw = this.bodyYaw;
         this.bodyYaw = this.getBodyYaw();
         mc.player.setYRot(this.yaw);
         mc.player.setXRot(this.pitch);
         ModuleManager.moveFix.fixRotation = Float.NaN;
      }
   }

   @EventHandler
   public void onJump(EventPlayerJump e) {
      ModuleManager.moveFix.onJump(e);
   }

   @EventHandler
   public void onPlayerMove(EventFixVelocity e) {
      ModuleManager.moveFix.onPlayerMove(e);
   }

   @EventHandler
   public void modifyVelocity(EventPlayerTravel e) {
      ModuleManager.moveFix.modifyVelocity(e);
   }

   @EventHandler
   public void onKeyInput(EventKeyboardInput e) {
      ModuleManager.moveFix.onKeyInput(e);
   }

   @EventHandler
   public void onSyncWithServer(PacketEvent.@NotNull Send event) {
      if (event.getPacket() instanceof ServerboundContainerClickPacket) {
         this.inInventory = true;
      }

      if (event.getPacket() instanceof ServerboundSetCarriedItemPacket slot) {
         this.switchTimer.reset();
         this.serverSideSlot = slot.getSlot();
      }

      if (event.getPacket() instanceof ServerboundContainerClosePacket) {
         this.inInventory = false;
      }
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.@NotNull Receive event) {
      if (event.getPacket() instanceof ClientboundSetHeldSlotPacket slot) {
         this.switchTimer.reset();
         this.serverSideSlot = slot.slot();
      }
   }

   private float getBodyYaw() {
      double x = mc.player.getX() - mc.player.xo;
      double z = mc.player.getZ() - mc.player.zo;
      float offset = this.bodyYaw;
      if (x * x + z * z > 0.0025000002F) {
         offset = (float)(Mth.atan2(z, x) * 180.0F / (float)Math.PI - 90.0);
      }

      if (mc.player.attackAnim > 0.0F) {
         offset = ((IClientPlayerEntity)Minecraft.getInstance().player).getLastYaw();
      }

      float deltaBodyYaw = Mth.clamp(
         Mth.wrapDegrees(
            ((IClientPlayerEntity)Minecraft.getInstance().player).getLastYaw() - (this.bodyYaw + Mth.wrapDegrees(offset - this.bodyYaw) * 0.3F)
         ),
         -45.0F,
         75.0F
      );
      return (deltaBodyYaw > 50.0F ? deltaBodyYaw * 0.2F : 0.0F) + ((IClientPlayerEntity)Minecraft.getInstance().player).getLastYaw() - deltaBodyYaw;
   }

   public boolean checkRtx(float yaw, float pitch, float distance, float wallDistance, boolean rayTraceEnabled) {
      if (!rayTraceEnabled) {
         return true;
      }

      HitResult result = this.rayTrace(distance, yaw, pitch);
      Vec3 startPoint = mc.player.position().add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
      double distancePow2 = Math.pow(distance, 2.0);
      if (result != null) {
         distancePow2 = startPoint.distanceToSqr(result.getLocation());
      }

      Vec3 rotationVector = this.getRotationVector(pitch, yaw).scale(distance);
      Vec3 endPoint = startPoint.add(rotationVector);
      AABB entityArea = mc.player.getBoundingBox().expandTowards(rotationVector).inflate(1.0, 1.0, 1.0);
      double maxDistance = Math.max(distancePow2, Math.pow(wallDistance, 2.0));
      EntityHitResult ehr = ProjectileUtil.getEntityHitResult(
         mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.isPickable() && (Aura.target == null || e == Aura.target), maxDistance
      );
      if (ehr != null) {
         boolean allowedWallDistance = startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(wallDistance, 2.0);
         boolean wallMissing = result == null;
         boolean wallBehindEntity = startPoint.distanceToSqr(ehr.getLocation()) < distancePow2;
         boolean allowWallHit = wallMissing || allowedWallDistance || wallBehindEntity;
         if (allowWallHit && startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(distance, 2.0)) {
            return Aura.target == null || ehr.getEntity() == Aura.target;
         }
      }

      return false;
   }

   public boolean checkRtx(float yaw, float pitch, float distance, float wallDistance, Entity entity) {
      HitResult result = this.rayTrace(distance, yaw, pitch);
      Vec3 startPoint = mc.player.position().add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
      double distancePow2 = Math.pow(distance, 2.0);
      if (result != null) {
         distancePow2 = startPoint.distanceToSqr(result.getLocation());
      }

      Vec3 rotationVector = this.getRotationVector(pitch, yaw).scale(distance);
      Vec3 endPoint = startPoint.add(rotationVector);
      AABB entityArea = mc.player.getBoundingBox().expandTowards(rotationVector).inflate(1.0, 1.0, 1.0);
      double maxDistance = Math.max(distancePow2, Math.pow(wallDistance, 2.0));
      EntityHitResult ehr = ProjectileUtil.getEntityHitResult(mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.isPickable() && e == entity, maxDistance);
      if (ehr != null) {
         boolean allowedWallDistance = startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(wallDistance, 2.0);
         boolean wallMissing = result == null;
         boolean wallBehindEntity = startPoint.distanceToSqr(ehr.getLocation()) < distancePow2;
         boolean allowWallHit = wallMissing || allowedWallDistance || wallBehindEntity;
         if (allowWallHit && startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(distance, 2.0)) {
            return ehr.getEntity() == entity;
         }
      }

      return false;
   }

   public Entity getRtxTarget(float yaw, float pitch, float distance, boolean ignoreWalls) {
      Entity targetedEntity = null;
      HitResult result = ignoreWalls ? null : this.rayTrace(distance, yaw, pitch);
      Vec3 vec3d = mc.player.position().add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
      double distancePow2 = Math.pow(distance, 2.0);
      if (result != null) {
         distancePow2 = result.getLocation().distanceToSqr(vec3d);
      }

      Vec3 vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3 vec3d3 = vec3d.add(vec3d2.x * distance, vec3d2.y * distance, vec3d2.z * distance);
      AABB box = mc.player.getBoundingBox().expandTowards(vec3d2.scale(distance)).inflate(1.0, 1.0, 1.0);
      EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(mc.player, vec3d, vec3d3, box, entity -> !entity.isSpectator() && entity.isPickable(), distancePow2);
      if (entityHitResult != null) {
         Entity entity2 = entityHitResult.getEntity();
         Vec3 vec3d4 = entityHitResult.getLocation();
         double g = vec3d.distanceToSqr(vec3d4);
         if ((g < distancePow2 || result == null) && entity2 instanceof LivingEntity) {
            return entity2;
         }
      }

      return targetedEntity;
   }

   public Vec3 getRtxPoint(float yaw, float pitch, float distance) {
      Vec3 vec3d = mc.player.position().add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
      double distancePow2 = Math.pow(distance, 2.0);
      Vec3 vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3 vec3d3 = vec3d.add(vec3d2.x * distance, vec3d2.y * distance, vec3d2.z * distance);
      AABB box = mc.player.getBoundingBox().expandTowards(vec3d2.scale(distance)).inflate(1.0, 1.0, 1.0);
      EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(mc.player, vec3d, vec3d3, box, entity -> !entity.isSpectator() && entity.isPickable(), distancePow2);
      if (entityHitResult != null) {
         Entity entity2 = entityHitResult.getEntity();
         Vec3 vec3d4 = entityHitResult.getLocation();
         if (entity2 instanceof LivingEntity) {
            return vec3d4;
         }
      }

      return null;
   }

   public boolean isLookingAtBox(float yaw, float pitch, BlockPos blockPos) {
      Vec3 vec3d = mc.player.getEyePosition(1.0F);
      Vec3 vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3 vec3d3 = vec3d.add(vec3d2.x * 7.0, vec3d2.y * 7.0, vec3d2.z * 7.0);
      BlockHitResult result = ExplosionUtility.rayCastBlock(new ClipContext(vec3d, vec3d3, Block.COLLIDER, Fluid.NONE, mc.player), blockPos);
      return result != null && result.getType() == Type.BLOCK && result.getBlockPos().equals(blockPos);
   }

   public HitResult rayTrace(double dst, float yaw, float pitch) {
      Vec3 vec3d = mc.player.getEyePosition(1.0F);
      Vec3 vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3 vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
      return mc.level.clip(new ClipContext(vec3d, vec3d3, Block.OUTLINE, Fluid.NONE, mc.player));
   }

   public HitResult getRtxTarget(float yaw, float pitch, double x, double y, double z) {
      HitResult result = this.rayTrace(5.0, yaw, pitch, x, y, z);
      Vec3 vec3d = new Vec3(x, y, z).add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
      double distancePow2 = 25.0;
      if (result != null) {
         distancePow2 = result.getLocation().distanceToSqr(vec3d);
      }

      Vec3 vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3 vec3d3 = vec3d.add(vec3d2.x * 5.0, vec3d2.y * 5.0, vec3d2.z * 5.0);
      AABB box = new AABB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3).expandTowards(vec3d2.scale(5.0)).inflate(1.0, 1.0, 1.0);
      EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(mc.player, vec3d, vec3d3, box, entity -> !entity.isSpectator() && entity.isPickable(), distancePow2);
      if (entityHitResult != null) {
         Entity entity2 = entityHitResult.getEntity();
         Vec3 vec3d4 = entityHitResult.getLocation();
         double g = vec3d.distanceToSqr(vec3d4);
         if ((g < distancePow2 || result == null) && entity2 instanceof LivingEntity) {
            return entityHitResult;
         }
      }

      return result;
   }

   public boolean isInWeb() {
      AABB pBox = mc.player.getBoundingBox();
      BlockPos pBlockPos = BlockPos.containing(mc.player.position());

      for (int x = pBlockPos.getX() - 2; x <= pBlockPos.getX() + 2; x++) {
         for (int y = pBlockPos.getY() - 1; y <= pBlockPos.getY() + 4; y++) {
            for (int z = pBlockPos.getZ() - 2; z <= pBlockPos.getZ() + 2; z++) {
               BlockPos bp = new BlockPos(x, y, z);
               if (pBox.intersects(new AABB(bp)) && mc.level.getBlockState(bp).getBlock() == Blocks.COBWEB) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public HitResult rayTrace(double dst, float yaw, float pitch, double x, double y, double z) {
      Vec3 vec3d = new Vec3(x, y, z);
      Vec3 vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3 vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
      return mc.level.clip(new ClipContext(vec3d, vec3d3, Block.OUTLINE, Fluid.NONE, mc.player));
   }

   public static float[] calcAngle(Vec3 to) {
      if (to == null) {
         return null;
      }

      double difX = to.x - mc.player.getEyePosition().x;
      double difY = (to.y - mc.player.getEyePosition().y) * -1.0;
      double difZ = to.z - mc.player.getEyePosition().z;
      double dist = Mth.sqrt((float)(difX * difX + difZ * difZ));
      return new float[]{
         (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))
      };
   }

   public static Vec2 calcAngleVec(Vec3 to) {
      if (to == null) {
         return null;
      }

      double difX = to.x - mc.player.getEyePosition().x;
      double difY = (to.y - mc.player.getEyePosition().y) * -1.0;
      double difZ = to.z - mc.player.getEyePosition().z;
      double dist = Mth.sqrt((float)(difX * difX + difZ * difZ));
      return new Vec2(
         (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))
      );
   }

   @NotNull
   public Vec3 getRotationVector(float yaw, float pitch) {
      return new Vec3(
         Mth.sin(-pitch * (float) (Math.PI / 180.0)) * Mth.cos(yaw * (float) (Math.PI / 180.0)),
         -Mth.sin(yaw * (float) (Math.PI / 180.0)),
         Mth.cos(-pitch * (float) (Math.PI / 180.0)) * Mth.cos(yaw * (float) (Math.PI / 180.0))
      );
   }

   public static float[] calcAngle(Vec3 from, Vec3 to) {
      if (to == null) {
         return null;
      }

      double difX = to.x - from.x;
      double difY = (to.y - from.y) * -1.0;
      double difZ = to.z - from.z;
      double dist = Mth.sqrt((float)(difX * difX + difZ * difZ));
      return new float[]{
         (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))
      };
   }
}
