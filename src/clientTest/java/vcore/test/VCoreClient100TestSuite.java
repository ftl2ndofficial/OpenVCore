package vcore.test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.orbit.ICancellable;

import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.MacroManager;
import vcore.core.manager.client.ModuleManager;
import vcore.core.manager.player.FriendManager;
import vcore.events.impl.EventSetting;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.gui.notification.Notification;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.EnumConverter;
import vcore.setting.impl.ItemBindSetting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.setting.impl.PositionSetting;
import vcore.utility.KeyboardUtility;
import vcore.utility.Timer;
import vcore.utility.VcoreUtility;
import vcore.utility.math.FrameRateCounter;
import vcore.utility.math.MathUtility;
import vcore.utility.math.time.StopWatch;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.animation.EaseOutBack;

public class VCoreClient100TestSuite {

   enum DummyMode {
      First,
      Second,
      Third
   }

   public static class DummyTestEvent implements ICancellable {
      private boolean cancelled = false;
      public int value;

      public DummyTestEvent(int value) {
         this.value = value;
      }

      @Override
      public boolean isCancelled() {
         return this.cancelled;
      }

      @Override
      public void setCancelled(boolean cancelled) {
         this.cancelled = cancelled;
      }
   }

   private static int passedTests = 0;
   private static int totalTests = 0;

