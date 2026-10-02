package vcore.features.modules.render;

import vcore.features.modules.Module;

public class Fullbright extends Module {
   public static final float MIN_BRIGHT = 0.5F;

   public Fullbright() {
      super("Fullbright", "Removes darkness.", Module.Category.RENDER);
   }

   @Override
   public void onEnable() {
      if (mc.gameRenderer != null && mc.gameRenderer.lightTexture() != null) {
         mc.gameRenderer.lightTexture().tick();
      }
   }

   @Override
   public void onDisable() {
      if (mc.gameRenderer != null && mc.gameRenderer.lightTexture() != null) {
         mc.gameRenderer.lightTexture().tick();
      }
   }
}
