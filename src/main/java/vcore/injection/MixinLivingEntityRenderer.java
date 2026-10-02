package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.misc.ClientSettings;
import vcore.features.modules.render.Hat;
import vcore.injection.accesors.IClientPlayerEntity;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   @Shadow
   protected M model;
   @Shadow
   @Final
   protected List<RenderLayer<S, M>> layers;

   @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
   public void onExtractRenderState(T livingEntity, S state, float partialTicks, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if (livingEntity == Module.mc.player && this.shouldApplyRenderRotations(livingEntity)) {
            IClientPlayerEntity playerAccessor = (IClientPlayerEntity)Module.mc.player;
            float interpolatedBodyYaw = Render2DEngine.interpolateFloat(Managers.PLAYER.prevBodyYaw, Managers.PLAYER.bodyYaw, partialTicks);
            state.bodyRot = interpolatedBodyYaw;
            state.yRot = Mth.wrapDegrees(playerAccessor.getLastYaw() - state.bodyRot);
            state.xRot = playerAccessor.getLastPitch();
         }
      }
   }

   @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
   public void onSubmitPre(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
      if (state instanceof AvatarRenderState avatarState && ModuleManager.customModel != null) {
         ModuleManager.customModel.beginPlayerRender(avatarState);
      }
      if (ModuleManager.freeCam.isEnabled()
         && ModuleManager.freeCam.track.getValue()
         && ModuleManager.freeCam.trackEntity != null
         && state instanceof AvatarRenderState avatarState
         && ModuleManager.freeCam.trackEntity.getId() == avatarState.id) {
         ci.cancel();
      }
   }

   @org.spongepowered.asm.mixin.injection.Redirect(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
      )
   )
   private <M_STATE> void redirectSubmitModel(
      SubmitNodeCollector collector,
      net.minecraft.client.model.Model<? super M_STATE> model,
      M_STATE modelState,
      PoseStack poseStack,
      net.minecraft.client.renderer.rendertype.RenderType renderType,
      int lightCoords,
      int overlayCoords,
      int tint,
      net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
      int outlineColor,
      net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
      boolean renderedCustom = false;
      if (modelState instanceof AvatarRenderState avatarState && ModuleManager.customModel != null && ModuleManager.customModel.isEnabled() && ModuleManager.customModel.shouldApply(avatarState)) {
         if (model instanceof PlayerModel playerModel) {
            playerModel.setupAnim(avatarState);
            if (ModuleManager.customModel.renderCustomPlayerModel(avatarState, playerModel, poseStack, collector, renderType, lightCoords, overlayCoords, tint)) {
               renderedCustom = true;
            }
         }
      }
      if (!renderedCustom) {
         collector.submitModel(model, modelState, poseStack, renderType, lightCoords, overlayCoords, tint, sprite, outlineColor, crumblingOverlay);
      }

      if (!Module.fullNullCheck() && ModuleManager.hat != null && ModuleManager.hat.isEnabled()) {
         if (modelState instanceof AvatarRenderState avatarState && Module.mc.player != null && avatarState.id == Module.mc.player.getId()) {
            if (!(Module.mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen)) {
               if (!Module.mc.options.getCameraType().isFirstPerson() || ModuleManager.freeCam.isEnabled()) {
                  if (model instanceof PlayerModel playerModel) {
                     playerModel.setupAnim(avatarState);
                     if (renderedCustom) {
                        ModuleManager.customModel.renderHat(avatarState, poseStack, collector);
                     } else {
                        poseStack.pushPose();
                        playerModel.head.translateAndRotate(poseStack);
                        float yOffset = Hat.getYOffset(Module.mc.player);
                        poseStack.translate(0.0F, yOffset, 0.0F);
                        poseStack.mulPose(Axis.ZN.rotationDegrees(180.0F));
                        poseStack.mulPose(Axis.YP.rotationDegrees(-180.0F));
                        ModuleManager.hat.renderHat(poseStack, collector);
                        poseStack.popPose();
                     }
                  }
               }
            }
         }
      }
   }

   @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("RETURN"))
   public void onSubmitPost(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
      if (state instanceof AvatarRenderState avatarState && ModuleManager.customModel != null) {
         ModuleManager.customModel.endPlayerRender(avatarState);
      }
   }

   @Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
   private void getModelTintHook(S state, CallbackInfoReturnable<Integer> cir) {
      if (!Module.fullNullCheck() && ModuleManager.noRender.isEnabled() && ModuleManager.noRender.antiPlayerCollision.getValue()) {
         if (state instanceof AvatarRenderState avatarState && Module.mc.player != null && avatarState.id != Module.mc.player.getId()) {
            float alpha = MathUtility.clamp((float)(avatarState.distanceToCameraSq / 3.0) + 0.2F, 0.0F, 1.0F);
            cir.setReturnValue(Render2DEngine.applyOpacity(-1, alpha));
         }
      }
   }

   @Unique
   private boolean shouldApplyRenderRotations(LivingEntity livingEntity) {
      if (Module.mc.player == null || livingEntity != Module.mc.player) {
         return false;
      } else if (!ClientSettings.renderRotations.getValue() || Vcore.isFuturePresent()) {
         return false;
      } else {
         return Module.mc.player.isPassenger() ? false : this.isClientRotationSystemActive();
      }
   }

   @Unique
   private boolean isClientRotationSystemActive() {
      if (ModuleManager.aura.isEnabled() && Aura.target != null && ModuleManager.aura.rotationMode.not(Aura.Mode.None)) {
         return true;
      }

      IClientPlayerEntity playerAccessor = (IClientPlayerEntity)Module.mc.player;
      float yawDelta = Math.abs(Mth.wrapDegrees(playerAccessor.getLastYaw() - Managers.PLAYER.yaw));
      float pitchDelta = Math.abs(playerAccessor.getLastPitch() - Managers.PLAYER.pitch);
      return yawDelta > 0.5F || pitchDelta > 0.5F;
   }
}