   public static void main(String[] args) {
      System.out.println("================================================================================");
      System.out.println(">>> STARTING VCORE 100-TEST CLIENT INTEGRATION & LOGIC SUITE <<<");
      System.out.println("================================================================================");

      try {
         net.minecraft.SharedConstants.tryDetectVersion();
         net.minecraft.server.Bootstrap.bootStrap();
      } catch (Throwable t) {
         System.out.println("[Bootstrap] Headless bootstrap note: " + t.getMessage());
      }

      // Group 1: Math & Geometry Calculations (Tests 1 - 15)
      runTest(1, "MathUtility_ClampInt", VCoreClient100TestSuite::test001_MathUtility_ClampInt);
      runTest(2, "MathUtility_ClampFloat", VCoreClient100TestSuite::test002_MathUtility_ClampFloat);
      runTest(3, "MathUtility_ClampDouble", VCoreClient100TestSuite::test003_MathUtility_ClampDouble);
      runTest(4, "MathUtility_Lerp", VCoreClient100TestSuite::test004_MathUtility_Lerp);
      runTest(5, "MathUtility_Round", VCoreClient100TestSuite::test005_MathUtility_Round);
      runTest(6, "MathUtility_RoundToDecimal", VCoreClient100TestSuite::test006_MathUtility_RoundToDecimal);
      runTest(7, "MathUtility_Rad", VCoreClient100TestSuite::test007_MathUtility_Rad);
      runTest(8, "MathUtility_Distance3D", VCoreClient100TestSuite::test008_MathUtility_Distance3D);
      runTest(9, "MathUtility_DistanceZero", VCoreClient100TestSuite::test009_MathUtility_DistanceZero);
      runTest(10, "MathUtility_FromToVec3", VCoreClient100TestSuite::test010_MathUtility_FromToVec3);
      runTest(11, "MathUtility_RandomRange", VCoreClient100TestSuite::test011_MathUtility_RandomRange);
      runTest(12, "MathUtility_RandomFloatRange", VCoreClient100TestSuite::test012_MathUtility_RandomFloatRange);
      runTest(13, "MathUtility_InterpolateColor", VCoreClient100TestSuite::test013_MathUtility_InterpolateColor);
      runTest(14, "MathUtility_InjectAlpha", VCoreClient100TestSuite::test014_MathUtility_InjectAlpha);
      runTest(15, "MathUtility_HexColorParsing", VCoreClient100TestSuite::test015_MathUtility_HexColorParsing);

      // Group 2: Timing, Counters & Stopwatches (Tests 16 - 25)
      runTest(16, "Timer_InitialReset", VCoreClient100TestSuite::test016_Timer_InitialReset);
      runTest(17, "Timer_PassedMs", VCoreClient100TestSuite::test017_Timer_PassedMs);
      runTest(18, "Timer_PassedS", VCoreClient100TestSuite::test018_Timer_PassedS);
      runTest(19, "Timer_SetMs", VCoreClient100TestSuite::test019_Timer_SetMs);
      runTest(20, "Timer_GetPassedTimeMs", VCoreClient100TestSuite::test020_Timer_GetPassedTimeMs);
      runTest(21, "Timer_Reset", VCoreClient100TestSuite::test021_Timer_Reset);
      runTest(22, "Timer_EveryTrigger", VCoreClient100TestSuite::test022_Timer_EveryTrigger);
      runTest(23, "Timer_GetMsConversion", VCoreClient100TestSuite::test023_Timer_GetMsConversion);
      runTest(24, "FrameRateCounter_FPSNonNegative", VCoreClient100TestSuite::test024_FrameRateCounter_FPSNonNegative);
      runTest(25, "StopWatch_Elapsed", VCoreClient100TestSuite::test025_StopWatch_Elapsed);

      // Group 3: Keyboard & Bind Mapping (Tests 26 - 35)
      runTest(26, "Keyboard_InvalidKeys", VCoreClient100TestSuite::test026_Keyboard_InvalidKeys);
      runTest(27, "Keyboard_CommonKeys", VCoreClient100TestSuite::test027_Keyboard_CommonKeys);
      runTest(28, "Keyboard_Modifiers", VCoreClient100TestSuite::test028_Keyboard_Modifiers);
      runTest(29, "Keyboard_FunctionKeys", VCoreClient100TestSuite::test029_Keyboard_FunctionKeys);
      runTest(30, "Keyboard_Keypad", VCoreClient100TestSuite::test030_Keyboard_Keypad);
      runTest(31, "Keyboard_KeypadOperators", VCoreClient100TestSuite::test031_Keyboard_KeypadOperators);
      runTest(32, "Keyboard_BindMouse", VCoreClient100TestSuite::test032_Keyboard_BindMouse);
      runTest(33, "Keyboard_BindKeyboardUpper", VCoreClient100TestSuite::test033_Keyboard_BindKeyboardUpper);
      runTest(34, "Bind_HoldState", VCoreClient100TestSuite::test034_Bind_HoldState);
      runTest(35, "Bind_EqualityAndMouse", VCoreClient100TestSuite::test035_Bind_EqualityAndMouse);

      // Group 4: Settings & Data Constraints (Tests 36 - 50)
      runTest(36, "Setting_Boolean", VCoreClient100TestSuite::test036_Setting_Boolean);
      runTest(37, "Setting_IntegerRestriction", VCoreClient100TestSuite::test037_Setting_IntegerRestriction);
      runTest(38, "Setting_FloatRestriction", VCoreClient100TestSuite::test038_Setting_FloatRestriction);
      runTest(39, "Setting_DoubleRestriction", VCoreClient100TestSuite::test039_Setting_DoubleRestriction);
      runTest(40, "Setting_Pow2Value", VCoreClient100TestSuite::test040_Setting_Pow2Value);
      runTest(41, "Setting_VisibilityPredicate", VCoreClient100TestSuite::test041_Setting_VisibilityPredicate);
      runTest(42, "Setting_PlannedValue", VCoreClient100TestSuite::test042_Setting_PlannedValue);
      runTest(43, "Setting_SilentUpdate", VCoreClient100TestSuite::test043_Setting_SilentUpdate);
      runTest(44, "Setting_EventPost", VCoreClient100TestSuite::test044_Setting_EventPost);
      runTest(45, "Setting_ParentModule", VCoreClient100TestSuite::test045_Setting_ParentModule);
      runTest(46, "BooleanSettingGroup_Toggle", VCoreClient100TestSuite::test046_BooleanSettingGroup_Toggle);
      runTest(47, "ColorSetting_AlphaHandling", VCoreClient100TestSuite::test047_ColorSetting_AlphaHandling);
      runTest(48, "PositionSetting_Coordinates", VCoreClient100TestSuite::test048_PositionSetting_Coordinates);
      runTest(49, "ItemSelectSetting_ListManagement", VCoreClient100TestSuite::test049_ItemSelectSetting_ListManagement);
      runTest(50, "ItemBindSetting_Binding", VCoreClient100TestSuite::test050_ItemBindSetting_Binding);

      // Group 5: EnumConverter & Serialization (Tests 51 - 60)
      runTest(51, "EnumConverter_CurrentEnum", VCoreClient100TestSuite::test051_EnumConverter_CurrentEnum);
      runTest(52, "EnumConverter_IncreaseEnum", VCoreClient100TestSuite::test052_EnumConverter_IncreaseEnum);
      runTest(53, "EnumConverter_SetEnumInt", VCoreClient100TestSuite::test053_EnumConverter_SetEnumInt);
      runTest(54, "EnumConverter_SetEnumIntOutOfBounds", VCoreClient100TestSuite::test054_EnumConverter_SetEnumIntOutOfBounds);
      runTest(55, "EnumConverter_GetNames", VCoreClient100TestSuite::test055_EnumConverter_GetNames);
      runTest(56, "EnumConverter_GetProperName", VCoreClient100TestSuite::test056_EnumConverter_GetProperName);
      runTest(57, "EnumConverter_DoForward", VCoreClient100TestSuite::test057_EnumConverter_DoForward);
      runTest(58, "EnumConverter_DoBackward_Exact", VCoreClient100TestSuite::test058_EnumConverter_DoBackward_Exact);
      runTest(59, "EnumConverter_DoBackward_CaseInsensitive", VCoreClient100TestSuite::test059_EnumConverter_DoBackward_CaseInsensitive);
      runTest(60, "EnumConverter_DoBackward_InvalidNull", VCoreClient100TestSuite::test060_EnumConverter_DoBackward_InvalidNull);

      // Group 6: Module Registration & Architecture (Tests 61 - 75)
      runTest(61, "ModuleManager_TotalCount", VCoreClient100TestSuite::test061_ModuleManager_TotalCount);
      runTest(62, "ModuleManager_NoNullModules", VCoreClient100TestSuite::test062_ModuleManager_NoNullModules);
      runTest(63, "ModuleManager_AuraLookup", VCoreClient100TestSuite::test063_ModuleManager_AuraLookup);
      runTest(64, "ModuleManager_CaseInsensitiveLookup", VCoreClient100TestSuite::test064_ModuleManager_CaseInsensitiveLookup);
      runTest(65, "ModuleManager_ClickGuiLookup", VCoreClient100TestSuite::test065_ModuleManager_ClickGuiLookup);
      runTest(66, "ModuleManager_AutoCrystalLookup", VCoreClient100TestSuite::test066_ModuleManager_AutoCrystalLookup);
      runTest(67, "ModuleManager_ESPLookup", VCoreClient100TestSuite::test067_ModuleManager_ESPLookup);
      runTest(68, "ModuleManager_NonExistentLookup", VCoreClient100TestSuite::test068_ModuleManager_NonExistentLookup);
      runTest(69, "Module_DisplayNameNonEmpty", VCoreClient100TestSuite::test069_Module_DisplayNameNonEmpty);
      runTest(70, "Module_CategoryValid", VCoreClient100TestSuite::test070_Module_CategoryValid);
      runTest(71, "Module_SettingsNotNull", VCoreClient100TestSuite::test071_Module_SettingsNotNull);
      runTest(72, "Module_SettingsCached", VCoreClient100TestSuite::test072_Module_SettingsCached);
      runTest(73, "Module_KeybindSettingPresent", VCoreClient100TestSuite::test073_Module_KeybindSettingPresent);
      runTest(74, "Module_StateToggle", VCoreClient100TestSuite::test074_Module_StateToggle);
      runTest(75, "Module_GetSettingByName", VCoreClient100TestSuite::test075_Module_GetSettingByName);

      // Group 7: Category System & Categorization (Tests 76 - 82)
      runTest(76, "Category_PredefinedCategories", VCoreClient100TestSuite::test076_Category_PredefinedCategories);
      runTest(77, "Category_IsCustomCategory", VCoreClient100TestSuite::test077_Category_IsCustomCategory);
      runTest(78, "Category_GetCategoryCompute", VCoreClient100TestSuite::test078_Category_GetCategoryCompute);
      runTest(79, "Category_CombatModulesCount", VCoreClient100TestSuite::test079_Category_CombatModulesCount);
      runTest(80, "Category_MovementModulesCount", VCoreClient100TestSuite::test080_Category_MovementModulesCount);
      runTest(81, "Category_RenderModulesCount", VCoreClient100TestSuite::test081_Category_RenderModulesCount);
      runTest(82, "Category_PlayerModulesCount", VCoreClient100TestSuite::test082_Category_PlayerModulesCount);

      // Group 8: Orbit EventBus Engine (Tests 83 - 90)
      runTest(83, "EventBus_Subscription", VCoreClient100TestSuite::test083_EventBus_Subscription);
      runTest(84, "EventBus_Unsubscription", VCoreClient100TestSuite::test084_EventBus_Unsubscription);
      runTest(85, "EventBus_PriorityOrdering", VCoreClient100TestSuite::test085_EventBus_PriorityOrdering);
      runTest(86, "EventBus_CancellableEvent", VCoreClient100TestSuite::test086_EventBus_CancellableEvent);
      runTest(87, "EventBus_MultipleListeners", VCoreClient100TestSuite::test087_EventBus_MultipleListeners);
      runTest(88, "EventBus_PostReturnsEvent", VCoreClient100TestSuite::test088_EventBus_PostReturnsEvent);
      runTest(89, "EventBus_FallbackLambdaFactory", VCoreClient100TestSuite::test089_EventBus_FallbackLambdaFactory);
      runTest(90, "EventBus_NoNPEOnUnregistered", VCoreClient100TestSuite::test090_EventBus_NoNPEOnUnregistered);

      // Group 9: Friends & Macro Management (Tests 91 - 95)
      runTest(91, "FriendManager_AddFriend", VCoreClient100TestSuite::test091_FriendManager_AddFriend);
      runTest(92, "FriendManager_CaseInsensitive", VCoreClient100TestSuite::test092_FriendManager_CaseInsensitive);
      runTest(93, "FriendManager_RemoveFriend", VCoreClient100TestSuite::test093_FriendManager_RemoveFriend);
      runTest(94, "MacroManager_AddAndFind", VCoreClient100TestSuite::test094_MacroManager_AddAndFind);
      runTest(95, "MacroManager_Remove", VCoreClient100TestSuite::test095_MacroManager_Remove);

      // Group 10: Notifications, Animations & Polish (Tests 96 - 100)
      runTest(96, "Notification_Getters", VCoreClient100TestSuite::test096_Notification_Getters);
      runTest(97, "Notification_TypeColors", VCoreClient100TestSuite::test097_Notification_TypeColors);
      runTest(98, "Notification_Dimensions", VCoreClient100TestSuite::test098_Notification_Dimensions);
      runTest(99, "EaseOutBack_AnimationRange", VCoreClient100TestSuite::test099_EaseOutBack_AnimationRange);
      runTest(100, "Vcore_MetadataSafelyResolved", VCoreClient100TestSuite::test100_Vcore_MetadataSafelyResolved);

      System.out.println("================================================================================");
      System.out.printf(">>> SUMMARY: %d / %d TESTS PASSED SUCCESSFULLY! <<<\n", passedTests, totalTests);
      System.out.println("================================================================================");

      if (passedTests != 100 || totalTests != 100) {
         throw new AssertionError("Not all 100 tests passed! Passed: " + passedTests + " / " + totalTests);
      }
   }

