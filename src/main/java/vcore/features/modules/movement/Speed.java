package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.events.impl.EventMove;
import vcore.events.impl.EventPlayerTravel;
import vcore.events.impl.EventPostTick;
import vcore.events.impl.EventSync;
import vcore.events.impl.EventTick;
import vcore.events.impl.PostPlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.injection.accesors.IInteractionManager;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.interfaces.IEntity;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.MovementUtility;
import vcore.utility.player.SearchInvResult;

public class Speed extends Module {
   public final Setting<Speed.Mode> mode = new Setting<>("Mode", Speed.Mode.NCP);
   public Setting<Boolean> useTimer = new Setting<>("Use Timer", false);
   public Setting<Boolean> pauseInLiquids = new Setting<>("PauseInLiquids", false);
   public Setting<Boolean> pauseWhileSneaking = new Setting<>("PauseWhileSneaking", false);
   public final Setting<Integer> hurttime = new Setting<>("HurtTime", 0, 0, 10, v -> this.mode.is(Speed.Mode.MatrixDamage));
   public final Setting<Float> boostFactor = new Setting<>(
      "BoostFactor", 2.0F, 0.0F, 10.0F, v -> this.mode.is(Speed.Mode.MatrixDamage) || this.mode.is(Speed.Mode.Vanilla)
   );
   public final Setting<Boolean> allowOffGround = new Setting<>("AllowOffGround", true, v -> this.mode.is(Speed.Mode.MatrixDamage));
   public final Setting<Integer> shiftTicks = new Setting<>("ShiftTicks", 0, 0, 10, v -> this.mode.is(Speed.Mode.MatrixDamage));
   public final Setting<Integer> fireWorkSlot = new Setting<>("FireSlot", 1, 1, 9, v -> this.mode.getValue() == Speed.Mode.FireWork);
   public final Setting<Integer> delay = new Setting<>("Delay", 8, 1, 20, v -> this.mode.getValue() == Speed.Mode.FireWork);
   public final Setting<Boolean> strict = new Setting<>("Strict", false, v -> this.mode.is(Speed.Mode.GrimIce));
   public final Setting<Float> matrixJBSpeed = new Setting<>("TimerSpeed", 1.088F, 1.0F, 2.0F, v -> this.mode.is(Speed.Mode.MatrixJB));
   public final Setting<Boolean> armorStands = new Setting<>(
      "ArmorStands", false, v -> this.mode.is(Speed.Mode.GrimCombo) || this.mode.is(Speed.Mode.GrimEntity2)
   );
   public double baseSpeed;
   private int stage;
   private int ticks;
   private int prevSlot;
   private float prevForward = 0.0F;
   private Timer elytraDelay = new Timer();
   private Timer startDelay = new Timer();

   public Speed() {
      super("Speed", "Increases movement speed.", Module.Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      Vcore.TICK_TIMER = 1.0F;
   }

   @Override
   public void onEnable() {
      this.stage = 1;
      this.ticks = 0;
      this.baseSpeed = 0.2873;
      this.startDelay.reset();
      this.prevSlot = -1;
   }

   @EventHandler
   public void onSync(EventSync e) {
      if ((!mc.player.isInLiquid() || !this.pauseInLiquids.getValue()) && (!mc.player.isShiftKeyDown() || !this.pauseWhileSneaking.getValue())) {
         if (this.mode.getValue() == Speed.Mode.MatrixJB) {
            boolean closeToGround = false;

            for (VoxelShape a : mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().inflate(0.5, 0.0, 0.5).move(0.0, -1.0, 0.0))) {
               if (a != Shapes.empty()) {
                  closeToGround = true;
                  break;
               }
            }

            if (MovementUtility.isMoving() && closeToGround && mc.player.fallDistance <= 0.0F) {
               Vcore.TICK_TIMER = 1.0F;
               mc.player.setOnGround(true);
               mc.player.jumpFromGround();
            } else if (mc.player.fallDistance > 0.0F && this.useTimer.getValue()) {
               Vcore.TICK_TIMER = this.matrixJBSpeed.getValue();
               mc.player.push(0.0, -0.003F, 0.0);
            }
         }
      }
   }

   @EventHandler
   public void modifyVelocity(EventPlayerTravel e) {
      if (this.mode.getValue() == Speed.Mode.GrimEntity && !e.isPre() && Vcore.core.getSetBackTime() > 1000L) {
         for (Player ent : Managers.ASYNC.getAsyncPlayers()) {
            if (ent != mc.player && mc.player.distanceToSqr(ent) <= 2.25) {
               float p = mc.level.getBlockState(((IEntity)mc.player).Vcore$getVelocityBP()).getBlock().getFriction();
               float f = mc.player.onGround() ? p * 0.91F : 0.91F;
               float f2 = mc.player.onGround() ? p : 0.99F;
               mc.player.setDeltaMovement(mc.player.getDeltaMovement().x() / f * f2, mc.player.getDeltaMovement().y(), mc.player.getDeltaMovement().z() / f * f2);
               break;
            }
         }
      }

      if ((this.mode.is(Speed.Mode.GrimEntity2) || this.mode.is(Speed.Mode.GrimCombo))
         && !e.isPre()
         && Vcore.core.getSetBackTime() > 1000L
         && MovementUtility.isMoving()) {
         int collisions = 0;

         for (Entity ent : mc.level.entitiesForRendering()) {
            if (ent != mc.player
               && (!(ent instanceof ArmorStand) || this.armorStands.getValue())
               && (ent instanceof LivingEntity || ent instanceof Boat)
               && mc.player.getBoundingBox().inflate(1.0).intersects(ent.getBoundingBox())) {
               collisions++;
            }
         }

         double[] motion = MovementUtility.forward(0.08 * collisions);
         mc.player.push(motion[0], 0.0, motion[1]);
      }
   }

