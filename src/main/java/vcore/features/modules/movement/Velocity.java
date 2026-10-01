package vcore.features.modules.movement;

import java.util.Optional;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.injection.accesors.IClientPlayerEntity;
import vcore.injection.accesors.IExplosionS2CPacket;
import vcore.injection.accesors.ISPacketEntityVelocity;
import vcore.setting.Setting;
import vcore.utility.player.MovementUtility;

public class Velocity extends Module {
   public Setting<Boolean> onlyAura = new Setting<>("OnlyDuringAura", false);
   public Setting<Boolean> pauseInWater = new Setting<>("PauseInLiquids", false);
   public Setting<Boolean> explosions = new Setting<>("Explosions", true);
   public Setting<Boolean> cc = new Setting<>("PauseOnFlag", false);
   public Setting<Boolean> fire = new Setting<>("PauseOnFire", false);
   private final Setting<Velocity.modeEn> mode = new Setting<>("Mode", Velocity.modeEn.Cancel);
   public Setting<Float> vertical = new Setting<>("Vertical", 0.0F, 0.0F, 100.0F, v -> this.mode.getValue() == Velocity.modeEn.Custom);
   private final Setting<Velocity.jumpModeEn> jumpMode = new Setting<>("JumpMode", Velocity.jumpModeEn.Jump, v -> this.mode.getValue() == Velocity.modeEn.Jump);
   public Setting<Float> horizontal = new Setting<>(
      "Horizontal", 0.0F, 0.0F, 100.0F, v -> this.mode.getValue() == Velocity.modeEn.Custom || this.mode.getValue() == Velocity.modeEn.Jump
   );
   public Setting<Float> motion = new Setting<>("Motion", 0.42F, 0.4F, 0.5F, v -> this.mode.getValue() == Velocity.modeEn.Jump);
   public Setting<Boolean> fail = new Setting<>("SmartFail", true, v -> this.mode.getValue() == Velocity.modeEn.Jump);
   public Setting<Float> failRate = new Setting<>("FailRate", 0.3F, 0.0F, 1.0F, v -> this.mode.getValue() == Velocity.modeEn.Jump && this.fail.getValue());
   public Setting<Float> jumpRate = new Setting<>("FailJumpRate", 0.25F, 0.0F, 1.0F, v -> this.mode.getValue() == Velocity.modeEn.Jump && this.fail.getValue());
   private boolean doJump;
   private boolean failJump;
   private boolean skip;
   private boolean flag;
   private int grimTicks;
   private int ccCooldown;

   public Velocity() {
      super("Velocity", "Prevents knockback.", Module.Category.MOVEMENT);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (!fullNullCheck()) {
         if (mc.player == null || !mc.player.isInWater() && !mc.player.isUnderWater() && !mc.player.isInLava() || !this.pauseInWater.getValue()) {
            if (mc.player == null || !mc.player.isOnFire() || !this.fire.getValue() || mc.player.hurtTime <= 0) {
               if (this.ccCooldown > 0) {
                  this.ccCooldown--;
               } else {
                   if (e.getPacket() instanceof ClientboundSetEntityMotionPacket pac
                      && pac.getId() == mc.player.getId()
                      && (!this.onlyAura.getValue() || ModuleManager.aura.isEnabled())) {
                      switch ((Velocity.modeEn)this.mode.getValue()) {
                         case Matrix:
                            if (!this.flag) {
                               e.cancel();
                               this.flag = true;
                            } else {
                               this.flag = false;
                               ((ISPacketEntityVelocity)(Object)pac).setMovement(new Vec3(pac.getMovement().x * -0.1, pac.getMovement().y, pac.getMovement().z * -0.1));
                            }
                            break;
                         case Cancel:
                            e.cancel();
                            break;
                         case Sunrise:
                            e.cancel();
                            this.sendPacket(new Pos(mc.player.getX(), -999.0, mc.player.getZ(), true, mc.player.horizontalCollision));
                            break;
                         case Custom:
                            ((ISPacketEntityVelocity)(Object)pac).setMovement(new Vec3(
                               pac.getMovement().x * this.horizontal.getValue() / 100.0,
                               pac.getMovement().y * this.vertical.getValue() / 100.0,
                               pac.getMovement().z * this.horizontal.getValue() / 100.0
                            ));
                            break;
                         case Redirect:
                            double vX = Math.abs(pac.getMovement().x);
                            double vZ = Math.abs(pac.getMovement().z);
                            double[] motion = MovementUtility.forward(vX + vZ);
                            ((ISPacketEntityVelocity)(Object)pac).setMovement(new Vec3(motion[0], 0.0, motion[1]));
                            break;
                         case OldGrim:
                            e.cancel();
                            this.grimTicks = 6;
                            break;
                         case Jump:
                            ((ISPacketEntityVelocity)(Object)pac).setMovement(new Vec3(
                               pac.getMovement().x * this.horizontal.getValue() / 100.0,
                               pac.getMovement().y,
                               pac.getMovement().z * this.horizontal.getValue() / 100.0
                            ));
                            break;
                         case GrimNew:
                            e.cancel();
                            this.flag = true;
                      }
                   }

                   if (e.getPacket() instanceof ClientboundExplodePacket explosion && this.explosions.getValue()) {
                      switch ((Velocity.modeEn)this.mode.getValue()) {
                         case Cancel:
                            ((IExplosionS2CPacket)(Object)explosion).setPlayerKnockback(Optional.empty());
                            break;
                         case Custom:
                            explosion.playerKnockback().ifPresent(kb -> {
                               Vec3 newKb = new Vec3(
                                  kb.x * this.horizontal.getValue() / 100.0,
                                  kb.y * this.vertical.getValue() / 100.0,
                                  kb.z * this.horizontal.getValue() / 100.0
                                );
                               ((IExplosionS2CPacket)(Object)explosion).setPlayerKnockback(Optional.of(newKb));
                            });
                            break;
                         case GrimNew:
                            ((IExplosionS2CPacket)(Object)explosion).setPlayerKnockback(Optional.empty());
                            this.flag = true;
                      }
                   }

                  if (this.mode.getValue() == Velocity.modeEn.OldGrim && e.getPacket() instanceof ClientboundPingPacket && this.grimTicks > 0) {
                     e.cancel();
                     this.grimTicks--;
                  }

                  if (e.getPacket() instanceof ClientboundPlayerPositionPacket && (this.cc.getValue() || this.mode.getValue() == Velocity.modeEn.GrimNew)) {
                     this.ccCooldown = 5;
                  }
               }
            }
         }
      }
   }