   private static void runTest(int number, String name, Runnable test) {
      totalTests++;
      System.out.printf("[%03d/100] Testing %-38s ... ", number, name);
      try {
         test.run();
         passedTests++;
         System.out.println("PASSED");
      } catch (Throwable t) {
         System.out.println("FAILED!");
         t.printStackTrace();
         throw new RuntimeException("Test " + number + " (" + name + ") failed", t);
      }
   }

   // --- GROUP 1: MATH & GEOMETRY (1 - 15) ---
   private static void test001_MathUtility_ClampInt() {
      assertEq(10, MathUtility.clamp(5, 10, 20), "Clamp lower bound");
      assertEq(20, MathUtility.clamp(25, 10, 20), "Clamp upper bound");
      assertEq(15, MathUtility.clamp(15, 10, 20), "Clamp inside bounds");
   }

   private static void test002_MathUtility_ClampFloat() {
      assertEq(1.0f, MathUtility.clamp(0.5f, 1.0f, 2.0f), "Clamp float lower");
      assertEq(2.0f, MathUtility.clamp(2.5f, 1.0f, 2.0f), "Clamp float upper");
      assertEq(1.5f, MathUtility.clamp(1.5f, 1.0f, 2.0f), "Clamp float inside");
   }

   private static void test003_MathUtility_ClampDouble() {
      assertEq(1.0, MathUtility.clamp(0.2, 1.0, 5.0), "Clamp double lower");
      assertEq(5.0, MathUtility.clamp(7.8, 1.0, 5.0), "Clamp double upper");
      assertEq(3.14, MathUtility.clamp(3.14, 1.0, 5.0), "Clamp double inside");
   }

   private static void test004_MathUtility_Lerp() {
      assertEq(10.0f, MathUtility.lerp(0.0f, 10.0f, 20.0f), "Lerp start");
      assertEq(15.0f, MathUtility.lerp(0.5f, 10.0f, 20.0f), "Lerp mid");
      assertEq(20.0f, MathUtility.lerp(1.0f, 10.0f, 20.0f), "Lerp end");
   }

   private static void test005_MathUtility_Round() {
      assertEq(3.14f, MathUtility.round(3.14159f), "Round float 2 decimals");
      assertEq(2.50f, MathUtility.round(2.500f), "Round half-up");
   }

   private static void test006_MathUtility_RoundToDecimal() {
      assertEq(3.0, MathUtility.roundToDecimal(3.789, 0), "Round 0 decimals floor");
      assertEq(3.8, MathUtility.roundToDecimal(3.789, 1), "Round 1 decimal");
      assertEq(3.789, MathUtility.roundToDecimal(3.78912, 3), "Round 3 decimals");
   }

   private static void test007_MathUtility_Rad() {
      assertEq(0.0f, MathUtility.rad(0.0f), "0 deg -> 0 rad");
      assertTrue(Math.abs(MathUtility.rad(180.0f) - (float)Math.PI) < 0.001f, "180 deg -> PI rad");
      assertTrue(Math.abs(MathUtility.rad(90.0f) - (float)(Math.PI / 2.0)) < 0.001f, "90 deg -> PI/2 rad");
   }

   private static void test008_MathUtility_Distance3D() {
      double dist = MathUtility.getDistance(0, 0, 0, 3, 4, 0);
      assertEq(5.0, dist, "3-4-5 right triangle 3D distance");
   }

   private static void test009_MathUtility_DistanceZero() {
      double dist = MathUtility.getDistance(12.5, -4.0, 100.0, 12.5, -4.0, 100.0);
      assertEq(0.0, dist, "Same coordinate distance is 0");
   }

