package vcore.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action;
import org.jetbrains.annotations.NotNull;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.utility.math.MathUtility;

public final class AntiServerRP extends Module {
   private boolean confirm;
   private boolean accepted;
   private int delay;

   public AntiServerRP() {
      super("AntiServerRP", "Disables server resource packs.", Module.Category.MISC);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.@NotNull Receive e) {
      if (e.getPacket() instanceof ClientboundResourcePackPushPacket) {
         this.confirm = true;
         this.accepted = false;
         this.delay = 0;
         e.cancel();
      }
   }

   @Override
   public void onUpdate() {
      if (this.confirm) {
         this.delay++;
         if (this.delay > MathUtility.random(15.0F, 30.0F) && !this.accepted) {
            this.sendPacket(new ServerboundResourcePackPacket(mc.player.getUUID(), Action.ACCEPTED));
            this.accepted = true;
         }

         if (this.delay > MathUtility.random(40.0F, 60.0F) && this.accepted) {
            this.sendPacket(new ServerboundResourcePackPacket(mc.player.getUUID(), Action.SUCCESSFULLY_LOADED));
            this.confirm = false;
         }
      }
   }
}
