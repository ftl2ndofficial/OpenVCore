package vcore.features.modules.combat;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventEntityRemoved;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.features.modules.misc.FakePlayer;
import vcore.setting.Setting;

public final class AntiBot extends Module {
   public final Setting<AntiBot.Mode> mode = new Setting<>("Mode", AntiBot.Mode.Matrix);
   public final Setting<Boolean> enabledRemove = new Setting<>("Remove", true);
   public final Setting<Boolean> onlyAura = new Setting<>("OnlyAura", false);
   private static final Set<Integer> botEntityIds = ConcurrentHashMap.newKeySet();

   public AntiBot() {
      super("AntiBot", "Removes matrix bots.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      botEntityIds.clear();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      botEntityIds.clear();
   }

   @EventHandler
   public void onSync(EventSync event) {
      if (mc.level != null && this.mode.is(AntiBot.Mode.Matrix)) {
         if (!this.onlyAura.getValue() || ModuleManager.aura.isEnabled()) {
            this.scanMatrixPlayers();
            if (this.enabledRemove.getValue() && !botEntityIds.isEmpty()) {
               this.removeDetectedBots();
            }
         }
      }
   }

   @EventHandler
   public void onEntityRemoved(EventEntityRemoved event) {
      if (event != null && event.getEntity() != null) {
         if (event.getEntity() instanceof Player player) {
            botEntityIds.remove(player.getId());
         }
      }
   }

   private void scanMatrixPlayers() {
      for (Player player : List.copyOf(mc.level.players())) {
         if (this.canCheckPlayer(player)) {
            if (isConfirmedMatrixBot(player)) {
               this.addBot(player);
            } else {
               this.clearBot(player);
            }
         }
      }
   }

   private boolean canCheckPlayer(Player player) {
      return player != null
         && player != mc.player
         && player instanceof RemotePlayer
         && player.isAlive()
         && (FakePlayer.fakePlayer == null || player != FakePlayer.fakePlayer);
   }

   private static boolean isConfirmedMatrixBot(Player player) {
      return hasMatrixArmorFingerprint(player);
   }

   private static boolean hasMatrixArmorFingerprint(Player player) {
      return hasRenderedArmorItem(player) && player.getArmorValue() == 0;
   }

   private static boolean hasRenderedArmorItem(Player player) {
      return isArmorItem(player.getItemBySlot(EquipmentSlot.HEAD))
         || isArmorItem(player.getItemBySlot(EquipmentSlot.CHEST))
         || isArmorItem(player.getItemBySlot(EquipmentSlot.LEGS))
         || isArmorItem(player.getItemBySlot(EquipmentSlot.FEET));
   }

   private static boolean isArmorItem(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.has(DataComponents.EQUIPPABLE);
   }

   private void removeDetectedBots() {
      for (Player player : List.copyOf(mc.level.players())) {
         if (botEntityIds.contains(player.getId()) && (FakePlayer.fakePlayer == null || player != FakePlayer.fakePlayer)) {
            if (!isConfirmedMatrixBot(player)) {
               this.clearBot(player);
            } else {
               mc.level.removeEntity(player.getId(), RemovalReason.KILLED);
            }
         }
      }
   }

   private void addBot(Player player) {
      if (botEntityIds.add(player.getId())) {
         this.sendMessage(player.getName().getString() + " is a bot (Matrix)!");
      }
   }

   private void clearBot(Player player) {
      botEntityIds.remove(player.getId());
   }

   public static boolean isBot(Entity entity) {
      if (entity instanceof Player player) {
         return FakePlayer.fakePlayer != null && entity == FakePlayer.fakePlayer ? false : botEntityIds.contains(player.getId());
      } else {
         return false;
      }
   }

   @Override
   public String getDisplayInfo() {
      return String.valueOf(botEntityIds.size());
   }

   public enum Mode {
      Matrix;
   }
}
