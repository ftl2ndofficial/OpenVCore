package vcore.features.modules.misc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class FixHP extends Module {
   private static final Pattern ACTION_BAR_HEALTH_ENTRY = Pattern.compile("(?<![A-Za-z0-9_])([A-Za-z0-9_]{3,16})\\s*\\(([^()]*)\\)");
   private static final Pattern DECIMAL_HEALTH_VALUE = Pattern.compile("[0-9]+(?:[\\.,][0-9]+)?");
   private static final long ACTION_BAR_HEALTH_CACHE_MS = 1500L;
   private static final Map<UUID, Float> ACTION_BAR_HEALTH_CACHE = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> ACTION_BAR_HEALTH_CACHE_TIME = new ConcurrentHashMap<>();
   private static FixHP INSTANCE;
   private final Setting<FixHP.Mode> mode = new Setting<>("Mode", FixHP.Mode.Scoreboard);

   public FixHP() {
      super("FixHP", "Accurate HP for Matrix AntiCheat.", Module.Category.MISC);
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      clearActionBarHealthCache();
   }

   @Override
   public void onEnable() {
      clearActionBarHealthCache();
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.isEnabled() && this.mode.getValue() == FixHP.Mode.ActionBar && mc.level != null) {
         if (event.getPacket() instanceof ClientboundSystemChatPacket packet) {
            cacheActionBarHealth(packet.content().getString());
         }
      }
   }

   public static float getHealth(Player ent) {
      if (ent == null) {
         return 0.0F;
      } else if (INSTANCE == null || !INSTANCE.isEnabled()) {
         return ent.getHealth();
      } else if (ent == mc.player) {
         return ent.getHealth();
      } else if (mc.level != null && mc.getConnection() != null && mc.getConnection().getServerData() != null) {
         return getMode() == FixHP.Mode.ActionBar ? getActionBarHealth(ent) : getScoreboardHealth(ent);
      } else {
         return ent.getHealth();
      }
   }

   public static boolean isActionBarModeActive() {
      return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.mode.getValue() == FixHP.Mode.ActionBar;
   }

   private static float getScoreboardHealth(Player ent) {
      try {
         Scoreboard scoreboard = ent.level().getScoreboard();
         Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
         if (objective == null) {
            return ent.getHealth();
         }

         ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(ent, objective);
         return score.value();
      } catch (Exception ignored) {
         return ent.getHealth();
      }
   }

   private static float getActionBarHealth(Player ent) {
      Float cachedHealth = getCachedActionBarHealth(ent);
      return cachedHealth != null ? cachedHealth : ent.getHealth();
   }

   private static void cacheActionBarHealth(String actionBarText) {
      if (actionBarText != null && !actionBarText.isBlank() && mc.level != null) {
         Matcher matcher = ACTION_BAR_HEALTH_ENTRY.matcher(actionBarText);

         while (matcher.find()) {
            Float health = parseActionBarHealthValue(matcher.group(2));
            if (health != null) {
               Player player = findWorldPlayerByName(matcher.group(1));
               if (player != null) {
                  cacheActionBarHealth(player, health);
               }
            }
         }
      }
   }

   private static Float parseActionBarHealthValue(String rawValue) {
      if (rawValue == null) {
         return null;
      }

      String value = removeLastCodePoint(rawValue.trim()).trim();

      while (!value.isEmpty() && isTrailingHeartCodePoint(value.codePointBefore(value.length()))) {
         value = removeLastCodePoint(value).trim();
      }

      if (!DECIMAL_HEALTH_VALUE.matcher(value).matches()) {
         return null;
      }

      try {
         float health = Float.parseFloat(value.replace(',', '.'));
         return health >= 0.0F && health <= 2048.0F ? health : null;
      } catch (NumberFormatException ignored) {
         return null;
      }
   }

   private static Player findWorldPlayerByName(String playerName) {
      if (playerName != null && mc.level != null) {
         for (Player player : mc.level.players()) {
            if (playerName.equals(player.getName().getString())) {
               return player;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String removeLastCodePoint(String value) {
      return value != null && !value.isEmpty() ? value.substring(0, value.offsetByCodePoints(value.length(), -1)) : "";
   }

   private static boolean isTrailingHeartCodePoint(int codePoint) {
      return codePoint == 10084 || codePoint == 9829 || codePoint == 9825 || codePoint == 65039;
   }

   private static void cacheActionBarHealth(Player ent, float health) {
      UUID uuid = ent.getUUID();
      ACTION_BAR_HEALTH_CACHE.put(uuid, health);
      ACTION_BAR_HEALTH_CACHE_TIME.put(uuid, System.currentTimeMillis());
   }

   private static Float getCachedActionBarHealth(Player ent) {
      UUID uuid = ent.getUUID();
      Long lastUpdate = ACTION_BAR_HEALTH_CACHE_TIME.get(uuid);
      if (lastUpdate == null) {
         return null;
      } else if (System.currentTimeMillis() - lastUpdate > 1500L) {
         ACTION_BAR_HEALTH_CACHE.remove(uuid);
         ACTION_BAR_HEALTH_CACHE_TIME.remove(uuid);
         return null;
      } else {
         return ACTION_BAR_HEALTH_CACHE.get(uuid);
      }
   }

   private static void clearActionBarHealthCache() {
      ACTION_BAR_HEALTH_CACHE.clear();
      ACTION_BAR_HEALTH_CACHE_TIME.clear();
   }

   private static FixHP.Mode getMode() {
      return INSTANCE == null ? FixHP.Mode.Scoreboard : INSTANCE.mode.getValue();
   }

   public enum Mode {
      Scoreboard,
      ActionBar;
   }
}
