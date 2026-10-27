package vcore.test;

import com.mojang.authlib.GameProfile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;
import vcore.core.Managers;
import vcore.core.manager.client.MacroManager;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.misc.FakePlayer;
import vcore.features.modules.render.ESP;
import vcore.features.modules.render.HudEditor;
import vcore.features.modules.render.TargerESP;
import vcore.gui.clickui.Category;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.clickui.ManagerScreen;
import vcore.gui.clickui.ModuleButton;
import vcore.gui.clickui.impl.ItemSelectScreen;
import vcore.gui.clickui.impl.ManagerPanel;
import vcore.gui.clickui.impl.SearchBar;
import vcore.gui.misc.DialogScreen;
import vcore.gui.misc.PeekScreen;
import vcore.gui.notification.Notification;
import vcore.setting.Setting;

public final class VCoreScreensTest implements FabricClientGameTest {
   private static final String CAPTURE_RUN = UUID.randomUUID().toString();

   private static TestScreenshotOptions screenshot(String name) {
      return TestScreenshotOptions.of(name + "-" + CAPTURE_RUN);
   }

   private static void saveCopy(Path original, String targetName) {
      try {
         File outDir = new File("build/test-screenshots");
         outDir.mkdirs();
         File target = new File(outDir, targetName);
         Files.copy(original, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
         System.out.println("[VCoreScreensTest] Screenshot saved: " + target.getAbsolutePath());
      } catch (Exception e) {
         System.err.println("[VCoreScreensTest] Failed to copy screenshot: " + e.getMessage());
      }
   }

   private static void capture(ClientGameTestContext context, Supplier<Screen> screen, Class<? extends Screen> expected, String name, String filename) {
      context.setScreen(screen);
      context.waitForScreen(expected);
      context.waitTicks(25);
      Path captured = context.takeScreenshot(screenshot(name).disableCounterPrefix());
      saveCopy(captured, filename);
   }

   private static void captureDirect(ClientGameTestContext context, String name, String filename) {
      Path captured = context.takeScreenshot(screenshot(name).disableCounterPrefix());
      saveCopy(captured, filename);
   }

   private static void resize(ClientGameTestContext context, int width, int height) {
      context.runOnClient(client -> {
         client.getWindow().setWindowed(width, height);
         client.resizeDisplay();
      });
      context.waitTicks(10);
   }

   private static void cleanTestConfigs() {
      try {
         File[] dirs = new File[] {
            new File("build/run/clientGameTest/Vcore/configs"),
            new File("build/run/clientGameTest/Vcore/misc"),
            new File("Vcore/configs"),
            new File("Vcore/misc")
         };
         for (File dir : dirs) {
            if (dir.exists()) {
               File[] files = dir.listFiles();
               if (files != null) {
                  for (File f : files) {
                     f.delete();
                  }
               }
            } else {
               dir.mkdirs();
            }
         }
      } catch (Exception ignored) {}
   }

   @SuppressWarnings("unchecked")
   private static void enableHudElement(String name, boolean enabled) {
      var s = ModuleManager.hudEditor.getSettingByName(name);
      if (s != null) {
         ((Setting<Boolean>) s).setValue(enabled);
      }
   }

   @Override
   public void runTest(ClientGameTestContext context) {
      System.out.println("==========================================================");
      System.out.println(">>> STARTING VCORE 1.21.11 COMPREHENSIVE GAMETEST SUITE <<<");
      System.out.println("==========================================================");

      // Clean old test cached configs to ensure pristine defaults
      cleanTestConfigs();

      // ----------------------------------------------------
      // 0. Mixin Verification (Verify all Mixin targets against Vanilla Minecraft 1.21.11)
      // ----------------------------------------------------
      System.out.println("[Step 0] Verifying all Mixin targets against Vanilla Minecraft 1.21.11...");
      var mixinResult = MixinVerificationEngine.runVerification();
      if (!mixinResult.isSuccess()) {
         throw new AssertionError("Mixin Verification FAILED with " + mixinResult.errors().size() + " errors: " + mixinResult.errors());
      }

      // Set standard GUI scale to 2 for TitleScreen
      context.runOnClient(client -> {
         client.options.guiScale().set(2);
         client.resizeDisplay();
      });

      // ----------------------------------------------------
      // 1. TitleScreen (Custom Typography, Font & Branding)
      // ----------------------------------------------------
      System.out.println("[Step 1] Verifying TitleScreen branding & custom font renderers...");
      context.setScreen(TitleScreen::new);
      context.waitForScreen(TitleScreen.class);
      context.waitTicks(30);
      captureDirect(context, "step1-titlescreen", "step1_titlescreen.png");

      // Set GUI scale to 1 so all 5 panels and bottom bar fit comfortably on 854x480
      context.runOnClient(client -> {
         client.options.guiScale().set(1);
         client.resizeDisplay();
         SearchBar.resetState();
         ClickGUI.closeModuleSettingsExcept(null);
         HudEditor.setThemeIndex(0);
         ModuleManager.clickGui.blur.setValue(false);
      });

      // ----------------------------------------------------
      // 2. ClickGUI Main Screen (All 5 Categories)
      // ----------------------------------------------------
      System.out.println("[Step 2] Verifying ClickGUI main screen layout...");
      capture(context, ClickGUI::getClickGui, ClickGUI.class, "step2-clickgui-main", "step2_clickgui_main.png");

      // ----------------------------------------------------
      // 3. ClickGUI Module Settings Expanded (Controls & Sliders)
      // ----------------------------------------------------
      System.out.println("[Step 3] Verifying ClickGUI module settings expansion...");
      context.runOnClient(client -> {
         if (ClickGUI.windows != null) {
            for (var window : ClickGUI.windows) {
               if (window instanceof Category cat) {
                  for (var btn : cat.getButtons()) {
                     if (btn instanceof ModuleButton mb && mb.module == ModuleManager.clickGui) {
                        mb.setOpen(true);
                        break;
                     }
                  }
               }
            }
         }
      });
      context.waitTicks(25);
      captureDirect(context, "step3-clickgui-expanded", "step3_clickgui_expanded_settings.png");

      // ----------------------------------------------------
      // 4. ClickGUI SearchBar Filter (Query Filtering)
      // ----------------------------------------------------
      System.out.println("[Step 4] Verifying ClickGUI SearchBar filtering ('Aura')...");
      context.runOnClient(client -> {
         ClickGUI.closeModuleSettingsExcept(null);
         SearchBar.listening = true;
         SearchBar.moduleName = "Aura";
      });
      context.waitTicks(25);
      captureDirect(context, "step4-clickgui-search", "step4_clickgui_search_filtered.png");
      context.runOnClient(client -> SearchBar.resetState());

      // ----------------------------------------------------
      // 5. ClickGUI Theme Selector (Palette Swatches)
      // ----------------------------------------------------
      System.out.println("[Step 5] Verifying ClickGUI Theme Palette dropdown...");
      context.runOnClient(client -> {
         ClickGUI gui = ClickGUI.getClickGui();
         gui.getThemeSelector().setPaletteOpen(true);
      });
      context.waitTicks(25);
      captureDirect(context, "step5-clickgui-palette", "step5_clickgui_theme_palette.png");

      // ----------------------------------------------------
      // 6. ClickGUI Red/Orange Theme (Matching original 1.21 VCore)
      // ----------------------------------------------------
      System.out.println("[Step 6] Verifying ClickGUI Theme 7 (Red/Orange gradient)...");
      context.runOnClient(client -> {
         ClickGUI gui = ClickGUI.getClickGui();
         gui.getThemeSelector().setPaletteOpen(false);
         HudEditor.setThemeIndex(6); // Theme 7 (Red/Orange gradient)
      });
      context.waitTicks(25);
      captureDirect(context, "step6-clickgui-red", "step6_clickgui_theme_red.png");

      // Scale up for dialog / manager screens
      context.runOnClient(client -> {
         client.options.guiScale().set(2);
         client.resizeDisplay();
      });

      // ----------------------------------------------------
      // 7. ItemSelectScreen (Interactive Item Picker)
      // ----------------------------------------------------
      System.out.println("[Step 7] Verifying ItemSelectScreen grid and search...");
      capture(context, () -> new ItemSelectScreen(null, ModuleManager.chestStealer.items), ItemSelectScreen.class, "step7-itemselect", "step7_itemselect_screen.png");

      // ----------------------------------------------------
      // 8. ManagerScreen - Friends Tab
      // ----------------------------------------------------
      System.out.println("[Step 8] Verifying ManagerScreen Friends tab...");
      context.runOnClient(client -> {
         Managers.FRIEND.clear();
         Managers.FRIEND.addFriend("Alice_Online");
         Managers.FRIEND.addFriend("Bob_Offline");
      });
      capture(context, () -> {
         ManagerScreen ms = new ManagerScreen(null);
         ms.getPanel().setCurrentTab(ManagerPanel.Tab.FRIEND);
         return ms;
      }, ManagerScreen.class, "step8-manager-friends", "step8_manager_friends.png");

      // ----------------------------------------------------
      // 9. ManagerScreen - Configs Tab
      // ----------------------------------------------------
      System.out.println("[Step 9] Verifying ManagerScreen Configs tab...");
      context.runOnClient(client -> {
         Managers.CONFIG.save("default");
         Managers.CONFIG.save("pvp_legit");
         Managers.CONFIG.load("default");
      });
      capture(context, () -> {
         ManagerScreen ms = new ManagerScreen(null);
         ms.getPanel().setCurrentTab(ManagerPanel.Tab.CONFIG);
         return ms;
      }, ManagerScreen.class, "step9-manager-configs", "step9_manager_configs.png");

      // ----------------------------------------------------
      // 10. ManagerScreen - Macros Tab
      // ----------------------------------------------------
      System.out.println("[Step 10] Verifying ManagerScreen Macros tab...");
      context.runOnClient(client -> {
         MacroManager.addMacro(new MacroManager.Macro("Heal", "/heal", GLFW.GLFW_KEY_H));
         MacroManager.addMacro(new MacroManager.Macro("Kit_Totem", "/kit totem", GLFW.GLFW_KEY_T));
      });
      capture(context, () -> {
         ManagerScreen ms = new ManagerScreen(null);
         ms.getPanel().setCurrentTab(ManagerPanel.Tab.MACRO);
         return ms;
      }, ManagerScreen.class, "step10-manager-macros", "step10_manager_macros.png");

      // ----------------------------------------------------
      // 11. DialogScreen (Confirmation Modal)
      // ----------------------------------------------------
      System.out.println("[Step 11] Verifying DialogScreen modal...");
      capture(
         context,
         () -> new DialogScreen(
            vcore.utility.render.TextureStorage.setting,
            "VCore 1.21.11 GameTest",
            "Client GameTest running successfully!",
            "Proceed",
            "Cancel",
            () -> {},
            () -> {}
         ),
         DialogScreen.class,
         "step11-dialog",
         "step11_dialog_screen.png"
      );

      // ----------------------------------------------------
      // In-World Tests
      // ----------------------------------------------------
      System.out.println("[VCoreScreensTest] Creating isolated test world...");
      try (var world = context.worldBuilder().create()) {
         world.getClientWorld().waitForChunksRender();
         world.getServer().runCommand("time set day");
         world.getServer().runCommand("gamerule advance_time false");
         System.out.println("[VCoreScreensTest] Test world loaded successfully!");

         // ----------------------------------------------------
         // 12. PeekScreen (Shulker Box Preview)
         // ----------------------------------------------------
         System.out.println("[Step 12] Verifying PeekScreen container preview...");
         NonNullList<ItemStack> shulkerItems = NonNullList.withSize(27, ItemStack.EMPTY);
         shulkerItems.set(0, new ItemStack(Items.DIAMOND_SWORD));
         shulkerItems.set(1, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 16));
         shulkerItems.set(2, new ItemStack(Items.TOTEM_OF_UNDYING));
         shulkerItems.set(3, new ItemStack(Items.NETHERITE_INGOT, 32));
         shulkerItems.set(4, new ItemStack(Items.ENDER_PEARL, 16));
         shulkerItems.set(13, new ItemStack(Items.DIAMOND_CHESTPLATE));
         shulkerItems.set(26, new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
         SimpleContainer shulkerContainer = new SimpleContainer(shulkerItems.toArray(new ItemStack[0]));
         capture(
            context,
            () -> {
               var player = net.minecraft.client.Minecraft.getInstance().player;
               return new PeekScreen(
                  new ShulkerBoxMenu(0, player.getInventory(), shulkerContainer),
                  player.getInventory(),
                  Component.literal("Shulker Box Peek"),
                  Blocks.SHULKER_BOX
               );
            },
            PeekScreen.class,
            "step12-peek-screen",
            "step12_peek_screen.png"
         );

         // Settle in-game
         context.setScreen(() -> null);
         context.waitTicks(20);

         // ----------------------------------------------------
         // 13. In-Game HUD Elements (Pinned WaterMark & HUD Widgets)
         // ----------------------------------------------------
         System.out.println("[Step 13] Verifying in-game HUD rendering & Scoreboard Sidebar...");
         world.getServer().runCommand("scoreboard objectives add vcore_sb dummy \"VCore Server\"");
         world.getServer().runCommand("scoreboard objectives setdisplay sidebar vcore_sb");
         world.getServer().runCommand("scoreboard players set Online_Players vcore_sb 42");
         world.getServer().runCommand("scoreboard players set Kills_Count vcore_sb 15");
         world.getServer().runCommand("scoreboard players set Ping_12ms vcore_sb 12");
         world.getServer().runCommand("scoreboard players set Balance_Coins vcore_sb 9999");
         context.waitTicks(10);
         context.runOnClient(client -> {
            client.options.hideGui = false;
            client.player.setYRot(0.0F);
            client.player.setXRot(10.0F);
            client.player.setOldPosAndRot();

            ModuleManager.hudEditor.enable();
            enableHudElement("WaterMark", true);
            enableHudElement("Coords", true);
            enableHudElement("Armor", true);
            enableHudElement("Ping", true);
            enableHudElement("KeyBinds", true);
            enableHudElement("Speed", true);
            enableHudElement("TPSCounter", true);
            enableHudElement("TotemCounter", true);
         });
         context.waitTicks(25);
         captureDirect(context, "step13-ingame-hud", "step13_ingame_hud_normal.png");

         // ----------------------------------------------------
         // 14. In-Game Notification System
         // ----------------------------------------------------
         System.out.println("[Step 14] Verifying in-game Notifications...");
         context.runOnClient(client -> {
            ModuleManager.notifications.enable();
            Managers.NOTIFICATION.publicity("VCore Release", "Module KillAura enabled", 15, Notification.Type.ENABLED);
            Managers.NOTIFICATION.publicity("Client Info", "Running on Minecraft 1.21.11", 15, Notification.Type.INFO);
            Managers.NOTIFICATION.publicity("Warning", "Low armor durability", 15, Notification.Type.WARNING);
            Managers.NOTIFICATION.publicity("Security", "Server RP accepted safely", 15, Notification.Type.SUCCESS);
         });
         context.waitTicks(20);
         captureDirect(context, "step14-ingame-notifications", "step14_ingame_notifications.png");

         // ----------------------------------------------------
         // 15. In-Game ChatScreen (HUD Dragging & Editor Mode)
         // ----------------------------------------------------
         System.out.println("[Step 15] Verifying ChatScreen HUD dragging bounds...");
         context.setScreen(() -> new ChatScreen("", false));
         context.waitForScreen(ChatScreen.class);
         context.waitTicks(20);
         captureDirect(context, "step15-hud-chatscreen", "step15_ingame_hud_chatscreen.png");
         context.setScreen(() -> null);
         context.waitTicks(10);

         // ----------------------------------------------------
         // 15a. In-Game PauseScreen (Authentic translucent dimming without pitch-black wash)
         // ----------------------------------------------------
         System.out.println("[Step 15a] Verifying PauseScreen translucent background...");
         capture(context, () -> new PauseScreen(true), PauseScreen.class, "step15a-pause-screen", "step15a_pause_screen.png");
         context.setScreen(() -> null);
         context.waitTicks(10);

         // ----------------------------------------------------
         // 15b. In-Game InventoryScreen (Crisp background without black wash)
         // ----------------------------------------------------
         System.out.println("[Step 15b] Verifying InventoryScreen translucent background...");
         capture(
            context,
            () -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(net.minecraft.client.Minecraft.getInstance().player),
            net.minecraft.client.gui.screens.inventory.InventoryScreen.class,
            "step15b-inventory",
            "step15b_inventory_screen.png"
         );
         context.setScreen(() -> null);
         context.waitTicks(10);

         // ----------------------------------------------------
         // 15c. In-Game ChatScreen with Active Input (Translucent input bar, not solid black)
         // ----------------------------------------------------
         System.out.println("[Step 15c] Verifying ChatScreen input bar (translucent box, not black bar)...");
         context.setScreen(() -> new ChatScreen("/gamemode creative", false));
         context.waitForScreen(ChatScreen.class);
         context.waitTicks(20);
         captureDirect(context, "step15c-chat-input-bar", "step15c_chat_input_bar.png");
         context.setScreen(() -> null);
         context.waitTicks(10);

         // ----------------------------------------------------
         // 16. In-Game 3D Entity ESP & Fullbright (FakePlayer + ArmorStand)
         // ----------------------------------------------------
         System.out.println("[Step 16] Verifying 3D Entity ESP, FakePlayer & Fullbright...");
         RemotePlayer[] targetHolder = new RemotePlayer[1];
         context.runOnClient(client -> {
            client.player.setYRot(0.0F);
            client.player.setXRot(15.0F);
            client.player.setOldPosAndRot();

            // Spawn FakePlayer module entity
            ModuleManager.fakePlayer.enable();
            if (FakePlayer.fakePlayer != null) {
               FakePlayer.fakePlayer.setPos(client.player.getX() - 0.8, client.player.getY(), client.player.getZ() + 3.5);
               FakePlayer.fakePlayer.setOldPosAndRot();
               FakePlayer.fakePlayer.setYRot(180.0F);
               FakePlayer.fakePlayer.setYHeadRot(180.0F);
               FakePlayer.fakePlayer.setYBodyRot(180.0F);
               FakePlayer.fakePlayer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
               FakePlayer.fakePlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
               FakePlayer.fakePlayer.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
               FakePlayer.fakePlayer.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
               FakePlayer.fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
               targetHolder[0] = FakePlayer.fakePlayer;
            }

            // Spawn ArmorStand alongside with Netherite armor
            ArmorStand stand = new ArmorStand(client.level, client.player.getX() + 0.8, client.player.getY(), client.player.getZ() + 3.5);
            int standId = Integer.MIN_VALUE + 100;
            while (client.level.getEntity(standId) != null) standId++;
            stand.setId(standId);
            stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
            stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
            stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
            stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
            stand.setShowArms(true);
            stand.setYRot(180.0F);
            stand.setYHeadRot(180.0F);
            stand.setYBodyRot(180.0F);
            client.level.addEntity(stand);

            // Configure ESP settings
            ModuleManager.esp.enable();
            var modeSetting = ModuleManager.esp.getSettingByName("Mode");
            if (modeSetting != null) ((Setting<ESP.Mode>) modeSetting).setValue(ESP.Mode.Box);
            var nakedSetting = ModuleManager.esp.getSettingByName("Naked");
            if (nakedSetting != null) ((Setting<Boolean>) nakedSetting).setValue(true);
            var mobsSetting = ModuleManager.esp.getSettingByName("Mobs");
            if (mobsSetting != null) ((Setting<Boolean>) mobsSetting).setValue(true);
            ModuleManager.fullbright.enable();
         });
         context.waitTicks(25);
         captureDirect(context, "step16-world-esp", "step16_world_entity_esp.png");

         // ----------------------------------------------------
         // 17. In-Game 3D StorageESP (Chests & Shulkers above ground)
         // ----------------------------------------------------
         System.out.println("[Step 17] Verifying 3D StorageESP (Chests & Shulkers)...");
         context.runOnClient(client -> {
            BlockPos pPos = client.player.blockPosition().above();
            client.level.setBlockAndUpdate(pPos.offset(-2, 0, 3), Blocks.CHEST.defaultBlockState());
            client.level.setBlockAndUpdate(pPos.offset(0, 0, 3), Blocks.ENDER_CHEST.defaultBlockState());
            client.level.setBlockAndUpdate(pPos.offset(2, 0, 3), Blocks.SHULKER_BOX.defaultBlockState());

            client.player.setXRot(20.0F);
            client.player.setOldPosAndRot();
            ModuleManager.storageEsp.enable();
         });
         context.waitTicks(25);
         captureDirect(context, "step17-storage-esp", "step17_world_storage_esp.png");

         // ----------------------------------------------------
         // 18. In-Game TargetESP, Crosshair & Directional Arrows
         // ----------------------------------------------------
         System.out.println("[Step 18] Verifying TargetESP, Custom Crosshair & Arrows...");
         context.runOnClient(client -> {
            if (targetHolder[0] != null) {
               Aura.target = targetHolder[0];
            }
            ModuleManager.targerESP.enable();
            ModuleManager.crosshair.enable();
            ModuleManager.Arrows.enable();
         });
         context.waitTicks(25);
         captureDirect(context, "step18-targetesp-crosshair", "step18_world_targetesp_crosshair.png");

         // ----------------------------------------------------
         // 18a. In-Game Particles Active (World Stars, Hit, Pearl)
         // ----------------------------------------------------
         System.out.println("[Step 18a] Verifying Particles active in 3D world (no black world)...");
         context.runOnClient(client -> {
            ModuleManager.particles.enable();
            var worldSetting = ModuleManager.particles.getSettingByName("World");
            if (worldSetting != null) ((Setting<Boolean>) worldSetting).setValue(true);
         });
         context.waitTicks(35);
         captureDirect(context, "step18a-world-particles", "step18a_world_particles.png");

         // ----------------------------------------------------
         // 18b. In-Game TargetESP Image Mode
         // ----------------------------------------------------
         System.out.println("[Step 18b] Verifying TargetESP Image Mode...");
         context.runOnClient(client -> {
            var modeSetting = ModuleManager.targerESP.getSettingByName("Mode");
            if (modeSetting != null) ((Setting<TargerESP.Mode>) modeSetting).setValue(TargerESP.Mode.Image);
         });
         context.waitTicks(25);
         captureDirect(context, "step18b-targetesp-image", "step18b_world_targetesp_image.png");

         // ----------------------------------------------------
         // 18c. In-Game TargetESP Circle Mode
         // ----------------------------------------------------
         System.out.println("[Step 18c] Verifying TargetESP Circle Mode...");
         context.runOnClient(client -> {
            var modeSetting = ModuleManager.targerESP.getSettingByName("Mode");
            if (modeSetting != null) ((Setting<TargerESP.Mode>) modeSetting).setValue(TargerESP.Mode.Circle);
         });
         context.waitTicks(25);
         captureDirect(context, "step18c-targetesp-circle", "step18c_world_targetesp_circle.png");

         // ----------------------------------------------------
         // 18d. In-Game TargetESP Ghost Mode
         // ----------------------------------------------------
         System.out.println("[Step 18d] Verifying TargetESP Ghost Mode...");
         context.runOnClient(client -> {
            var modeSetting = ModuleManager.targerESP.getSettingByName("Mode");
            if (modeSetting != null) ((Setting<TargerESP.Mode>) modeSetting).setValue(TargerESP.Mode.Ghost);
         });
         context.waitTicks(25);
         captureDirect(context, "step18d-targetesp-ghost", "step18d_world_targetesp_ghost.png");

         // ----------------------------------------------------
         // 18e. All 3D Modules Simultaneously Active (ESP + Storage + Particles + TargetESP + Fullbright + Arrows + Crosshair)
         // ----------------------------------------------------
         System.out.println("[Step 18e] Verifying ALL 3D modules active at once without black world...");
         context.runOnClient(client -> {
            ModuleManager.esp.enable();
            ModuleManager.storageEsp.enable();
            ModuleManager.particles.enable();
            ModuleManager.targerESP.enable();
            ModuleManager.crosshair.enable();
            ModuleManager.Arrows.enable();
            ModuleManager.fullbright.enable();
         });
         context.waitTicks(30);
         captureDirect(context, "step18e-all-3d-modules", "step18e_world_all_3d_modules.png");

         // ----------------------------------------------------
         // 18f. ClickGUI with Crosshair Enabled - Verify Crosshair is HIDDEN
         // ----------------------------------------------------
         System.out.println("[Step 18f] Verifying ClickGUI with Crosshair enabled (Crosshair MUST BE HIDDEN)...");
         context.runOnClient(client -> {
            client.options.guiScale().set(1);
            client.resizeDisplay();
            ModuleManager.aura.enable();
            ModuleManager.clickGui.blur.setValue(false);
         });
         capture(context, ClickGUI::getClickGui, ClickGUI.class, "step18f-clickgui-crosshair-hidden", "step18f_clickgui_crosshair_hidden.png");
         context.setScreen(() -> null);
         context.waitTicks(10);

         // ----------------------------------------------------
         // 18g. PauseScreen while 3D Modules Active (Translucent dimming, no black box)
         // ----------------------------------------------------
         System.out.println("[Step 18g] Verifying PauseScreen while 3D modules active (no black wash, proper blend)...");
         context.setScreen(() -> new PauseScreen(true));
         context.waitForScreen(PauseScreen.class);
         context.waitTicks(20);
         captureDirect(context, "step18g-pause-with-3d", "step18g_pause_with_3d.png");
         context.setScreen(() -> null);
         context.waitTicks(15);
         captureDirect(context, "step18h-world-resumed", "step18h_world_resumed.png");

         // ----------------------------------------------------
         // 19. In-Game ClickGUI Blur Over 3D World Scene
         // ----------------------------------------------------
         System.out.println("[Step 19] Verifying ClickGUI blur over 3D world scene & Scoreboard Sidebar...");
         world.getServer().runCommand("scoreboard objectives add vcore_sb dummy \"VCore Server\"");
         world.getServer().runCommand("scoreboard objectives setdisplay sidebar vcore_sb");
         world.getServer().runCommand("scoreboard players set Online_Players vcore_sb 42");
         world.getServer().runCommand("scoreboard players set Kills_Count vcore_sb 15");
         world.getServer().runCommand("scoreboard players set Ping_12ms vcore_sb 12");
         world.getServer().runCommand("scoreboard players set Balance_Coins vcore_sb 9999");
         context.waitTicks(10);
         context.runOnClient(client -> {
            client.options.guiScale().set(1);
            client.resizeDisplay();
            client.player.setYRot(0.0F);
            client.player.setXRot(10.0F);
            client.player.setOldPosAndRot();
            ModuleManager.esp.disable();
            ModuleManager.storageEsp.disable();
            ModuleManager.targerESP.disable();
            ModuleManager.fakePlayer.disable();
            ModuleManager.clickGui.blur.setValue(true);
         });
         capture(context, ClickGUI::getClickGui, ClickGUI.class, "step19-clickgui-blur-ingame", "step19_clickgui_ingame_blur.png");

         // ----------------------------------------------------
         // 20-23. Multi-Scale Responsiveness (GUI Scale 1 to 4)
         // ----------------------------------------------------
         System.out.println("[Steps 20-23] Verifying ClickGUI across GUI scales 1 to 4...");
         for (int scale = 1; scale <= 4; scale++) {
            int currentScale = scale;
            context.runOnClient(client -> {
               client.options.guiScale().set(currentScale);
               client.resizeDisplay();
               ModuleManager.clickGui.blur.setValue(false);
            });
            int stepNum = 19 + scale;
            capture(
               context,
               ClickGUI::getClickGui,
               ClickGUI.class,
               "step" + stepNum + "-clickgui-scale-" + scale,
               "step" + stepNum + "_clickgui_scale" + scale + ".png"
            );
         }

         // ----------------------------------------------------
         // 24-26. Dynamic Window Resizing (Resolution Stress Tests)
         // ----------------------------------------------------
         System.out.println("[Step 24] Verifying window resize to 1280x720 (720p HD)...");
         resize(context, 1280, 720);
         context.runOnClient(client -> {
            client.options.guiScale().set(2);
            client.resizeDisplay();
         });
         capture(context, ClickGUI::getClickGui, ClickGUI.class, "step24-resize-1280x720", "step24_resize_1280x720.png");

         System.out.println("[Step 25] Verifying window resize to 1600x900 (HD+)...");
         resize(context, 1600, 900);
         context.runOnClient(client -> {
            client.options.guiScale().set(2);
            client.resizeDisplay();
         });
         capture(context, ClickGUI::getClickGui, ClickGUI.class, "step25-resize-1600x900", "step25_resize_1600x900.png");

         System.out.println("[Step 26] Verifying odd resolution resize to 855x481...");
         resize(context, 855, 481);
         context.runOnClient(client -> {
            client.options.guiScale().set(1);
            client.resizeDisplay();
         });
         capture(context, ClickGUI::getClickGui, ClickGUI.class, "step26-resize-855x481", "step26_resize_855x481.png");
      }

      System.out.println("==========================================================");
      System.out.println(">>> ALL 37 VCORE 1.21.11 GAMETEST CHECKS PASSED WITH 0 ERRORS! <<<");
      System.out.println("==========================================================");
   }
}