   @EventHandler
   public void onTick(EventTick e) {
      if ((this.mode.is(Speed.Mode.GrimIce) || this.mode.is(Speed.Mode.GrimCombo)) && mc.player.onGround()) {
         BlockPos pos = ((IEntity)mc.player).Vcore$getVelocityBP();
         SearchInvResult result = InventoryUtility.findBlockInHotBar(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);
         if (mc.level.isEmptyBlock(pos) || !result.found() || !mc.options.keyJump.isDown()) {
            return;
         }

         this.prevSlot = mc.player.getInventory().getSelectedSlot();
         result.switchTo();
         this.sendPacket(new PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), 90.0F, mc.player.onGround(), mc.player.horizontalCollision));
         if (this.strict.getValue()) {
            this.sendPacket(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, Direction.UP));
            this.sendPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, pos, Direction.UP));
         }

         this.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
         this.sendSequencedPacket(
            id -> new ServerboundUseItemOnPacket(
               InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0.0, 0.5, 0.0), Direction.UP, pos.below(), false), id
            )
         );
         mc.level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
      }
   }

   @EventHandler
   public void onPostTick(EventPostTick e) {
      if ((this.mode.is(Speed.Mode.GrimIce) || this.mode.is(Speed.Mode.GrimCombo)) && this.prevSlot != -1) {
         mc.player.getInventory().setSelectedSlot(this.prevSlot);
         ((IInteractionManager)mc.gameMode).syncSlot();
         this.prevSlot = -1;
      }
   }

   @Override
   public void onUpdate() {
      if ((!mc.player.isInLiquid() || !this.pauseInLiquids.getValue()) && (!mc.player.isShiftKeyDown() || !this.pauseWhileSneaking.getValue())) {
         if (this.mode.getValue() == Speed.Mode.FireWork) {
            this.ticks--;
            int ellySlot = InventoryUtility.getElytra();
            int fireSlot = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET).slot();
            boolean inOffHand = mc.player.getOffhandItem().getItem() == Items.FIREWORK_ROCKET;
            if (fireSlot == -1) {
               int fireInInv = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET).slot();
               if (fireInInv != -1) {
                  mc.gameMode
                     .handleInventoryMouseClick(mc.player.containerMenu.containerId, fireInInv, this.fireWorkSlot.getValue() - 1, ClickType.SWAP, mc.player);
               }
            }

            if (ellySlot != -1 && (fireSlot != -1 || inOffHand) && !mc.player.onGround() && mc.player.fallDistance > 0.0F && this.ticks <= 0) {
               if (ellySlot != -2) {
                  mc.gameMode.handleInventoryMouseClick(0, ellySlot, 1, ClickType.PICKUP, mc.player);
                  mc.gameMode.handleInventoryMouseClick(0, 6, 1, ClickType.PICKUP, mc.player);
               }

               mc.player
                  .connection
                  .send(new ServerboundPlayerCommandPacket(mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
               int prevSlot = mc.player.getInventory().getSelectedSlot();
               if (prevSlot != fireSlot && !inOffHand) {
                  this.sendPacket(new ServerboundSetCarriedItemPacket(fireSlot));
               }

               mc.gameMode.useItem(mc.player, inOffHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
               if (prevSlot != fireSlot && !inOffHand) {
                  this.sendPacket(new ServerboundSetCarriedItemPacket(prevSlot));
               }

               if (ellySlot != -2) {
                  mc.gameMode.handleInventoryMouseClick(0, 6, 1, ClickType.PICKUP, mc.player);
                  mc.gameMode.handleInventoryMouseClick(0, ellySlot, 1, ClickType.PICKUP, mc.player);
               }

               mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
               this.ticks = this.delay.getValue();
            }
         }

         if (this.mode.getValue() == Speed.Mode.ElytraLowHop) {
            if (mc.player.onGround()) {
               mc.player.jumpFromGround();
               return;
            }

            if (mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().inflate(-0.29, 0.0, -0.29).move(0.0, -3.0, 0.0)).iterator().hasNext()
               && this.elytraDelay.passedMs(150L)
               && this.startDelay.passedMs(500L)) {
               int elytra = InventoryUtility.getElytra();
               if (elytra == -1) {
                  this.disable("You need elytra for this mode!");
               }

               mc.player.setDeltaMovement(mc.player.getDeltaMovement().x(), 0.0, mc.player.getDeltaMovement().z());
               if (MovementUtility.isMoving()) {
                  MovementUtility.setMotion(0.85);
               }

               this.elytraDelay.reset();
            }
         }
      }
   }

   @EventHandler
   public void onPostPlayerUpdate(PostPlayerUpdateEvent event) {
      if (this.mode.getValue() == Speed.Mode.MatrixDamage && MovementUtility.isMoving() && mc.player.hurtTime > this.hurttime.getValue()) {
         if (mc.player.onGround()) {
            MovementUtility.setMotion(0.387F * this.boostFactor.getValue());
         } else if (mc.player.isInWater()) {
            MovementUtility.setMotion(0.346F * this.boostFactor.getValue());
         } else if (!mc.player.onGround() && this.allowOffGround.getValue()) {
            MovementUtility.setMotion(0.448F * this.boostFactor.getValue());
         }

         if (this.shiftTicks.getValue() > 0) {
            event.cancel();
            event.setIterations(this.shiftTicks.getValue());
         }
      }
   }

   @EventHandler
   public void onMove(EventMove event) {
      if ((!mc.player.isInLiquid() || !this.pauseInLiquids.getValue()) && (!mc.player.isShiftKeyDown() || !this.pauseWhileSneaking.getValue())) {
         if (this.mode.getValue() == Speed.Mode.NCP || this.mode.getValue() == Speed.Mode.StrictStrafe) {
            if (!mc.player.getAbilities().flying) {
               if (!mc.player.isFallFlying()) {
                  if (mc.player.getFoodData().getFoodLevel() > 6) {
                     if (!event.isCancelled()) {
                        event.cancel();
                        if (MovementUtility.isMoving()) {
                           Vcore.TICK_TIMER = this.useTimer.getValue() ? 1.088F : 1.0F;
                           float currentSpeed = this.mode.getValue() == Speed.Mode.NCP && vcore.utility.player.InputUtility.getForward(mc.player.input) <= 0.0F && this.prevForward > 0.0F
                              ? Managers.PLAYER.currentPlayerSpeed * 0.66F
                              : Managers.PLAYER.currentPlayerSpeed;
                           boolean canJump = !mc.player.horizontalCollision;
                           if (this.stage == 1 && mc.player.onGround() && canJump) {
                              mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, MovementUtility.getJumpSpeed(), mc.player.getDeltaMovement().z);
                              event.setY(MovementUtility.getJumpSpeed());
                              this.baseSpeed *= 2.149;
                              this.stage = 2;
                           } else if (this.stage == 2) {
                              this.baseSpeed = currentSpeed - 0.66 * (currentSpeed - MovementUtility.getBaseMoveSpeed());
                              this.stage = 3;
                           } else {
                              if (mc.level
                                    .getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, mc.player.getDeltaMovement().y(), 0.0))
                                    .iterator()
                                    .hasNext()
                                 || mc.player.verticalCollision) {
                                 this.stage = 1;
                              }

                              this.baseSpeed = currentSpeed - currentSpeed / 159.0;
                           }

                           this.baseSpeed = Math.max(this.baseSpeed, MovementUtility.getBaseMoveSpeed());
                           double ncpSpeed = this.mode.getValue() != Speed.Mode.StrictStrafe && !(vcore.utility.player.InputUtility.getForward(mc.player.input) < 1.0F) ? 0.576 : 0.465;
                           double ncpBypassSpeed = this.mode.getValue() != Speed.Mode.StrictStrafe && !(vcore.utility.player.InputUtility.getForward(mc.player.input) < 1.0F) ? 0.57 : 0.44;
                           if (mc.player.hasEffect(MobEffects.SPEED)) {
                              double amplifier = mc.player.getEffect(MobEffects.SPEED).getAmplifier();
                              ncpSpeed *= 1.0 + 0.2 * (amplifier + 1.0);
                              ncpBypassSpeed *= 1.0 + 0.2 * (amplifier + 1.0);
                           }

                           if (mc.player.hasEffect(MobEffects.SLOWNESS)) {
                              double amplifier = mc.player.getEffect(MobEffects.SLOWNESS).getAmplifier();
                              ncpSpeed /= 1.0 + 0.2 * (amplifier + 1.0);
                              ncpBypassSpeed /= 1.0 + 0.2 * (amplifier + 1.0);
                           }

                           this.baseSpeed = Math.min(this.baseSpeed, this.ticks > 25 ? ncpSpeed : ncpBypassSpeed);
                           if (this.ticks++ > 50) {
                              this.ticks = 0;
                           }

                           MovementUtility.modifyEventSpeed(event, this.baseSpeed);
                           this.prevForward = vcore.utility.player.InputUtility.getForward(mc.player.input);
                        } else {
                           Vcore.TICK_TIMER = 1.0F;
                           event.setX(0.0);
                           event.setZ(0.0);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public enum Mode {
      ElytraLowHop,
      FireWork,
      GrimCombo,
      GrimEntity,
      GrimEntity2,
      GrimIce,
      MatrixDamage,
      MatrixJB,
      NCP,
      StrictStrafe,
      Vanilla;
   }
}
