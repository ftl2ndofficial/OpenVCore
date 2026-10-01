package vcore.features.modules.player;

import java.util.Comparator;
import java.util.HashMap;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventEntitySpawn;
import vcore.events.impl.EventPostSync;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.utility.Timer;
import vcore.utility.math.MathUtility;
import vcore.utility.player.MovementUtility;

public class PearlChaser extends Module {
   private final Setting<BooleanSettingGroup> stopMotion = new Setting<>("StopMotion", new BooleanSettingGroup(false));
   private final Setting<Boolean> legitStop = new Setting<>("LegitStop", false).addToGroup(this.stopMotion);
   private final Setting<Boolean> pauseAura = new Setting<>("PauseAura", false);
   private final Setting<Boolean> onlyOnGround = new Setting<>("OnlyOnGround", false);
   private final Setting<Boolean> noMove = new Setting<>("NoMove", false);
   private final Setting<Boolean> onlyTarget = new Setting<>("OnlyTarget", false);
   private Runnable postSyncAction;
   private final Timer delayTimer = new Timer();
   private BlockPos targetBlock;
   private int lastPearlId;
   private int lastOurPearlId;
   private HashMap<Player, Long> targets = new HashMap<>();

   public PearlChaser() {
      super("PearlChaser", "Auto pursuit with pearls.", Module.Category.PLAYER);
   }

