package vcore.features.modules.misc;

import java.util.Locale;
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
   public static Setting<Boolean> showIP = new Setting<>("ShowIP", true);
   public static Setting<Boolean> nickname = new Setting<>("Nickname", true);
   public static final String DISCORD_LINK = "https://dsc.knualabs.icu";
   private static final String NAME_PROTECT_SMALL_IMAGE_TEXT = "https://dsc.knualabs.icu";
   public static DiscordRichPresence presence;
   public static boolean started;
   private final Timer timer_delay = new Timer();
   private static Thread thread;
   String slov;
   String[] rpc_perebor_en = new String[]{"Playing Minecraft", "Chilling with OpenVCore"};
   int randomInt;

   public RPC() {
      super("DiscordRPC", "Discord rich presence integration.", Module.Category.MISC);
   }

   public static boolean isWindows() {
      String os = System.getProperty("os.name");
      return os != null && os.toLowerCase(Locale.ROOT).contains("win");
   }

   @Override
   public void onEnable() {
      if (!isWindows() || !DiscordRPC.isSupported()) {
         this.disable();
         return;
      }
      this.startRpc();
   }

   @Override
   public void onDisable() {
      started = false;
      if (thread != null && !thread.isInterrupted()) {
         thread.interrupt();
         thread = null;
      }

      if (isWindows()) {
         try {
            DiscordRPC rpc = DiscordRPC.getInstance();
            if (rpc != null) {
               rpc.Discord_Shutdown();
            }
         } catch (Throwable ignored) {
         }
      }
   }

   @Override
   public void onUpdate() {
      if (!isWindows() || !DiscordRPC.isSupported()) {
         if (started) {
            this.onDisable();
         }
         return;
      }
      this.startRpc();
   }

   public void startRpc() {
      if (!isWindows() || this.isDisabled() || started) {
         return;
      }

      DiscordRPC rpc = DiscordRPC.getInstance();
      if (rpc == null) {
         return;
      }

      try {
         started = true;
         if (presence == null) {
            presence = new DiscordRichPresence();
         }
         DiscordEventHandlers handlers = new DiscordEventHandlers();
         rpc.Discord_Initialize("1555445272459485244", handlers, true, "");
         presence.startTimestamp = System.currentTimeMillis() / 1000L;
         presence.largeImageKey = "https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExdzdvbmNocjZoN3lxaDVkbW0zYnA0eHNqODF1bjRtdmM4cm1zNnprcyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/DyvuwYcDPGgI86JGWw/giphy.gif";
         this.updateButtons();
         rpc.Discord_UpdatePresence(presence);
         thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
               try {
                  DiscordRPC currentRpc = DiscordRPC.getInstance();
                  if (currentRpc == null || !started) {
                     break;
                  }
                  currentRpc.Discord_RunCallbacks();
                  if (presence != null) {
                     presence.details = this.getDetails();
                     presence.state = "Build >> Release (1.21.11)";
                     if (nickname.getValue()) {
                        presence.smallImageText = ModuleManager.nameProtect.isEnabled() ? NAME_PROTECT_SMALL_IMAGE_TEXT : mc.getUser().getName();
                        presence.smallImageKey = "https://minotar.net/helm/" + mc.getUser().getName() + "/100.png";
                     } else {
                        presence.smallImageText = "";
                        presence.smallImageKey = "";
                     }

                     this.updateButtons();
                     currentRpc.Discord_UpdatePresence(presence);
                  }
               } catch (Throwable ignored) {
               }

               try {
                  Thread.sleep(2000L);
               } catch (InterruptedException var2) {
                  break;
               }
            }
         }, "RPC-Callback-Handler");
         thread.setDaemon(true);
         thread.start();
      } catch (Throwable t) {
         started = false;
      }
   }

   private void updateButtons() {
      if (presence != null) {
         presence.button_label_1 = "Discord";
         presence.button_url_1 = DISCORD_LINK;
         presence.button_label_2 = null;
         presence.button_url_2 = null;
      }
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
