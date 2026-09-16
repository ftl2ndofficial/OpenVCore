package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.render.animation.AnimationUtility;

public class NoCameraClip extends Module {
   public Setting<Boolean> antiFront = new Setting<>("AntiFront", false);
   public Setting<Float> distance = new Setting<>("Distance", 3.0F, 1.0F, 20.0F);
   private float animation;

   public NoCameraClip() {
      super("NoCameraClip", "Removes camera collision.", Module.Category.RENDER);
   }

   @Override
   public void onRender3D(PoseStack matrix) {
      if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
         this.animation = AnimationUtility.fast(this.animation, 0.0F, 10.0F);
      } else {
         this.animation = AnimationUtility.fast(this.animation, 1.0F, 10.0F);
      }

      if (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT && this.antiFront.getValue()) {
         mc.options.setCameraType(CameraType.FIRST_PERSON);
      }
   }

   public float getDistance() {
      return 1.0F + (this.distance.getValue() - 1.0F) * this.animation;
   }
}
