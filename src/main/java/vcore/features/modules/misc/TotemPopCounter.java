package vcore.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.TotemPopEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.AntiBot;
import vcore.gui.notification.Notification;
import vcore.setting.Setting;

public class TotemPopCounter extends Module {
   public Setting<Boolean> notification = new Setting<>("Notification", true);

   public TotemPopCounter() {
      super("TotemPopCounter", "Counts players' totem pops.", Module.Category.MISC);
   }

   @EventHandler
   public void onTotemPop(@NotNull TotemPopEvent event) {
      if (event.getEntity() != mc.player) {
         String s = ChatFormatting.GREEN
            + event.getEntity().getName().getString()
            + ChatFormatting.WHITE
            + " popped "
            + ChatFormatting.AQUA
            + (event.getPops() > 1 ? "" + event.getPops() + ChatFormatting.WHITE + " totems!" : ChatFormatting.WHITE + " a totem!");
         Managers.NOTIFICATION.publicity("TotemPopCounter", s, 2, Notification.Type.INFO);
      }
   }

   @Override
   public void onUpdate() {
      for (Player player : mc.level.players()) {
         if (player != mc.player
            && (!ModuleManager.antiBot.isEnabled() || ModuleManager.antiBot.mode.getValue() != AntiBot.Mode.Matrix || !AntiBot.isBot(player))
            && !(player.getHealth() > 0.0F)
            && Managers.COMBAT.popList.containsKey(player.getName().getString())) {
            String s = ChatFormatting.GREEN
               + player.getName().getString()
               + ChatFormatting.WHITE
               + " popped "
               + (
                  Managers.COMBAT.popList.get(player.getName().getString()) > 1
                     ? "" + Managers.COMBAT.popList.get(player.getName().getString()) + ChatFormatting.WHITE + " totems and died EZ LMAO!"
                     : ChatFormatting.WHITE + " totem and died EZ LMAO!"
               );
            Managers.NOTIFICATION.publicity("TotemPopCounter", s, 2, Notification.Type.INFO);
         }
      }
   }
}
