package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.ItemPhysics;
import vcore.utility.interfaces.IItemEntityRenderState;

@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
   protected MixinItemEntityRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void onExtractRenderState(ItemEntity entity, ItemEntityRenderState state, float partialTicks, CallbackInfo ci) {
      if (state instanceof IItemEntityRenderState accessor) {
         accessor.vcore$setUUID(entity.getUUID());
         accessor.vcore$setId(entity.getId());
         accessor.vcore$setOnGround(entity.onGround());
         accessor.vcore$setInWater(entity.isInWater());
         accessor.vcore$setInLava(entity.isInLava());
         accessor.vcore$setTickDelta(partialTicks);
      }
   }

   @Inject(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void onSubmit(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
      ItemPhysics itemPhysics = ModuleManager.itemPhysics;
      if (itemPhysics != null && itemPhysics.isEnabled() && !state.item.isEmpty()) {
         itemPhysics.onRenderItemEntity(state, poseStack, submitNodeCollector, camera);
         super.submit(state, poseStack, submitNodeCollector, camera);
         ci.cancel();
      }
   }
}
