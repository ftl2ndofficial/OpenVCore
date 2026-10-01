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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vcore.core.manager.client.ModuleManager;
import vcore.gui.clickui.Category;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.clickui.ManagerScreen;
import vcore.gui.clickui.impl.ItemSelectScreen;
import vcore.gui.misc.DialogScreen;
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
         System.out.println("[VCoreScreensTest] Screenshot copied to: " + target.getAbsolutePath());
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

   @Override
   public void runTest(ClientGameTestContext context) {
      System.out.println("==========================================================");
      System.out.println(">>> STARTING VCORE 1.21.11 CLIENT GAMETEST SUITE <<<");
      System.out.println("==========================================================");

      // Set GUI scale to 2 for consistent scaled rendering
      context.runOnClient(client -> client.options.guiScale().set(2));

      // 1. Capture TitleScreen
      context.setScreen(TitleScreen::new);
      context.waitForScreen(TitleScreen.class);
      context.waitTicks(40);
      Path p1 = context.takeScreenshot(screenshot("step1-titlescreen").disableCounterPrefix());
      saveCopy(p1, "step1_titlescreen.png");

      // 2. ClickGUI Main
      capture(context, ClickGUI::getClickGui, ClickGUI.class, "step2-clickgui-main", "step2_clickgui_main.png");

      // 3. Expand ClickGUI Category settings
      context.runOnClient(client -> {
         if (ClickGUI.windows != null) {
            for (var window : ClickGUI.windows) {
               if (window instanceof Category cat) {
                  cat.hudClicked(ModuleManager.clickGui);
                  break;
               }
            }
         }
      });
      context.waitTicks(25);
      Path p3 = context.takeScreenshot(screenshot("step3-clickgui-expanded").disableCounterPrefix());
      saveCopy(p3, "step3_clickgui_expanded.png");

      // 4. ItemSelectScreen
      capture(context, () -> new ItemSelectScreen(null, ModuleManager.chestStealer.items), ItemSelectScreen.class, "step4-itemselect", "step4_itemselect_screen.png");

      // 5. ManagerScreen
      capture(context, () -> new ManagerScreen(null), ManagerScreen.class, "step5-manager", "step5_manager_screen.png");

      // 6. DialogScreen
      capture(
         context,
         () -> new DialogScreen(
            Identifier.fromNamespaceAndPath("vcore", "textures/ui/mini_logo.png"),
            "VCore 1.21.11 GameTest",
            "Client GameTest running successfully!",
            "Proceed",
            "Cancel",
            () -> {},
            () -> {}
         ),
         DialogScreen.class,
         "step6-dialog",
         "step6_dialog_screen.png"
      );

      // 7. Enter isolated test world
      System.out.println("[VCoreScreensTest] Creating isolated test world...");
      try (var world = context.worldBuilder().create()) {
         world.getClientWorld().waitForChunksRender();
         System.out.println("[VCoreScreensTest] Test world loaded successfully!");

         // Close any screen, settle in-game
         context.setScreen(() -> null);
         context.waitTicks(20);

         // Spawn a remote player fixture with equipment
         context.runOnClient(client -> {
            var fixture = new RemotePlayer(client.level, new GameProfile(UUID.randomUUID(), "Test Target"));
            int id = Integer.MIN_VALUE;
            while (client.level.getEntity(id) != null) id++;
            fixture.setId(id);
            fixture.setPos(client.player.position().add(0, 0, 4));
            fixture.setOldPosAndRot();
            fixture.setNoGravity(true);
            fixture.setOnGround(true);
            fixture.setHealth(14);
            fixture.setAbsorptionAmount(4);
            fixture.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            fixture.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            fixture.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            fixture.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            fixture.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            client.level.addEntity(fixture);
         });
         context.waitTicks(10);

         // Enable HUD elements
         context.runOnClient(client -> {
            ModuleManager.hudEditor.enable();
            var wm = ModuleManager.hudEditor.getSettingByName("WaterMark");
            if (wm != null) ((Setting<Boolean>) wm).setValue(true);
            var cd = ModuleManager.hudEditor.getSettingByName("Coords");
            if (cd != null) ((Setting<Boolean>) cd).setValue(true);
            var ar = ModuleManager.hudEditor.getSettingByName("Armor");
            if (ar != null) ((Setting<Boolean>) ar).setValue(true);
            var pi = ModuleManager.hudEditor.getSettingByName("Ping");
            if (pi != null) ((Setting<Boolean>) pi).setValue(true);
         });
         context.waitTicks(25);
         Path pHud = context.takeScreenshot(screenshot("step7-ingame-hud").disableCounterPrefix());
         saveCopy(pHud, "step7_ingame_hud.png");

         // Enable 3D ESP & Fullbright
         context.runOnClient(client -> {
            ModuleManager.esp.enable();
            ModuleManager.fullbright.enable();
         });
         context.waitTicks(25);
         Path pEsp = context.takeScreenshot(screenshot("step8-ingame-render3d").disableCounterPrefix());
         saveCopy(pEsp, "step8_ingame_render3d.png");

         // Set GUI scale to 1 so all 5 categories fit on 854x480 test resolution
         context.runOnClient(client -> {
            client.options.guiScale().set(1);
            client.resizeDisplay();
            ModuleManager.clickGui.blur.setValue(false);
         });
         capture(context, ClickGUI::getClickGui, ClickGUI.class, "step9-clickgui-ingame", "step9_clickgui_ingame.png");

         // In-game ClickGUI with blur enabled
         context.runOnClient(client -> {
            ModuleManager.clickGui.blur.setValue(true);
         });
         context.waitTicks(10);
         Path pBlur = context.takeScreenshot(screenshot("step10-clickgui-blur-ingame").disableCounterPrefix());
         saveCopy(pBlur, "step10_clickgui_ingame_blur.png");
      }

      System.out.println("==========================================================");
      System.out.println(">>> VCORE 1.21.11 CLIENT GAMETEST COMPLETED WITH 0 ERRORS! <<<");
      System.out.println("==========================================================");
   }
}
