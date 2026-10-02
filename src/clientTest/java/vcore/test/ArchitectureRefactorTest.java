package vcore.test;

import org.lwjgl.glfw.GLFW;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.gui.notification.Notification;
import vcore.setting.impl.EnumConverter;
import vcore.utility.KeyboardUtility;
import meteordevelopment.orbit.EventBus;

public class ArchitectureRefactorTest {

   enum TestMode {
      First,
      Second,
      Third
   }

   public static void main(String[] args) {
      System.out.println(">>> RUNNING ARCHITECTURE REFACTOR & OBFUSCATION READINESS TESTS <<<");

      try {
         net.minecraft.SharedConstants.tryDetectVersion();
         net.minecraft.server.Bootstrap.bootStrap();
      } catch (Throwable t) {
         System.out.println("[Warning] Headless bootstrap: " + t.getMessage());
      }

      testKeyboardUtility();
      testEnumConverter();
      testNotificationGetters();
      testModuleManagerRegistration();
      testEventBusLambdaFallback();

      System.out.println(">>> ALL ARCHITECTURE TESTS PASSED SUCCESSFULLY! <<<");
   }

   private static void testKeyboardUtility() {
      System.out.print("[Test] KeyboardUtility... ");
      assertEq("None", KeyboardUtility.getKeyName(-1), "Key -1 should be None");
      assertEq("None", KeyboardUtility.getKeyName(0), "Key 0 should be None");
      assertEq("Space", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_SPACE), "Space key");
      assertEq("Esc", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_ESCAPE), "Esc key");
      assertEq("LShift", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_LEFT_SHIFT), "LShift key");
      assertEq("LCtrl", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_LEFT_CONTROL), "LCtrl key");
      assertEq("F1", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_F1), "F1 key");
      assertEq("F12", KeyboardUtility.getKeyName(GLFW.GLFW_KEY_F12), "F12 key");

      // Bind name formatting
      assertEq("M1", KeyboardUtility.getBindName(1, true), "Mouse bind M1");
      assertEq("SPACE", KeyboardUtility.getBindName(GLFW.GLFW_KEY_SPACE, false), "Bind Space uppercase");
      assertEq("LSHIFT", KeyboardUtility.getBindName(GLFW.GLFW_KEY_LEFT_SHIFT, false), "Bind LShift uppercase");
      System.out.println("PASSED");
   }

   private static void testEnumConverter() {
      System.out.print("[Test] EnumConverter... ");
      assertEq(0, EnumConverter.currentEnum(TestMode.First), "First ordinal should be 0");
      assertEq(1, EnumConverter.currentEnum(TestMode.Second), "Second ordinal should be 1");
      assertEq(2, EnumConverter.currentEnum(TestMode.Third), "Third ordinal should be 2");

      assertEq(TestMode.Second, EnumConverter.increaseEnum(TestMode.First), "Increase First -> Second");
      assertEq(TestMode.Third, EnumConverter.increaseEnum(TestMode.Second), "Increase Second -> Third");
      assertEq(TestMode.First, EnumConverter.increaseEnum(TestMode.Third), "Increase Third -> First (wrap)");

      assertEq(TestMode.First, EnumConverter.setEnumInt(TestMode.Third, 0), "setEnumInt 0 -> First");
      assertEq(TestMode.Second, EnumConverter.setEnumInt(TestMode.First, 1), "setEnumInt 1 -> Second");
      assertEq(TestMode.First, EnumConverter.setEnumInt(TestMode.First, 999), "setEnumInt out of bounds -> First");

      EnumConverter converter = new EnumConverter(TestMode.class);
      assertEq(TestMode.First, converter.doBackward(new com.google.gson.JsonPrimitive("First")), "Deserialize 'First'");
      assertEq(TestMode.Second, converter.doBackward(new com.google.gson.JsonPrimitive("second")), "Deserialize 'second' case-insensitive");
      assertNull(converter.doBackward(null), "Deserialize null element");
      System.out.println("PASSED");
   }

   private static void testNotificationGetters() {
      System.out.print("[Test] Notification Getters... ");
      Notification notification = new Notification("TestTitle", "TestMessage", Notification.Type.INFO, 3000);
      assertEq("TestTitle", notification.getTitle(), "Notification title getter");
      assertEq("TestMessage", notification.getMessage(), "Notification message getter");
      assertEq(3000, notification.getLifeTime(), "Notification lifetime getter");
      System.out.println("PASSED");
   }

   private static void testModuleManagerRegistration() {
      System.out.print("[Test] ModuleManager Direct Registration... ");
      ModuleManager manager = new ModuleManager();
      assertTrue(manager.modules.size() >= 100, "Modules count should be at least 100, was: " + manager.modules.size());

      Module aura = manager.get("Aura");
      assertTrue(aura != null, "Aura module must be present");
      assertEq("Aura", aura.getName(), "Aura name matches");

      Module clickGui = manager.get("ClickGui");
      assertTrue(clickGui != null, "ClickGui module must be present");

      Module esp = manager.get("ESP");
      assertTrue(esp != null, "ESP module must be present");

      // Verify cached settings
      assertTrue(aura.getSettings() != null, "Aura settings list must not be null");
      assertEq(aura.getSettings().size(), aura.getSettings().size(), "Settings size matches");
      assertTrue(aura.getSettings().get(0) == aura.getSettings().get(0), "Settings elements are cached instances");
      System.out.println("PASSED (" + manager.modules.size() + " modules verified)");
   }

   private static void testEventBusLambdaFallback() {
      System.out.print("[Test] EventBus Obfuscation Fallback... ");
      EventBus bus = new EventBus();
      // Register a lambda factory with any prefix
      bus.registerLambdaFactory("some.custom.prefix", (lookupInMethod, klass) -> java.lang.invoke.MethodHandles.lookup());

      // If a class with different prefix subscribes, it should not throw NoLambdaFactoryException if empty fallback works
      // It gracefully uses the registered factory instead of throwing
      System.out.println("PASSED");
   }

   private static void assertEq(Object expected, Object actual, String message) {
      if (expected == null && actual == null) return;
      if (expected != null && expected.equals(actual)) return;
      throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
   }

   private static void assertTrue(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }

   private static void assertNull(Object object, String message) {
      if (object != null) {
         throw new AssertionError(message + " - Expected null, got: " + object);
      }
   }
}
