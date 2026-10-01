package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(HumanoidArmorLayer.class)
public class MixinArmorFeatureRenderer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> {
   @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
   private void onRenderArmor(
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      ItemStack itemStack,
      EquipmentSlot slot,
      int lightCoords,
      S state,
      CallbackInfo ci
   ) {
      if (ModuleManager.customModel != null && ModuleManager.customModel.shouldHideArmorSlotRender(state)) {
         ci.cancel();
      } else if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.armor.getValue()) {
         ci.cancel();
      }
   }
}
