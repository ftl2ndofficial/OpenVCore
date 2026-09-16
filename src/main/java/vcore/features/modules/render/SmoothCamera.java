package vcore.features.modules.render;

import net.minecraft.client.CameraType;
import net.minecraft.world.phys.Vec3;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class SmoothCamera extends Module {
   private final Setting<Boolean> enableFirstPOV = new Setting<>("FirstPOV", false);
   private final Setting<Boolean> resetOnPerspectiveChange = new Setting<>("ResetView", true);
   private final Setting<Float> horizontalFactor = new Setting<>("HorizontalFactor", 0.9F, 0.0F, 0.95F);
   private final Setting<Float> verticalFactor = new Setting<>("VerticalFactor", 0.9F, 0.0F, 0.95F);
   private Vec3 smoothPos = Vec3.ZERO;
   private CameraType lastPerspective;

   public SmoothCamera() {
      super("SmoothCamera", "Makes your camera move smoother.", Module.Category.RENDER);
   }

   @Override
   public void onDisable() {
      this.resetState();
   }

   @Override
   public void onLogout() {
      this.resetState();
   }

   public Vec3 getSmoothedPosition(double x, double y, double z) {
      Vec3 currentPos = new Vec3(x, y, z);
      CameraType currentPerspective = mc.options.getCameraType();
      if (this.resetOnPerspectiveChange.getValue() && this.lastPerspective != currentPerspective) {
         this.smoothPos = currentPos;
         this.lastPerspective = currentPerspective;
         return currentPos;
      }

      this.lastPerspective = currentPerspective;
      if (!this.enableFirstPOV.getValue() && currentPerspective == CameraType.FIRST_PERSON) {
         this.smoothPos = currentPos;
         return currentPos;
      }

      if (this.isLikelyZero(this.smoothPos)) {
         this.smoothPos = currentPos;
      }

      double horizontal = this.horizontalFactor.getValue().floatValue();
      double vertical = this.verticalFactor.getValue().floatValue();
      this.smoothPos = new Vec3(
         this.smoothPos.x * horizontal + x * (1.0 - horizontal),
         this.smoothPos.y * vertical + y * (1.0 - vertical),
         this.smoothPos.z * horizontal + z * (1.0 - horizontal)
      );
      return this.smoothPos;
   }

   private void resetState() {
      this.smoothPos = Vec3.ZERO;
      this.lastPerspective = null;
   }

   private boolean isLikelyZero(Vec3 vec) {
      return Math.abs(vec.x) < 0.001 && Math.abs(vec.y) < 0.001 && Math.abs(vec.z) < 0.001;
   }
}
