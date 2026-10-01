package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import vcore.events.impl.EventTick;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;

public class AirStuck extends Module {
   private static final boolean CANCEL_PACKETS = true;
   private Vec3 freezePos = Vec3.ZERO;

   public AirStuck() {
      super("AirStuck", "Freezes the player in mid-air, preventing movement.", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.freezePos = mc.player.position();
      }
   }

   @Override
   public void onDisable() {
      this.freezePos = Vec3.ZERO;
   }

   @EventHandler
   public void onTick(EventTick e) {
      if (!fullNullCheck() && this.freezePos != Vec3.ZERO) {
         LocalPlayer player = mc.player;
         if (player != null) {
            player.setDeltaMovement(0.0, 0.0, 0.0);
            if (player.input != null) {
               vcore.utility.player.InputUtility.clearMovement(player.input);
            }

            player.setPosRaw(player.getX(), this.freezePos.y, player.getZ());
         }
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (!fullNullCheck()) {
         if (e.getPacket() instanceof ServerboundMovePlayerPacket) {
            e.cancel();
         }
      }
   }
}