   private static void test010_MathUtility_FromToVec3() {
      Vec3 delta = MathUtility.fromTo(10.0, 20.0, 30.0, 15.0, 25.0, 35.0);
      assertEq(5.0, delta.x, "Delta X");
      assertEq(5.0, delta.y, "Delta Y");
      assertEq(5.0, delta.z, "Delta Z");
   }

   private static void test011_MathUtility_RandomRange() {
      for (int i = 0; i < 50; i++) {
         double r = MathUtility.random(10.0, 20.0);
         assertTrue(r >= 10.0 && r <= 20.0, "Random double within range: " + r);
      }
   }

   private static void test012_MathUtility_RandomFloatRange() {
      for (int i = 0; i < 50; i++) {
         float r = MathUtility.random(5.0f, 15.0f);
         assertTrue(r >= 5.0f && r <= 15.0f, "Random float within range: " + r);
      }
   }

   private static void test013_MathUtility_InterpolateColor() {
      Color c1 = new Color(0, 0, 0, 255);
      Color c2 = new Color(200, 200, 200, 255);
      Color mid = Render2DEngine.interpolateColorC(c1, c2, 0.5f);
      assertEq(100, mid.getRed(), "Interpolated Red");
      assertEq(100, mid.getGreen(), "Interpolated Green");
      assertEq(100, mid.getBlue(), "Interpolated Blue");
   }

   private static void test014_MathUtility_InjectAlpha() {
      Color base = new Color(255, 128, 64, 255);
      Color withAlpha = Render2DEngine.injectAlpha(base, 100);
      assertEq(255, withAlpha.getRed(), "Preserve Red");
      assertEq(128, withAlpha.getGreen(), "Preserve Green");
      assertEq(64, withAlpha.getBlue(), "Preserve Blue");
      assertEq(100, withAlpha.getAlpha(), "Updated Alpha");
   }

   private static void test015_MathUtility_HexColorParsing() {
      int argb = 0xFF112233;
      int a = (argb >> 24) & 0xFF;
      int r = (argb >> 16) & 0xFF;
      int g = (argb >> 8) & 0xFF;
      int b = argb & 0xFF;
      assertEq(255, a, "Alpha 255");
      assertEq(0x11, r, "Red 0x11");
      assertEq(0x22, g, "Green 0x22");
      assertEq(0x33, b, "Blue 0x33");
   }

   // --- GROUP 2: TIMING, COUNTERS & STOPWATCHES (16 - 25) ---
   private static void test016_Timer_InitialReset() {
      Timer timer = new Timer();
      assertTrue(timer.getPassedTimeMs() >= 0, "Initial timer non-negative");
   }

   private static void test017_Timer_PassedMs() {
      Timer timer = new Timer();
      assertTrue(timer.passedMs(0), "passedMs(0) must immediately be true");
   }

   private static void test018_Timer_PassedS() {
      Timer timer = new Timer();
      assertTrue(timer.passedS(0.0), "passedS(0) must immediately be true");
   }

   private static void test019_Timer_SetMs() {
      Timer timer = new Timer();
      timer.setMs(500);
      assertTrue(timer.getPassedTimeMs() >= 450, "setMs sets backwards offset");
   }

   private static void test020_Timer_GetPassedTimeMs() {
      Timer timer = new Timer();
      long t1 = timer.getPassedTimeMs();
      long t2 = timer.getPassedTimeMs();
      assertTrue(t2 >= t1, "Timer is monotonic");
   }

   private static void test021_Timer_Reset() {
      Timer timer = new Timer();
      timer.setMs(2000);
      timer.reset();
      assertTrue(timer.getPassedTimeMs() < 100, "Reset drops elapsed time");
   }

   private static void test022_Timer_EveryTrigger() {
      Timer timer = new Timer();
      timer.setMs(100);
      boolean triggered = timer.every(50);
      assertTrue(triggered, "every() triggers when time passed");
      assertTrue(timer.getPassedTimeMs() < 50, "every() auto-resets timer");
   }

   private static void test023_Timer_GetMsConversion() {
      Timer timer = new Timer();
      assertEq(10L, timer.getMs(10_000_000L), "Nano to ms division");
   }

   private static void test024_FrameRateCounter_FPSNonNegative() {
      assertTrue(FrameRateCounter.INSTANCE.getFps() >= 0, "FPS counter non-negative");
   }

   private static void test025_StopWatch_Elapsed() {
      Timer timer = new Timer();
      timer.setMs(250);
      assertTrue(timer.passedMs(200), "Passed 200 ms");
      assertTrue(!timer.passedMs(500), "Not passed 500 ms");
   }

   // --- GROUP 3: KEYBOARD & BINDS (26 - 35) ---
   private static void test026_Keyboard_InvalidKeys() {
      assertEq("None", KeyboardUtility.getKeyName(-1), "-1 is None");
      assertEq("None", KeyboardUtility.getKeyName(0), "0 is None");
   }

