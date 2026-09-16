package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(HumanoidArmorLayer.class)
public class MixinArmorFeatureRenderer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {
   @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
   private void onRenderArmor(
      PoseStack matrices, MultiBufferSource vertexConsumers, T livingEntity, EquipmentSlot equipmentSlot, int i, A bipedEntityModel, CallbackInfo ci
   ) {
      if (ModuleManager.customModel != null && ModuleManager.customModel.shouldHideArmorSlotRender(livingEntity)) {
         ci.cancel();
      } else {
         if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.armor.getValue()) {
            ci.cancel();
         }
      }
   }
}
