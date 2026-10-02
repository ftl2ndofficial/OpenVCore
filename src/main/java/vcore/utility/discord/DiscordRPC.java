package vcore.utility.discord;

import com.sun.jna.Library;
import com.sun.jna.Native;
import java.util.Locale;

public interface DiscordRPC extends Library {
   class Holder {
      private static final DiscordRPC INSTANCE = loadInstance();

      private static DiscordRPC loadInstance() {
         if (!isWindows()) {
            return null;
         }
         try {
            return (DiscordRPC) Native.load("discord-rpc", DiscordRPC.class);
         } catch (Throwable ignored) {
            return null;
         }
      }

      public static boolean isWindows() {
         String os = System.getProperty("os.name");
         return os != null && os.toLowerCase(Locale.ROOT).contains("win");
      }
   }

   DiscordRPC INSTANCE = Holder.INSTANCE;

   static boolean isSupported() {
      return Holder.isWindows() && Holder.INSTANCE != null;
   }

   static DiscordRPC getInstance() {
      return Holder.INSTANCE;
   }

   void Discord_UpdateHandlers(DiscordEventHandlers var1);

   void Discord_UpdatePresence(DiscordRichPresence var1);

   void Discord_Respond(String var1, int var2);

   void Discord_Register(String var1, String var2);

   void Discord_Shutdown();

   void Discord_UpdateConnection();

   void Discord_RegisterSteamGame(String var1, String var2);

   void Discord_RunCallbacks();

   void Discord_Initialize(String var1, DiscordEventHandlers var2, boolean var3, String var4);

   void Discord_ClearPresence();
}
