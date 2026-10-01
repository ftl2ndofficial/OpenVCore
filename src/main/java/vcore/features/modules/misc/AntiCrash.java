package vcore.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.jetbrains.annotations.NotNull;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.Timer;

public class AntiCrash extends Module {
   public final Setting<Boolean> debug = new Setting<>("Debug", false);
   private Timer debugTimer = new Timer();

   public AntiCrash() {
      super("AntiCrash", "Prevents game crashes.", Module.Category.MISC);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.@NotNull Receive receive) {
      if (!(
         receive.getPacket() instanceof ClientboundExplodePacket exp && (exp.center().x > 1.0E9 || exp.center().y > 1.0E9 || exp.center().z > 1.0E9 || exp.radius() > 1.0E9)
      )) {
         if (!(
            receive.getPacket() instanceof ClientboundLevelParticlesPacket p
               && (
                  p.getX() > 1.0E9
                     || p.getY() > 1.0E9
                     || p.getZ() > 1.0E9
                     || p.getMaxSpeed() > 1.0E9
                     || p.getXDist() > 1.0E9
                     || p.getYDist() > 1.0E9
                     || p.getZDist() > 1.0E9
               )
         )) {
            if (receive.getPacket() instanceof ClientboundPlayerPositionPacket pos
               && (pos.change().position().x > 1.0E9 || pos.change().position().y > 1.0E9 || pos.change().position().z > 1.0E9 || pos.change().yRot() > 1.0E9 || pos.change().xRot() > 1.0E9)) {
               if (this.debug.getValue() && this.debugTimer.passedMs(1000L)) {
                  this.sendMessage("PlayerPositionLookS2CPacket canceled");
                  this.debugTimer.reset();
               }

               receive.cancel();
            }
         } else {
            if (this.debug.getValue() && this.debugTimer.passedMs(1000L)) {
               this.sendMessage("ParticleS2CPacket canceled");
               this.debugTimer.reset();
            }

            receive.cancel();
         }
      } else {
         if (this.debug.getValue() && this.debugTimer.passedMs(1000L)) {
            this.sendMessage("ExplosionS2CPacket canceled");
            this.debugTimer.reset();
         }

         receive.cancel();
      }
   }
}
