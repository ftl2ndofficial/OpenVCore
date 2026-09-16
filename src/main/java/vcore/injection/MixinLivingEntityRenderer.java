package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
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
import vcore.utility.render.Render3DEngine;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
   private LivingEntity lastEntity;
   private float originalHeadYaw;
   private float originalPrevHeadYaw;
   private float originalPrevHeadPitch;
   private float originalHeadPitch;
   private float originalBodyYaw;
   private float originalPrevBodyYaw;
   @Shadow
   protected M model;
   @Shadow
   @Final
   protected List<RenderLayer<T, M>> layers;

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   public void onRenderPre(T livingEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         boolean applyRenderRotations = this.shouldApplyRenderRotations(livingEntity);
         if (applyRenderRotations) {
            Player player = Module.mc.player;
            IClientPlayerEntity playerAccessor = (IClientPlayerEntity)player;
            float tickDelta = Render3DEngine.getTickDelta();
            float interpolatedBodyYaw = Render2DEngine.interpolateFloat(Managers.PLAYER.prevBodyYaw, Managers.PLAYER.bodyYaw, tickDelta);
            this.originalHeadYaw = livingEntity.yHeadRot;
            this.originalPrevHeadYaw = livingEntity.yHeadRotO;
            this.originalPrevHeadPitch = livingEntity.xRotO;
            this.originalHeadPitch = livingEntity.getXRot();
            this.originalBodyYaw = livingEntity.yBodyRot;
            this.originalPrevBodyYaw = livingEntity.yBodyRotO;
            livingEntity.setXRot(playerAccessor.getLastPitch());
            livingEntity.xRotO = Managers.PLAYER.lastPitch;
            livingEntity.yHeadRot = playerAccessor.getLastYaw();
            livingEntity.yBodyRot = interpolatedBodyYaw;
            livingEntity.yHeadRotO = Managers.PLAYER.lastYaw;
            livingEntity.yBodyRotO = interpolatedBodyYaw;
         }

         if (livingEntity != Module.mc.player
            && ModuleManager.freeCam.isEnabled()
            && ModuleManager.freeCam.track.getValue()
            && ModuleManager.freeCam.trackEntity != null
            && ModuleManager.freeCam.trackEntity == livingEntity) {
            ci.cancel();
         } else {
            this.lastEntity = livingEntity;
         }
      }
   }

   @Unique
   public void postRender(T livingEntity) {
      if (!Module.fullNullCheck()) {
         if (this.shouldApplyRenderRotations(livingEntity)) {
            livingEntity.xRotO = this.originalPrevHeadPitch;
            livingEntity.setXRot(this.originalHeadPitch);
            livingEntity.yHeadRot = this.originalHeadYaw;
            livingEntity.yHeadRotO = this.originalPrevHeadYaw;
            livingEntity.yBodyRot = this.originalBodyYaw;
            livingEntity.yBodyRotO = this.originalPrevBodyYaw;
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

   @Inject(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
         shift = Shift.AFTER
      )
   )
   public void onRenderModelAfter(
      T livingEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci
   ) {
      if (!Module.fullNullCheck()) {
         if (livingEntity == Module.mc.player && ModuleManager.hat.isEnabled() && this.model instanceof PlayerModel<?> playerModel) {
            matrixStack.pushPose();
            playerModel.head.translateAndRotate(matrixStack);
            float yOffset = Hat.getYOffset(livingEntity);
            matrixStack.translate(0.0F, yOffset, 0.0F);
            matrixStack.mulPose(Axis.ZN.rotationDegrees(180.0F));
            matrixStack.mulPose(Axis.YP.rotationDegrees(-180.0F));
            ModuleManager.hat.renderHat(matrixStack);
            matrixStack.popPose();
         }
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   public void onRenderPost(T livingEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         this.postRender(livingEntity);
      }
   }

   @ModifyArgs(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private void renderHook(Args args) {
      if (!Module.fullNullCheck()) {
         if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.antiPlayerCollision.getValue()) {
            if (this.lastEntity != Module.mc.player && this.lastEntity instanceof Player pl && !pl.isInvisible()) {
               float alpha = MathUtility.clamp((float)(Module.mc.player.distanceToSqr(this.lastEntity.position()) / 3.0) + 0.2F, 0.0F, 1.0F);
               args.set(4, Render2DEngine.applyOpacity(654311423, alpha));
            }
         }
      }
   }
}
