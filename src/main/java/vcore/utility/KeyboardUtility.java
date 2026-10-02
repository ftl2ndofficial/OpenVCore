package vcore.utility;

import java.util.HashMap;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

public final class KeyboardUtility {
   private static final Map<Integer, String> KEY_NAMES = new HashMap<>(128);

   static {
      register(GLFW.GLFW_KEY_SPACE, "Space");
      register(GLFW.GLFW_KEY_APOSTROPHE, "'");
      register(GLFW.GLFW_KEY_COMMA, ",");
      register(GLFW.GLFW_KEY_MINUS, "-");
      register(GLFW.GLFW_KEY_PERIOD, ".");
      register(GLFW.GLFW_KEY_SLASH, "/");
      register(GLFW.GLFW_KEY_SEMICOLON, ";");
      register(GLFW.GLFW_KEY_EQUAL, "=");
      register(GLFW.GLFW_KEY_LEFT_BRACKET, "[");
      register(GLFW.GLFW_KEY_BACKSLASH, "\\");
      register(GLFW.GLFW_KEY_RIGHT_BRACKET, "]");
      register(GLFW.GLFW_KEY_GRAVE_ACCENT, "`");

      register(GLFW.GLFW_KEY_ESCAPE, "Esc");
      register(GLFW.GLFW_KEY_ENTER, "Enter");
      register(GLFW.GLFW_KEY_TAB, "Tab");
      register(GLFW.GLFW_KEY_BACKSPACE, "Back");
      register(GLFW.GLFW_KEY_INSERT, "Ins");
      register(GLFW.GLFW_KEY_DELETE, "Del");
      register(GLFW.GLFW_KEY_RIGHT, "Right");
      register(GLFW.GLFW_KEY_LEFT, "Left");
      register(GLFW.GLFW_KEY_DOWN, "Down");
      register(GLFW.GLFW_KEY_UP, "Up");
      register(GLFW.GLFW_KEY_PAGE_UP, "PgUp");
      register(GLFW.GLFW_KEY_PAGE_DOWN, "PgDn");
      register(GLFW.GLFW_KEY_HOME, "Home");
      register(GLFW.GLFW_KEY_END, "End");
      register(GLFW.GLFW_KEY_CAPS_LOCK, "Caps");
      register(GLFW.GLFW_KEY_SCROLL_LOCK, "ScrollLock");
      register(GLFW.GLFW_KEY_NUM_LOCK, "NumLock");
      register(GLFW.GLFW_KEY_PRINT_SCREEN, "PrintScreen");
      register(GLFW.GLFW_KEY_PAUSE, "Pause");

      register(GLFW.GLFW_KEY_LEFT_SHIFT, "LShift");
      register(GLFW.GLFW_KEY_LEFT_CONTROL, "LCtrl");
      register(GLFW.GLFW_KEY_LEFT_ALT, "LAlt");
      register(GLFW.GLFW_KEY_LEFT_SUPER, "LWin");
      register(GLFW.GLFW_KEY_RIGHT_SHIFT, "RShift");
      register(GLFW.GLFW_KEY_RIGHT_CONTROL, "RCtrl");
      register(GLFW.GLFW_KEY_RIGHT_ALT, "RAlt");
      register(GLFW.GLFW_KEY_RIGHT_SUPER, "RWin");
      register(GLFW.GLFW_KEY_MENU, "Menu");

      for (int i = 1; i <= 25; i++) {
         register(GLFW.GLFW_KEY_F1 + (i - 1), "F" + i);
      }

      for (int i = 0; i <= 9; i++) {
         register(GLFW.GLFW_KEY_KP_0 + i, "Num" + i);
      }
      register(GLFW.GLFW_KEY_KP_DECIMAL, "Num.");
      register(GLFW.GLFW_KEY_KP_DIVIDE, "Num/");
      register(GLFW.GLFW_KEY_KP_MULTIPLY, "Num*");
      register(GLFW.GLFW_KEY_KP_SUBTRACT, "Num-");
      register(GLFW.GLFW_KEY_KP_ADD, "Num+");
      register(GLFW.GLFW_KEY_KP_ENTER, "NumEnter");
      register(GLFW.GLFW_KEY_KP_EQUAL, "Num=");
   }

   private static void register(int key, String name) {
      KEY_NAMES.put(key, name);
   }

   private KeyboardUtility() {}

   /**
    * Returns a clean, human-readable name for a GLFW key code without using reflection.
    */
   public static String getKeyName(int key) {
      if (key == -1 || key == 0) {
         return "None";
      }

      String mapped = KEY_NAMES.get(key);
      if (mapped != null) {
         return mapped;
      }

      try {
         String glfwName = GLFW.glfwGetKeyName(key, GLFW.glfwGetKeyScancode(key));
         if (glfwName == null) {
            glfwName = GLFW.glfwGetKeyName(key, 0);
         }
         if (glfwName != null && !glfwName.isEmpty()) {
            return glfwName.length() == 1 ? glfwName.toUpperCase() : capitalize(glfwName);
         }
      } catch (Throwable ignored) {
      }

      return "Key " + key;
   }

   /**
    * Returns the bind string format used for Configs and Displays (e.g. "M1", "LSHIFT", "SPACE").
    */
   public static String getBindName(int key, boolean mouse) {
      if (mouse) {
         return "M" + key;
      }
      return getKeyName(key).toUpperCase();
   }

   private static String capitalize(String str) {
      if (str == null || str.isEmpty()) return str;
      return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
   }
}