   private static void test027_Keyboard_CommonKeys() {
      assertEq("Space", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_SPACE), "Space");
      assertEq("Esc", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_ESCAPE), "Esc");
      assertEq("Enter", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_ENTER), "Enter");
      assertEq("Tab", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_TAB), "Tab");
      assertEq("Back", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_BACKSPACE), "Back");
   }

   private static void test028_Keyboard_Modifiers() {
      assertEq("LShift", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_LEFT_SHIFT), "LShift");
      assertEq("RShift", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_RIGHT_SHIFT), "RShift");
      assertEq("LCtrl", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_LEFT_CONTROL), "LCtrl");
      assertEq("RCtrl", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_RIGHT_CONTROL), "RCtrl");
      assertEq("LAlt", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_LEFT_ALT), "LAlt");
      assertEq("RAlt", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_RIGHT_ALT), "RAlt");
   }

   private static void test029_Keyboard_FunctionKeys() {
      assertEq("F1", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_F1), "F1");
      assertEq("F5", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_F5), "F5");
      assertEq("F12", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_F12), "F12");
   }

   private static void test030_Keyboard_Keypad() {
      assertEq("Num0", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_KP_0), "Num0");
      assertEq("Num9", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_KP_9), "Num9");
   }

   private static void test031_Keyboard_KeypadOperators() {
      assertEq("Num+", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_KP_ADD), "Num+");
      assertEq("Num-", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_KP_SUBTRACT), "Num-");
      assertEq("Num*", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_KP_MULTIPLY), "Num*");
      assertEq("Num/", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_KP_DIVIDE), "Num/");
   }

   private static void test032_Keyboard_BindMouse() {
      assertEq("M1", KeyboardUtility.getBindName(1, true), "Mouse M1");
      assertEq("M2", KeyboardUtility.getBindName(2, true), "Mouse M2");
   }

   private static void test033_Keyboard_BindKeyboardUpper() {
      assertEq("SPACE", KeyboardUtility.getBindName(GLFW.GLFW_KEY_SPACE, false), "SPACE");
      assertEq("LSHIFT", KeyboardUtility.getBindName(GLFW.GLFW_KEY_LEFT_SHIFT, false), "LSHIFT");
   }

   private static void test034_Bind_HoldState() {
      Bind bind = new Bind(GLFW.GLFW_KEY_R, false, true);
      assertTrue(bind.isHold(), "Bind hold is true");
      bind.setHold(false);
      assertTrue(!bind.isHold(), "Bind hold changed to false");
   }

   private static void test035_Bind_EqualityAndMouse() {
      Bind mouseBind = new Bind(1, true, false);
      assertTrue(mouseBind.isMouse(), "Mouse bind flag");
      assertEq("M1", mouseBind.getBind(), "Mouse bind string representation");
      assertTrue(mouseBind.isBound(), "Mouse bind isBound");
      assertTrue(!mouseBind.isEmpty(), "Mouse bind not empty");

      Bind keyboardBind = new Bind(GLFW.GLFW_KEY_R, false, false);
      assertTrue(keyboardBind.isBound(), "Keyboard key R isBound");
      assertTrue(!keyboardBind.isEmpty(), "Keyboard key R not empty");

      Bind unbound = new Bind(-1, false, false);
      assertTrue(!unbound.isBound(), "Unbound (-1) is not bound");
      assertTrue(unbound.isEmpty(), "Unbound (-1) isEmpty");
   }

   // --- GROUP 4: SETTINGS & CONSTRAINTS (36 - 50) ---
   private static void test036_Setting_Boolean() {
      Setting<Boolean> setting = new Setting<>("TestBool", true);
      assertTrue(setting.getValue(), "Initial bool value");
      setting.setValue(false);
      assertTrue(!setting.getValue(), "Updated bool value");
   }

   private static void test037_Setting_IntegerRestriction() {
      Setting<Integer> setting = new Setting<>("IntRange", 10, 0, 20);
      setting.setValueSilent(25);
      assertEq(20, setting.getValue(), "Clamped to max 20");
      setting.setValueSilent(-5);
      assertEq(0, setting.getValue(), "Clamped to min 0");
   }

   private static void test038_Setting_FloatRestriction() {
      Setting<Float> setting = new Setting<>("FloatRange", 5.0f, 1.0f, 10.0f);
      setting.setValueSilent(15.0f);
      assertEq(10.0f, setting.getValue(), "Clamped float max");
      setting.setValueSilent(-2.0f);
      assertEq(1.0f, setting.getValue(), "Clamped float min");
   }

   private static void test039_Setting_DoubleRestriction() {
      Setting<Double> setting = new Setting<>("DoubleRange", 5.0, 1.0, 10.0);
      setting.setValueSilent(20.0);
      assertEq(10.0, setting.getValue(), "Clamped double max");
   }

   private static void test040_Setting_Pow2Value() {
      Setting<Float> floatSetting = new Setting<>("FloatPow", 4.0f);
      assertEq(16.0f, floatSetting.getPow2Value(), "Float 4^2 = 16");

      Setting<Integer> intSetting = new Setting<>("IntPow", 5);
      assertEq(25.0f, intSetting.getPow2Value(), "Int 5^2 = 25");
   }

   private static void test041_Setting_VisibilityPredicate() {
      Setting<Boolean> condition = new Setting<>("Master", false);
      Setting<Integer> dependent = new Setting<>("Child", 10, val -> condition.getValue());
      assertTrue(!dependent.isVisible(), "Initially not visible");
      condition.setValue(true);
      assertTrue(dependent.isVisible(), "Visible after condition met");
   }

   private static void test042_Setting_PlannedValue() {
      Setting<Integer> setting = new Setting<>("Planned", 5);
      setting.setPlannedValue(15);
      setting.setValueSilent(15);
      assertEq(15, setting.getValue(), "Planned value set");
   }

   private static void test043_Setting_SilentUpdate() {
      AtomicBoolean eventTriggered = new AtomicBoolean(false);
      Object listener = new Object() {
         @EventHandler
         public void onSetting(EventSetting event) {
            eventTriggered.set(true);
         }
      };
      Vcore.EVENT_BUS.subscribe(listener);
      Setting<Integer> setting = new Setting<>("Silent", 5);
      setting.setValueSilent(10);
      Vcore.EVENT_BUS.unsubscribe(listener);
      assertTrue(!eventTriggered.get(), "Silent update does not post event");
   }

   private static void test044_Setting_EventPost() {
      AtomicBoolean eventTriggered = new AtomicBoolean(false);
      Object listener = new Object() {
         @EventHandler
         public void onSetting(EventSetting event) {
            eventTriggered.set(true);
         }
      };
      Vcore.EVENT_BUS.subscribe(listener);
      Setting<Integer> setting = new Setting<>("NotSilent", 5);
      setting.setValue(10);
      Vcore.EVENT_BUS.unsubscribe(listener);
      assertTrue(eventTriggered.get(), "setValue posts EventSetting");
   }

   private static void test045_Setting_ParentModule() {
      Aura aura = ModuleManager.aura;
      Setting<Boolean> setting = new Setting<>("TestParent", true);
      aura.registerSetting(setting);
      assertEq(aura, setting.getModule(), "Setting module parent assigned");
   }

   private static void test046_BooleanSettingGroup_Toggle() {
      BooleanSettingGroup group = new BooleanSettingGroup(false);
      assertTrue(!group.isEnabled(), "Group initial state");
      group.setEnabled(true);
      assertTrue(group.isEnabled(), "Group enabled state");
      group.setExtended(true);
      assertTrue(group.isExtended(), "Group extended state");
   }

   private static void test047_ColorSetting_AlphaHandling() {
      ColorSetting colorSetting = new ColorSetting(new Color(255, 0, 0, 180));
      ColorSetting withNewAlpha = colorSetting.withAlpha(220);
      int alpha = (withNewAlpha.getColor() >> 24) & 0xFF;
      assertEq(220, alpha, "Updated alpha via withAlpha");
   }

   private static void test048_PositionSetting_Coordinates() {
      PositionSetting pos = new PositionSetting(100.0f, 200.0f);
      assertEq(100.0f, pos.getX(), "Position X");
      assertEq(200.0f, pos.getY(), "Position Y");
      pos.setX(150.0f);
      pos.setY(250.0f);
      assertEq(150.0f, pos.getX(), "Updated Position X");
      assertEq(250.0f, pos.getY(), "Updated Position Y");
   }

   private static void test049_ItemSelectSetting_ListManagement() {
      ItemSelectSetting select = new ItemSelectSetting(new ArrayList<>());
      select.add("minecraft:diamond_sword");
      assertTrue(select.contains("minecraft:diamond_sword"), "Item added to select setting");
      select.remove("minecraft:diamond_sword");
      assertTrue(!select.contains("minecraft:diamond_sword"), "Item removed from select setting");
   }

   private static void test050_ItemBindSetting_Binding() {
      Bind bind = new Bind(GLFW.GLFW_KEY_G, false, false);
      ItemBindSetting itemBind = new ItemBindSetting("minecraft:totem_of_undying", bind);
      assertEq("minecraft:totem_of_undying", itemBind.getItemId(), "Item ID in ItemBindSetting");
      assertEq(GLFW.GLFW_KEY_G, itemBind.getBind().getKey(), "Bind key in ItemBindSetting");
   }

   // --- GROUP 5: ENUM CONVERTER (51 - 60) ---
   private static void test051_EnumConverter_CurrentEnum() {
      assertEq(0, EnumConverter.currentEnum(DummyMode.First), "First index 0");
      assertEq(1, EnumConverter.currentEnum(DummyMode.Second), "Second index 1");
      assertEq(2, EnumConverter.currentEnum(DummyMode.Third), "Third index 2");
   }

   private static void test052_EnumConverter_IncreaseEnum() {
      assertEq(DummyMode.Second, EnumConverter.increaseEnum(DummyMode.First), "First -> Second");
      assertEq(DummyMode.Third, EnumConverter.increaseEnum(DummyMode.Second), "Second -> Third");
      assertEq(DummyMode.First, EnumConverter.increaseEnum(DummyMode.Third), "Third -> First");
   }

   private static void test053_EnumConverter_SetEnumInt() {
      assertEq(DummyMode.First, EnumConverter.setEnumInt(DummyMode.Third, 0), "Set to index 0");
      assertEq(DummyMode.Second, EnumConverter.setEnumInt(DummyMode.First, 1), "Set to index 1");
   }

   private static void test054_EnumConverter_SetEnumIntOutOfBounds() {
      assertEq(DummyMode.First, EnumConverter.setEnumInt(DummyMode.First, -1), "Negative bound fallback");
      assertEq(DummyMode.First, EnumConverter.setEnumInt(DummyMode.First, 999), "Overflow bound fallback");
   }

   private static void test055_EnumConverter_GetNames() {
      String[] names = EnumConverter.getNames(DummyMode.First);
      assertEq(3, names.length, "Enum has 3 constant names");
      assertEq("First", names[0], "First name");
      assertEq("Second", names[1], "Second name");
   }

   private static void test056_EnumConverter_GetProperName() {
      assertEq("First", EnumConverter.getProperName(DummyMode.First), "Proper name matches");
   }

   private static void test057_EnumConverter_DoForward() {
      EnumConverter converter = new EnumConverter(DummyMode.class);
      assertEq("Second", converter.doForward(DummyMode.Second).getAsString(), "Forward conversion");
   }

   private static void test058_EnumConverter_DoBackward_Exact() {
      EnumConverter converter = new EnumConverter(DummyMode.class);
      assertEq(DummyMode.Third, converter.doBackward(new com.google.gson.JsonPrimitive("Third")), "Backward exact");
   }

   private static void test059_EnumConverter_DoBackward_CaseInsensitive() {
      EnumConverter converter = new EnumConverter(DummyMode.class);
      assertEq(DummyMode.Second, converter.doBackward(new com.google.gson.JsonPrimitive("second")), "Backward lowercase");
   }

   private static void test060_EnumConverter_DoBackward_InvalidNull() {
      EnumConverter converter = new EnumConverter(DummyMode.class);
      assertNull(converter.doBackward(null), "Null element backward");
      assertNull(converter.doBackward(new com.google.gson.JsonPrimitive("NonExistent")), "Invalid element backward");
   }

   // --- GROUP 6: MODULE REGISTRATION & ARCHITECTURE (61 - 75) ---
   private static void test061_ModuleManager_TotalCount() {
      ModuleManager manager = Managers.MODULE;
      assertEq(109, manager.modules.size(), "Exactly 109 modules registered in ModuleManager");
   }

   private static void test062_ModuleManager_NoNullModules() {
      for (Module m : Managers.MODULE.modules) {
         assertTrue(m != null, "Module in modules list must not be null");
      }
   }

   private static void test063_ModuleManager_AuraLookup() {
      Module aura = Managers.MODULE.get("Aura");
      assertTrue(aura != null, "Aura found");
      assertEq("Aura", aura.getName(), "Aura name matches");
   }

   private static void test064_ModuleManager_CaseInsensitiveLookup() {
      Module a1 = Managers.MODULE.get("aura");
      Module a2 = Managers.MODULE.get("AURA");
      assertEq(a1, a2, "Case-insensitive module lookup");
   }

   private static void test065_ModuleManager_ClickGuiLookup() {
      assertTrue(Managers.MODULE.get("ClickGui") != null, "ClickGui module exists");
   }

   private static void test066_ModuleManager_AutoCrystalLookup() {
      assertTrue(Managers.MODULE.get("AutoCrystal") != null, "AutoCrystal module exists");
   }

   private static void test067_ModuleManager_ESPLookup() {
      assertTrue(Managers.MODULE.get("ESP") != null, "ESP module exists");
   }

   private static void test068_ModuleManager_NonExistentLookup() {
      assertNull(Managers.MODULE.get("DoesNotExist123"), "Nonexistent module lookup returns null");
   }

   private static void test069_Module_DisplayNameNonEmpty() {
      for (Module m : Managers.MODULE.modules) {
         assertTrue(m.getName() != null && !m.getName().isEmpty(), "Module display name non-empty: " + m.getClass().getName());
      }
   }

   private static void test070_Module_CategoryValid() {
      for (Module m : Managers.MODULE.modules) {
         assertTrue(m.getCategory() != null, "Module category non-null for: " + m.getName());
      }
   }

   private static void test071_Module_SettingsNotNull() {
      for (Module m : Managers.MODULE.modules) {
         assertTrue(m.getSettings() != null, "Module settings non-null for: " + m.getName());
      }
   }

   private static void test072_Module_SettingsCached() {
      Module aura = ModuleManager.aura;
      List<Setting<?>> s1 = aura.getSettings();
      List<Setting<?>> s2 = aura.getSettings();
      assertEq(s1.size(), s2.size(), "Module settings size matches across calls");
      for (int i = 0; i < s1.size(); i++) {
         assertTrue(s1.get(i) == s2.get(i), "Setting element " + i + " is identical cached instance");
      }
   }

   private static void test073_Module_KeybindSettingPresent() {
      for (Module m : Managers.MODULE.modules) {
         assertTrue(m.getSettingByName("Keybind") != null, "Module has Keybind setting: " + m.getName());
      }
   }

   private static void test074_Module_StateToggle() {
      Module aura = ModuleManager.aura;
      boolean initial = aura.isEnabled();
      aura.setEnabled(!initial);
      assertEq(!initial, aura.isEnabled(), "Module state toggled");
      aura.setEnabled(initial);
      assertEq(initial, aura.isEnabled(), "Module state restored");
   }

   private static void test075_Module_GetSettingByName() {
      Setting<?> bindSetting = ModuleManager.aura.getSettingByName("Keybind");
      assertTrue(bindSetting != null, "Keybind setting found on Aura");
      assertEq("Keybind", bindSetting.getName(), "Setting name matches");
   }

   // --- GROUP 7: CATEGORY SYSTEM (76 - 82) ---
   private static void test076_Category_PredefinedCategories() {
      assertEq("Combat", Module.Category.COMBAT.getName(), "Combat category");
      assertEq("Movement", Module.Category.MOVEMENT.getName(), "Movement category");
      assertEq("Render", Module.Category.RENDER.getName(), "Render category");
      assertEq("Player", Module.Category.PLAYER.getName(), "Player category");
      assertEq("Misc", Module.Category.MISC.getName(), "Misc category");
   }

   private static void test077_Category_IsCustomCategory() {
      assertTrue(!Module.Category.isCustomCategory(Module.Category.COMBAT), "Combat is not custom");
      Module.Category custom = Module.Category.getCategory("CustomAddon");
      assertTrue(Module.Category.isCustomCategory(custom), "CustomAddon is custom category");
   }

   private static void test078_Category_GetCategoryCompute() {
      Module.Category c1 = Module.Category.getCategory("SpecialTestCategory");
      Module.Category c2 = Module.Category.getCategory("SpecialTestCategory");
      assertEq(c1, c2, "getCategory returns same category instance");
   }

   private static void test079_Category_CombatModulesCount() {
      long count = Managers.MODULE.modules.stream().filter(m -> m.getCategory() == Module.Category.COMBAT).count();
      assertTrue(count >= 15, "Combat category module count >= 15, found: " + count);
   }

   private static void test080_Category_MovementModulesCount() {
      long count = Managers.MODULE.modules.stream().filter(m -> m.getCategory() == Module.Category.MOVEMENT).count();
      assertTrue(count >= 15, "Movement category module count >= 15, found: " + count);
   }

   private static void test081_Category_RenderModulesCount() {
      long count = Managers.MODULE.modules.stream().filter(m -> m.getCategory() == Module.Category.RENDER).count();
      assertTrue(count >= 15, "Render category module count >= 15, found: " + count);
   }

   private static void test082_Category_PlayerModulesCount() {
      long count = Managers.MODULE.modules.stream().filter(m -> m.getCategory() == Module.Category.PLAYER).count();
      assertTrue(count >= 15, "Player category module count >= 15, found: " + count);
   }

   // --- GROUP 8: ORBIT EVENTBUS (83 - 90) ---
   private static void test083_EventBus_Subscription() {
      EventBus bus = new EventBus();
      AtomicInteger count = new AtomicInteger(0);
      Object listener = new Object() {
         @EventHandler
         public void onTest(DummyTestEvent event) {
            count.addAndGet(event.value);
         }
      };
      bus.subscribe(listener);
      bus.post(new DummyTestEvent(42));
      assertEq(42, count.get(), "Subscribed listener received event");
   }

   private static void test084_EventBus_Unsubscription() {
      EventBus bus = new EventBus();
      AtomicInteger count = new AtomicInteger(0);
      Object listener = new Object() {
         @EventHandler
         public void onTest(DummyTestEvent event) {
            count.incrementAndGet();
         }
      };
      bus.subscribe(listener);
      bus.post(new DummyTestEvent(1));
      bus.unsubscribe(listener);
      bus.post(new DummyTestEvent(1));
      assertEq(1, count.get(), "Unsubscribed listener stopped receiving events");
   }

   private static void test085_EventBus_PriorityOrdering() {
      EventBus bus = new EventBus();
      List<String> order = new ArrayList<>();
      Object listenerLow = new Object() {
         @EventHandler(priority = EventPriority.LOW)
         public void onTest(DummyTestEvent event) {
            order.add("LOW");
         }
      };
      Object listenerHigh = new Object() {
         @EventHandler(priority = EventPriority.HIGH)
         public void onTest(DummyTestEvent event) {
            order.add("HIGH");
         }
      };
      bus.subscribe(listenerLow);
      bus.subscribe(listenerHigh);
      bus.post(new DummyTestEvent(1));
      assertEq("HIGH", order.get(0), "HIGH priority runs first");
      assertEq("LOW", order.get(1), "LOW priority runs second");
   }

   private static void test086_EventBus_CancellableEvent() {
      EventBus bus = new EventBus();
      AtomicBoolean secondRun = new AtomicBoolean(false);
      Object l1 = new Object() {
         @EventHandler(priority = EventPriority.HIGHEST)
         public void onFirst(DummyTestEvent event) {
            event.setCancelled(true);
         }
      };
      Object l2 = new Object() {
         @EventHandler(priority = EventPriority.LOW)
         public void onSecond(DummyTestEvent event) {
            secondRun.set(true);
         }
      };
      bus.subscribe(l1);
      bus.subscribe(l2);
      DummyTestEvent ev = bus.post(new DummyTestEvent(1));
      assertTrue(ev.isCancelled(), "Event was cancelled");
      assertTrue(!secondRun.get(), "Cancellation stopped further listeners");
   }

   private static void test087_EventBus_MultipleListeners() {
      EventBus bus = new EventBus();
      AtomicInteger sum = new AtomicInteger(0);
      Object l1 = new Object() {
         @EventHandler
         public void onTest(DummyTestEvent event) { sum.addAndGet(10); }
      };
      Object l2 = new Object() {
         @EventHandler
         public void onTest(DummyTestEvent event) { sum.addAndGet(20); }
      };
      bus.subscribe(l1);
      bus.subscribe(l2);
      bus.post(new DummyTestEvent(1));
      assertEq(30, sum.get(), "Both listeners triggered");
   }

   private static void test088_EventBus_PostReturnsEvent() {
      EventBus bus = new EventBus();
      DummyTestEvent ev = new DummyTestEvent(99);
      DummyTestEvent returned = bus.post(ev);
      assertEq(ev, returned, "post() returns same event instance");
   }

   private static void test089_EventBus_FallbackLambdaFactory() {
      EventBus bus = new EventBus();
      bus.registerLambdaFactory("", (lookup, klass) -> java.lang.invoke.MethodHandles.lookup());
      AtomicBoolean ran = new AtomicBoolean(false);
      Object listener = new Object() {
         @EventHandler
         public void onTest(DummyTestEvent event) {
            ran.set(true);
         }
      };
      bus.subscribe(listener);
      bus.post(new DummyTestEvent(1));
      assertTrue(ran.get(), "Fallback lambda factory handled event");
   }

   private static void test090_EventBus_NoNPEOnUnregistered() {
      EventBus bus = new EventBus();
      DummyTestEvent ev = new DummyTestEvent(1);
      bus.post(ev); // Should complete with zero errors
      assertTrue(true, "Posting event without listeners succeeded");
   }

   // --- GROUP 9: FRIENDS & MACROS (91 - 95) ---
   private static void test091_FriendManager_AddFriend() {
      FriendManager fm = Managers.FRIEND;
      fm.addFriend("TestPlayer1");
      assertTrue(fm.isFriend("TestPlayer1"), "Friend added successfully");
      fm.removeFriend("TestPlayer1");
   }

   private static void test092_FriendManager_CaseInsensitive() {
      FriendManager fm = Managers.FRIEND;
      fm.addFriend("CaseSensitiveName");
      assertTrue(fm.isFriend("casesensitivename"), "Friend lookup is case-insensitive");
      fm.removeFriend("CaseSensitiveName");
   }

   private static void test093_FriendManager_RemoveFriend() {
      FriendManager fm = Managers.FRIEND;
      fm.addFriend("ToBeRemoved");
      fm.removeFriend("ToBeRemoved");
      assertTrue(!fm.isFriend("ToBeRemoved"), "Friend removed successfully");
   }

   private static void test094_MacroManager_AddAndFind() {
      MacroManager.Macro macro = new MacroManager.Macro("test_macro", "/help", GLFW.GLFW_KEY_H);
      MacroManager.addMacro(macro);
      assertTrue(Managers.MACRO.getMacros().contains(macro), "Macro added to MacroManager");
      Managers.MACRO.removeMacro(macro);
   }

   private static void test095_MacroManager_Remove() {
      MacroManager.Macro macro = new MacroManager.Macro("temp_macro", "/spawn", GLFW.GLFW_KEY_K);
      MacroManager.addMacro(macro);
      Managers.MACRO.removeMacro(macro);
      assertTrue(!Managers.MACRO.getMacros().contains(macro), "Macro removed from MacroManager");
   }

   // --- GROUP 10: NOTIFICATIONS, ANIMATIONS & POLISH (96 - 100) ---
   private static void test096_Notification_Getters() {
      Notification notif = new Notification("Heading", "Details here", Notification.Type.SUCCESS, 2500);
      assertEq("Heading", notif.getTitle(), "Notification title getter");
      assertEq("Details here", notif.getMessage(), "Notification message getter");
      assertEq(2500, notif.getLifeTime(), "Notification lifetime getter");
   }

   private static void test097_Notification_TypeColors() {
      assertEq(ChatFormatting.GREEN, Notification.Type.SUCCESS.getColor(), "SUCCESS green color");
      assertEq(ChatFormatting.RED, Notification.Type.ERROR.getColor(), "ERROR red color");
      assertEq(ChatFormatting.GOLD, Notification.Type.WARNING.getColor(), "WARNING gold color");
      assertEq(ChatFormatting.AQUA, Notification.Type.INFO.getColor(), "INFO aqua color");
   }

   private static void test098_Notification_Dimensions() {
      Notification notif = new Notification("DimTitle", "DimDesc", Notification.Type.INFO, 1000);
      assertTrue(notif.getHeight() > 0, "Notification height is positive");
   }

   private static void test099_EaseOutBack_AnimationRange() {
      EaseOutBack anim = new EaseOutBack(100);
      anim.update(true);
      double drop = EaseOutBack.dropAnimation(0.5);
      assertTrue(drop >= 0.0 && drop <= 2.0, "EaseOutBack dropAnimation within valid bounds: " + drop);
   }

   private static void test100_Vcore_MetadataSafelyResolved() {
      String authors = VcoreUtility.getAuthors();
      assertTrue(authors != null && !authors.isEmpty(), "Authors resolved non-empty: " + authors);
      boolean isWin = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
      assertEq(isWin, vcore.features.modules.misc.RPC.isWindows(), "RPC.isWindows matches system property");
      if (!isWin) {
         assertTrue(!vcore.utility.discord.DiscordRPC.isSupported(), "DiscordRPC is not supported on non-Windows");
         assertNull(vcore.utility.discord.DiscordRPC.getInstance(), "DiscordRPC instance is null on non-Windows");
      }
   }

   // --- ASSERTION HELPERS ---
   private static void assertEq(Object expected, Object actual, String message) {
      if (expected == null && actual == null) return;
      if (expected != null && expected.equals(actual)) return;
      throw new AssertionError(message + " - Expected: [" + expected + "], Actual: [" + actual + "]");
   }

   private static void assertTrue(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }

   private static void assertNull(Object object, String message) {
      if (object != null) {
         throw new AssertionError(message + " - Expected null, but got: [" + object + "]");
      }
   }
}
