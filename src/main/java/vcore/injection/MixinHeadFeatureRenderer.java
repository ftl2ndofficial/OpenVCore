package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(CustomHeadLayer.class)
public class MixinHeadFeatureRenderer<S extends LivingEntityRenderState, M extends EntityModel<S> & HeadedModel> {
   @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
   private void onRenderHeadSlot(
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      S state,
      float yRot,
      float xRot,
      CallbackInfo ci
   ) {
      if (ModuleManager.customModel != null && ModuleManager.customModel.shouldHideArmorSlotRender(state)) {
         ci.cancel();
      }
   }
}