   @Override
   public void onUpdate() {
      if (mc.player == null || !mc.player.isInWater() && !mc.player.isUnderWater() || !this.pauseInWater.getValue()) {
         switch ((Velocity.modeEn)this.mode.getValue()) {
            case Matrix:
               if (mc.player.hurtTime > 0 && !mc.player.onGround()) {
                  double var3 = mc.player.getYRot() * (float) (Math.PI / 180.0);
                  double var5 = Math.sqrt(mc.player.getDeltaMovement().x * mc.player.getDeltaMovement().x + mc.player.getDeltaMovement().z * mc.player.getDeltaMovement().z);
                  mc.player.setDeltaMovement(-Math.sin(var3) * var5, mc.player.getDeltaMovement().y, Math.cos(var3) * var5);
                  mc.player.setSprinting(mc.player.tickCount % 2 != 0);
               }
               break;
            case Jump:
               if ((this.failJump || mc.player.hurtTime > 6) && mc.player.onGround()) {
                  if (this.failJump) {
                     this.failJump = false;
                  }

                  if (!this.doJump) {
                     this.skip = true;
                  }

                  if (Math.random() <= this.failRate.getValue().floatValue() && this.fail.getValue()) {
                     if (Math.random() <= this.jumpRate.getValue().floatValue()) {
                        this.doJump = true;
                        this.failJump = true;
                     } else {
                        this.doJump = false;
                        this.failJump = false;
                     }
                  } else {
                     this.doJump = true;
                     this.failJump = false;
                  }

                  if (this.skip) {
                     this.skip = false;
                     return;
                  }

                  switch ((Velocity.jumpModeEn)this.jumpMode.getValue()) {
                     case Motion:
                        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x(), this.motion.getValue().floatValue(), mc.player.getDeltaMovement().z());
                        break;
                     case Jump:
                        mc.player.jumpFromGround();
                        break;
                     case Both:
                        mc.player.jumpFromGround();
                        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, this.motion.getValue().floatValue(), mc.player.getDeltaMovement().z);
                  }
               }
               break;
            case GrimNew:
               if (this.flag) {
                  if (this.ccCooldown <= 0) {
                     this.sendPacket(
                        new PosRot(
                           mc.player.getX(),
                           mc.player.getY(),
                           mc.player.getZ(),
                           ((IClientPlayerEntity)mc.player).getLastYaw(),
                           ((IClientPlayerEntity)mc.player).getLastPitch(),
                           mc.player.onGround(),
                           mc.player.horizontalCollision
                        )
                     );
                     this.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, BlockPos.containing(mc.player.position()), Direction.DOWN));
                  }

                  this.flag = false;
               }
         }

         if (this.grimTicks > 0) {
            this.grimTicks--;
         }
      }
   }

   private boolean isValidMotion(double motion, double min, double max) {
      return Math.abs(motion) > min && Math.abs(motion) < max;
   }

   @Override
   public void onEnable() {
      this.grimTicks = 0;
   }

   public enum jumpModeEn {
      Motion,
      Jump,
      Both;
   }

   public enum modeEn {
      Matrix,
      Cancel,
      Sunrise,
      Custom,
      Redirect,
      OldGrim,
      Jump,
      GrimNew;
   }
}
