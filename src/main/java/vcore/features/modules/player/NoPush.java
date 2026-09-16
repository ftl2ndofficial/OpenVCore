package vcore.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.projectile.FishingHook;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class NoPush extends Module {
   public Setting<Boolean> blocks = new Setting<>("Blocks", true);
   public Setting<Boolean> players = new Setting<>("Players", true);
   public Setting<Boolean> water = new Setting<>("Liquids", false);
   public Setting<Boolean> fishingHook = new Setting<>("FishingHook", false);

   public NoPush() {
      super("NoPush", "Prevents being pushed.", Module.Category.PLAYER);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (e.getPacket() instanceof ClientboundEntityEventPacket pac
         && pac.getEventId() == 31
         && pac.getEntity(mc.level) instanceof FishingHook hook
         && this.fishingHook.getValue()
         && hook.getHookedIn() == mc.player) {
         e.cancel();
      }
   }
}
