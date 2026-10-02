package vcore.features.modules;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.CommandManager;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.misc.ClientSettings;
import vcore.features.modules.misc.UnHook;
import vcore.gui.notification.Notification;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;

public abstract class Module {
   private final Setting<Bind> bind = new Setting<>("Keybind", new Bind(-1, false, false));
   private final Setting<Boolean> enabled = new Setting<>("Enabled", false);
   private final String description;
   private final Module.Category category;
   private final String displayName;
   private static boolean silentToggle = false;
   private final List<String> ignoreSoundList = Arrays.asList("ClickGui", "HudEditor");
   public static final Minecraft mc = Minecraft.getInstance();

   public Module(@NotNull String name, @NotNull Module.Category category) {
      this(name, "No description provided.", category);
   }

   public Module(@NotNull String name, @NotNull String description, @NotNull Module.Category category) {
      this.displayName = name;
      this.description = description;
      this.category = category;
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void onLogin() {
   }

   public void onLogout() {
   }

   public void onUpdate() {
   }

   public void onRender2D(GuiGraphics event) {
   }

   public void onRender3D(PoseStack event) {
   }

   public void onUnload() {
   }

   public boolean isToggleable() {
      return true;
   }

   protected void sendPacket(Packet<?> packet) {
      if (mc.getConnection() != null) {
         mc.getConnection().send(packet);
      }
   }

   protected void sendPacketSilent(Packet<?> packet) {
      if (mc.getConnection() != null) {
         Vcore.core.silentPackets.add(packet);
         Vcore.core.silentPostPackets.add(packet);
         mc.getConnection().send(packet);
      }
   }

   protected void sendSequencedPacket(PredictiveAction packetCreator) {
      if (mc.getConnection() != null && mc.level != null) {
         BlockStatePredictionHandler pendingUpdateManager = mc.level.getBlockStatePredictionHandler().startPredicting();

         try {
            int i = pendingUpdateManager.currentSequence();
            mc.getConnection().send(packetCreator.predict(i));
         } catch (Throwable var6) {
            if (pendingUpdateManager != null) {
               try {
                  pendingUpdateManager.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (pendingUpdateManager != null) {
            pendingUpdateManager.close();
         }
      }
   }

   public String getDisplayInfo() {
      return null;
   }

   public boolean isOn() {
      return this.enabled.getValue();
   }

   public boolean isOff() {
      return !this.enabled.getValue();
   }

   public void setEnabled(boolean enabled) {
      this.enabled.setValue(enabled);
   }

   public void onThread() {
   }

   public void enable() {
      if (!(this instanceof UnHook)) {
         this.enabled.setValue(true);
      }

      if (!fullNullCheck() || this instanceof UnHook) {
         this.onEnable();
      }

      if (this.isOn()) {
         Vcore.EVENT_BUS.subscribe(this);
      }

      if (!fullNullCheck()) {
         if (!silentToggle && !this.ignoreSoundList.contains(this.getDisplayName())) {
            Managers.NOTIFICATION.publicity(this.getDisplayName(), "Was Enabled!", 2, Notification.Type.ENABLED);
            Managers.SOUND.playEnable();
         }
      }
   }

   public void disable(String reason) {
      this.sendMessage(reason);
      this.disable();
   }

   public void disable() {
      try {
         Vcore.EVENT_BUS.unsubscribe(this);
      } catch (Exception var2) {
      }

      this.enabled.setValue(false);
      if (!fullNullCheck()) {
         this.onDisable();
         if (!silentToggle && !this.ignoreSoundList.contains(this.getDisplayName())) {
            Managers.NOTIFICATION.publicity(this.getDisplayName(), "Was Disabled!", 2, Notification.Type.DISABLED);
            Managers.SOUND.playDisable();
         }
      }
   }

   public void enableSilently() {
      boolean prev = silentToggle;
      silentToggle = true;

      try {
         this.enable();
      } finally {
         silentToggle = prev;
      }
   }

   public void disableSilently() {
      boolean prev = silentToggle;
      silentToggle = true;

      try {
         this.disable();
      } finally {
         silentToggle = prev;
      }
   }

   public void toggle() {
      if (this.enabled.getValue()) {
         this.disable();
      } else {
         this.enable();
      }
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getDescription() {
      return this.description;
   }

   public Module.Category getCategory() {
      return this.category;
   }

   public Bind getBind() {
      return this.bind.getValue();
   }

   public void setBind(int key, boolean mouse, boolean hold) {
      this.setBind(new Bind(key, mouse, hold));
   }

   public void setBind(Bind b) {
      this.bind.setValue(b);
   }

   public boolean listening() {
      return this.isOn();
   }

   public static boolean fullNullCheck() {
      return mc == null || mc.player == null || mc.level == null || (ModuleManager.unHook != null && ModuleManager.unHook.isEnabled());
   }

   public String getName() {
      return this.getDisplayName();
   }

   private List<Setting<?>> cachedSettings = null;

   public synchronized <T extends Setting<?>> T registerSetting(T setting) {
      setting.setModule(this);
      if (this.cachedSettings == null) {
         this.getSettings();
      }
      this.cachedSettings.add(setting);
      return setting;
   }

   public synchronized List<Setting<?>> getSettings() {
      if (this.cachedSettings != null) {
         return new ArrayList<>(this.cachedSettings);
      }
      ArrayList<Setting<?>> settingList = new ArrayList<>();
      IdentityHashMap<Setting<?>, Integer> settingDepth = new IdentityHashMap<>();
      Class<?> currentSuperclass = this.getClass();

      for (int depth = 0; currentSuperclass != null; depth++) {
         for (Field field : currentSuperclass.getDeclaredFields()) {
            if (Setting.class.isAssignableFrom(field.getType())) {
               try {
                  field.setAccessible(true);
                  Setting<?> setting = (Setting<?>)field.get(this);
                  if (setting != null) {
                     settingList.add(setting);
                     settingDepth.put(setting, depth);
                  }
               } catch (IllegalAccessException error) {
                  Vcore.LOGGER.warn(error.getMessage());
               }
            }
         }

         currentSuperclass = currentSuperclass.getSuperclass();
      }

      settingList.sort(
         Comparator.<Setting<?>>comparingInt(setting -> settingDepth.getOrDefault(setting, Integer.MAX_VALUE)).thenComparingLong(Setting::getCreationOrder)
      );
      settingList.forEach(s -> s.setModule(this));
      this.cachedSettings = new ArrayList<>(settingList);
      return new ArrayList<>(this.cachedSettings);
   }

   public boolean isEnabled() {
      return this.isOn();
   }

   public boolean isDisabled() {
      return !this.isEnabled();
   }

   public static void clickSlot(int id) {
      if (id != -1 && mc.gameMode != null && mc.player != null) {
         mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, id, 0, ClickType.PICKUP, mc.player);
      }
   }

   public static void clickSlot(int id, ClickType type) {
      if (id != -1 && mc.gameMode != null && mc.player != null) {
         mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, id, 0, type, mc.player);
      }
   }

   public static void clickSlot(int id, int button, ClickType type) {
      if (id != -1 && mc.gameMode != null && mc.player != null) {
         mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, id, button, type, mc.player);
      }
   }

   public void sendMessage(String message) {
      if (!fullNullCheck() && ClientSettings.clientMessages.getValue() && !ModuleManager.unHook.isEnabled()) {
         if (mc.isSameThread()) {
            mc.player
               .displayClientMessage(
                  Component.nullToEmpty(
                     CommandManager.getClientMessage()
                        + " "
                        + ChatFormatting.GRAY
                        + "["
                        + ChatFormatting.DARK_PURPLE
                        + this.getDisplayName()
                        + ChatFormatting.GRAY
                        + "] "
                        + message
                  ),
                  false
               );
         } else {
            mc.executeIfPossible(
               () -> mc.player
                  .displayClientMessage(
                     Component.nullToEmpty(
                        CommandManager.getClientMessage()
                           + " "
                           + ChatFormatting.GRAY
                           + "["
                           + ChatFormatting.DARK_PURPLE
                           + this.getDisplayName()
                           + ChatFormatting.GRAY
                           + "] "
                           + message
                     ),
                     false
                  )
            );
         }
      }
   }

   public void sendChatMessage(String message) {
      if (!fullNullCheck()) {
         mc.getConnection().sendChat(message);
      }
   }

   public void sendChatCommand(String command) {
      if (!fullNullCheck()) {
         mc.getConnection().sendCommand(command);
      }
   }

   public void debug(String message) {
      if (!fullNullCheck() && ClientSettings.debug.getValue()) {
         if (mc.isSameThread()) {
            mc.player
               .displayClientMessage(
                  Component.nullToEmpty(
                     CommandManager.getClientMessage()
                        + " "
                        + ChatFormatting.GRAY
                        + "["
                        + ChatFormatting.DARK_PURPLE
                        + this.getDisplayName()
                        + ChatFormatting.GRAY
                        + "] [\ud83d\udd27] "
                        + message
                  ),
                  false
               );
         } else {
            mc.executeIfPossible(
               () -> mc.player
                  .displayClientMessage(
                     Component.nullToEmpty(
                        CommandManager.getClientMessage()
                           + " "
                           + ChatFormatting.GRAY
                           + "["
                           + ChatFormatting.DARK_PURPLE
                           + this.getDisplayName()
                           + ChatFormatting.GRAY
                           + "] [\ud83d\udd27] "
                           + message
                     ),
                     false
                  )
            );
         }
      }
   }

   public boolean isKeyPressed(int button) {
      if (button != -1 && !ModuleManager.unHook.isEnabled()) {
         if (Managers.MODULE.activeMouseKeys.contains(button)) {
            Managers.MODULE.activeMouseKeys.clear();
            return true;
         } else {
            return button < 10 ? false : InputConstants.isKeyDown(mc.getWindow(), button);
         }
      } else {
         return false;
      }
   }

   public boolean isKeyPressed(Setting<Bind> bind) {
      return bind.getValue().getKey() != -1 && !ModuleManager.unHook.isEnabled() ? this.isKeyPressed(bind.getValue().getKey()) : false;
   }

   public boolean isBindDown(Setting<Bind> bind) {
      if (bind != null && bind.getValue() != null && bind.getValue().getKey() != -1 && !ModuleManager.unHook.isEnabled()) {
         Bind value = bind.getValue();
         return value.isMouse()
            ? GLFW.glfwGetMouseButton(mc.getWindow().handle(), value.getKey()) == 1
            : InputConstants.isKeyDown(mc.getWindow(), value.getKey());
      } else {
         return false;
      }
   }

   @Nullable
   public Setting<?> getSettingByName(String name) {
      for (Setting<?> setting : this.getSettings()) {
         if (setting.getName().equalsIgnoreCase(name)) {
            return setting;
         }
      }

      return null;
   }

   public static class Category {
      private final String name;
      private static final Map<String, Module.Category> CATEGORIES = new LinkedHashMap<>();
      public static final Module.Category COMBAT = new Module.Category("Combat");
      public static final Module.Category MISC = new Module.Category("Misc");
      public static final Module.Category RENDER = new Module.Category("Render");
      public static final Module.Category MOVEMENT = new Module.Category("Movement");
      public static final Module.Category PLAYER = new Module.Category("Player");

      private Category(String name) {
         this.name = name;
      }

      public String getName() {
         return this.name;
      }

      public static Module.Category getCategory(String name) {
         return CATEGORIES.computeIfAbsent(name, Module.Category::new);
      }

      public static Collection<Module.Category> values() {
         return CATEGORIES.values();
      }

      public static boolean isCustomCategory(Module.Category category) {
         Set<String> predefinedCategoryNames = Set.of("Combat", "Misc", "Render", "Movement", "Player");
         return !predefinedCategoryNames.contains(category.getName());
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            Module.Category category = (Module.Category)o;
            return Objects.equals(this.name, category.name);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.name);
      }

      static {
         CATEGORIES.put("Combat", COMBAT);
         CATEGORIES.put("Movement", MOVEMENT);
         CATEGORIES.put("Render", RENDER);
         CATEGORIES.put("Player", PLAYER);
         CATEGORIES.put("Misc", MISC);
      }
   }
}
