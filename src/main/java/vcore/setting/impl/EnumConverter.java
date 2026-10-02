package vcore.setting.impl;

import com.google.common.base.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

public class EnumConverter extends Converter<Enum, JsonElement> {
   private final Class<? extends Enum> clazz;

   public EnumConverter(Class<? extends Enum> clazz) {
      this.clazz = clazz;
   }

   public static int currentEnum(Enum clazz) {
      return clazz != null ? clazz.ordinal() : -1;
   }

   public static Enum<?> increaseEnum(Enum<?> clazz) {
      if (clazz == null) {
         return null;
      }
      Enum<?>[] constants = clazz.getDeclaringClass().getEnumConstants();
      if (constants == null || constants.length == 0) {
         return clazz;
      }
      return constants[(clazz.ordinal() + 1) % constants.length];
   }

   public static Enum<?> setEnumInt(@NotNull Enum<?> clazz, int id) {
      Enum<?>[] constants = clazz.getDeclaringClass().getEnumConstants();
      if (constants == null || constants.length == 0) {
         return clazz;
      }
      if (id >= 0 && id < constants.length) {
         return constants[id];
      }
      return constants[0];
   }

   public static String[] getNames(Enum clazz) {
      if (clazz == null) {
         return new String[0];
      }
      return Arrays.stream((Enum[])clazz.getClass().getEnumConstants()).map(Enum::toString).toArray(String[]::new);
   }

   public static String getProperName(Enum clazz) {
      return clazz != null ? clazz.toString() : "";
   }

   public JsonElement doForward(Enum anEnum) {
      return new JsonPrimitive(anEnum != null ? anEnum.toString() : "");
   }

   public Enum doBackward(JsonElement jsonElement) {
      if (jsonElement == null || !jsonElement.isJsonPrimitive()) {
         return null;
      }
      String raw = jsonElement.getAsString();
      if (raw == null) {
         return null;
      }

      try {
         return Enum.valueOf(this.clazz, raw);
      } catch (IllegalArgumentException ignored) {
         for (Enum e : this.clazz.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(raw) || e.toString().equalsIgnoreCase(raw)) {
               return e;
            }
         }

         return null;
      }
   }
}
