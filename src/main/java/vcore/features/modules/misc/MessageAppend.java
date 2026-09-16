package vcore.features.modules.misc;

import java.util.Objects;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.world.item.Items;
import vcore.core.Managers;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class MessageAppend extends Module {
   private final Setting<String> word = new Setting<>("word", " | NextGen Release");
   private String skip;

   public MessageAppend() {
      super("MessageAppend", "Appends text to chat messages.", Module.Category.MISC);
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (!fullNullCheck()) {
         if (e.getPacket() instanceof ServerboundChatPacket pac) {
            if (Objects.equals(pac.message(), this.skip)) {
               return;
            }

            if (mc.player.getMainHandItem().getItem() == Items.FILLED_MAP || mc.player.getOffhandItem().getItem() == Items.FILLED_MAP) {
               return;
            }

            if (pac.message().startsWith("/") || pac.message().startsWith(Managers.COMMAND.getPrefix())) {
               return;
            }

            this.skip = pac.message() + this.word.getValue();
            mc.player.connection.sendChat(pac.message() + this.word.getValue());
            e.cancel();
         }
      }
   }
}