   @EventHandler
   public void onEntitySpawn(EventEntitySpawn e) {
      if (e.getEntity() instanceof ThrownEnderpearl) {
         mc.level.players().stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(e.getEntity().position()))).ifPresent(player -> {
            if (player.equals(mc.player)) {
               this.lastOurPearlId = e.getEntity().getId();
            }
         });
      }
   }

   @EventHandler(priority = -100)
   public void onSync(EventSync event) {
      if (this.onlyTarget.getValue()
         && Aura.target != null
         && ModuleManager.aura.isEnabled()
         && Aura.target instanceof Player pl
         && !this.targets.containsKey(pl)) {
         this.targets.put(pl, System.currentTimeMillis());
      }

      if (!(mc.player.getHealth() < 5.0F)) {
         if (this.delayTimer.passedMs(1000L)) {
            for (Entity ent : mc.level.entitiesForRendering()) {
               if (ent instanceof ThrownEnderpearl && ent.getId() != this.lastPearlId && ent.getId() != this.lastOurPearlId) {
                  mc.level
                     .players()
                     .stream()
                     .filter(e -> this.targets.containsKey(e) || !this.onlyTarget.getValue())
                     .min(Comparator.comparingDouble(p -> p.distanceToSqr(ent.position())))
                     .ifPresent(player -> {
                        if (!player.equals(mc.player)) {
                           this.targetBlock = this.calcTrajectory(ent);
                           this.lastPearlId = ent.getId();
                        }
                     });
               }
            }

            if (this.targetBlock != null) {
               if (!(mc.player.distanceToSqr(Vec3.atCenterOf(this.targetBlock)) < 49.0)) {
                  float rotationPitch = (float)(-Math.toDegrees(this.calcTrajectory(this.targetBlock)));
                  float rotationYaw = (float)Math.toDegrees(
                        Math.atan2(this.targetBlock.getZ() + 0.5F - mc.player.getZ(), this.targetBlock.getX() + 0.5F - mc.player.getX())
                     )
                     - 90.0F;
                  BlockPos tracedBP = this.checkTrajectory(rotationYaw, rotationPitch);
                  if (tracedBP != null && !(this.targetBlock.distToCenterSqr(Vec3.atCenterOf(tracedBP)) > 36.0)) {
                     if (this.pauseAura.getValue() && ModuleManager.aura.isEnabled()) {
                        ModuleManager.aura.pause();
                     }

                     if (!this.onlyOnGround.getValue() || mc.player.onGround()) {
                        if (!this.noMove.getValue() || !MovementUtility.isMoving()) {
                           if (this.stopMotion.getValue().isEnabled()) {
                              if (!this.legitStop.getValue()) {
                                 mc.player.setDeltaMovement(0.0, 0.0, 0.0);
                              }

                              mc.options.keyUp.setDown(false);
                              mc.options.keyDown.setDown(false);
                              mc.options.keyLeft.setDown(false);
                              mc.options.keyRight.setDown(false);
                              vcore.utility.player.InputUtility.setMoveVector(mc.player.input, 0.0F, 0.0F);
                           } else {
                              this.sendMessage(
                                 "Chasing pearl on X:"
                                    + tracedBP.getX()
                                    + " Y:"
                                    + tracedBP.getY()
                                    + " Z:"
                                    + tracedBP.getZ()
                                    + " Angle Y:"
                                    + rotationYaw
                                    + " P:"
                                    + rotationPitch
                              );
                              mc.player.setYRot(rotationYaw);
                              mc.player.setXRot(MathUtility.clamp(rotationPitch, -89.0F, 89.0F));
                              float yaw = mc.player.getYRot();
                              float pitch = mc.player.getXRot();
                              this.postSyncAction = () -> {
                                 int epSlot = this.findEPSlot();
                                 int originalSlot = mc.player.getInventory().getSelectedSlot();
                                 if (epSlot != -1) {
                                    mc.player.getInventory().setSelectedSlot(epSlot);
                                    this.sendPacket(new ServerboundSetCarriedItemPacket(epSlot));
                                    this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, yaw, pitch));
                                    this.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                                    mc.player.getInventory().setSelectedSlot(originalSlot);
                                    this.sendPacket(new ServerboundSetCarriedItemPacket(originalSlot));
                                 }
                              };
                              this.targetBlock = null;
                              this.delayTimer.reset();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPostSync(EventPostSync event) {
      if (this.postSyncAction != null) {
         this.postSyncAction.run();
         this.postSyncAction = null;
      }
   }

   private int findEPSlot() {
      int epSlot = -1;
      if (mc.player.getMainHandItem().getItem() == Items.ENDER_PEARL) {
         epSlot = mc.player.getInventory().getSelectedSlot();
      }

      if (epSlot == -1) {
         for (int l = 0; l < 9; l++) {
            if (mc.player.getInventory().getItem(l).getItem() == Items.ENDER_PEARL) {
               epSlot = l;
               break;
            }
         }
      }

      return epSlot;
   }

   private float calcTrajectory(@NotNull BlockPos bp) {
      double a = Math.hypot(bp.getX() + 0.5F - mc.player.getX(), bp.getZ() + 0.5F - mc.player.getZ());
      double y = 6.125 * (bp.getY() + 1.0F - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose())));
      y = 0.05F * (0.05F * (a * a) + y);
      y = Math.sqrt(9.378906F - y);
      double d = 3.0625 - y;
      y = Math.atan2(d * d + y, 0.05F * a);
      d = Math.atan2(d, 0.05F * a);
      return (float)Math.min(y, d);
   }

   private BlockPos calcTrajectory(Entity e) {
      return this.traceTrajectory(e.getX(), e.getY(), e.getZ(), e.getDeltaMovement().x, e.getDeltaMovement().y, e.getDeltaMovement().z);
   }

   private BlockPos checkTrajectory(float yaw, float pitch) {
      if (Float.isNaN(pitch)) {
         return null;
      }

      float yawRad = yaw / 180.0F * (float) Math.PI;
      float pitchRad = pitch / 180.0F * (float) Math.PI;
      double x = mc.player.getX() - Mth.cos(yawRad) * 0.16F;
      double y = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
      double z = mc.player.getZ() - Mth.sin(yawRad) * 0.16F;
      double motionX = -Mth.sin(yawRad) * Mth.cos(pitchRad) * 0.4F;
      double motionY = -Mth.sin(pitchRad) * 0.4F;
      double motionZ = Mth.cos(yawRad) * Mth.cos(pitchRad) * 0.4F;
      float distance = Mth.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
      motionX /= distance;
      motionY /= distance;
      motionZ /= distance;
      motionX *= 1.5;
      motionY *= 1.5;
      motionZ *= 1.5;
      if (!mc.player.onGround()) {
         motionY += mc.player.getDeltaMovement().y();
      }

      return this.traceTrajectory(x, y, z, motionX, motionY, motionZ);
   }

   private BlockPos traceTrajectory(double x, double y, double z, double mx, double my, double mz) {
      for (int i = 0; i < 300; i++) {
         Vec3 lastPos = new Vec3(x, y, z);
         x += mx;
         y += my;
         z += mz;
         mx *= 0.99;
         my *= 0.99;
         mz *= 0.99;
         my -= 0.03F;
         Vec3 pos = new Vec3(x, y, z);
         BlockHitResult bhr = mc.level.clip(new ClipContext(lastPos, pos, Block.OUTLINE, Fluid.NONE, mc.player));
         if (bhr != null && bhr.getType() == Type.BLOCK) {
            return bhr.getBlockPos();
         }

         for (Entity ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof Arrow)
               && ent != mc.player
               && !(ent instanceof ThrownEnderpearl)
               && ent.getBoundingBox().intersects(new AABB(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.2))) {
               return null;
            }
         }

         if (y <= -65.0) {
            break;
         }
      }

      return null;
   }
}
