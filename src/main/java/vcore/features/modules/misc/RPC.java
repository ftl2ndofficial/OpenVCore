package vcore.features.modules.misc;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.discord.DiscordEventHandlers;
import vcore.utility.discord.DiscordRPC;
import vcore.utility.discord.DiscordRichPresence;

public final class RPC extends Module {
   private static final DiscordRPC rpc = DiscordRPC.INSTANCE;
   public static Setting<Boolean> showIP = new Setting<>("ShowIP", true);
   public static Setting<Boolean> nickname = new Setting<>("Nickname", true);
   private static final String NAME_PROTECT_SMALL_IMAGE_TEXT = "discord.gg/GNYyWbwKUE";
   public static DiscordRichPresence presence = new DiscordRichPresence();
   public static boolean started;
   private final Timer timer_delay = new Timer();
   private static Thread thread;
   String slov;
   String[] rpc_perebor_en = new String[]{"Playing Minecraft", "Chilling with OpenVCore"};
   int randomInt;

   public RPC() {
      super("DiscordRPC", "Discord rich presence integration.", Module.Category.MISC);
   }

   @Override
   public void onDisable() {
      started = false;
      if (thread != null && !thread.isInterrupted()) {
         thread.interrupt();
      }

      rpc.Discord_Shutdown();
   }

   @Override
   public void onUpdate() {
      this.startRpc();
   }

   public void startRpc() {
      if (!this.isDisabled()) {
         if (!started) {
            started = true;
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            rpc.Discord_Initialize("1555445272459485244", handlers, true, "");
            presence.startTimestamp = System.currentTimeMillis() / 1000L;
            presence.largeImageKey = "https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExdzdvbmNocjZoN3lxaDVkbW0zYnA0eHNqODF1bjRtdmM4cm1zNnprcyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/DyvuwYcDPGgI86JGWw/giphy.gif";
            rpc.Discord_UpdatePresence(presence);
            thread = new Thread(() -> {
               while (!Thread.currentThread().isInterrupted()) {
                  rpc.Discord_RunCallbacks();
                  presence.details = this.getDetails();
                  presence.state = "Build >> Release (1.21.11)";
                  if (nickname.getValue()) {
                     presence.smallImageText = ModuleManager.nameProtect.isEnabled() ? "discord.gg/GNYyWbwKUE" : mc.getUser().getName();
                     presence.smallImageKey = "https://minotar.net/helm/" + mc.getUser().getName() + "/100.png";
                  } else {
                     presence.smallImageText = "";
                     presence.smallImageKey = "";
                  }

                  this.clearButtons();
                  rpc.Discord_UpdatePresence(presence);

                  try {
                     Thread.sleep(2000L);
                  } catch (InterruptedException var2) {
                  }
               }
            }, "RPC-Callback-Handler");
            thread.start();
         }
      }
   }

   private void clearButtons() {
      presence.button_label_1 = null;
      presence.button_url_1 = null;
      presence.button_label_2 = null;
      presence.button_url_2 = null;
   }

   private String getDetails() {
      String result = "";
      if (!(mc.screen instanceof JoinMultiplayerScreen) && !(mc.screen instanceof ManageServerScreen) && !(mc.screen instanceof TitleScreen)) {
         if (mc.getCurrentServer() != null) {
            result = showIP.getValue() ? "Playing on " + mc.getCurrentServer().ip : "Playing on server";
         } else if (mc.isLocalServer()) {
            result = "SinglePlayer";
         }
      } else {
         if (this.slov == null || this.timer_delay.passedMs(60000L)) {
            this.randomInt = ThreadLocalRandom.current().nextInt(this.rpc_perebor_en.length);
            this.slov = this.rpc_perebor_en[this.randomInt];
            this.timer_delay.reset();
         }

         result = this.slov;
      }

      return result;
   }
}
