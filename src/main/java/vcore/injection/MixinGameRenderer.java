package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.utility.math.FrameRateCounter;
import vcore.utility.render.BlockAnimationUtility;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.RenderSystem;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
   @Shadow
   private Camera mainCamera;

   @Shadow
   public abstract void tick();

   @Shadow
   abstract float getFov(Camera camera, float partialTick, boolean useFovSetting);

   @Shadow
   public abstract Matrix4f getProjectionMatrix(float fov);

   @Shadow
   public abstract float getDepthFar();

   @Inject(method = "render", at = @At("TAIL"))
   void postHudRenderHook(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
      FrameRateCounter.INSTANCE.recordFrame();
   }

   @Inject(method = "renderItemInHand(FZLorg/joml/Matrix4f;)V", at = @At("HEAD"))
   void render3dHook(float partialTick, boolean sleeping, Matrix4f projectionMatrix, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         Camera camera = this.mainCamera;
         PoseStack matrixStack = new PoseStack();
         RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.last().pose());
         matrixStack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
         matrixStack.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
         RenderSystem.applyModelViewMatrix();
         float fov = this.getFov(this.mainCamera, partialTick, true);
         Render3DEngine.lastProjMat.set(this.getProjectionMatrix(fov));
         Render3DEngine.lastModMat.set(RenderSystem.getModelViewMatrix());
         Render3DEngine.lastWorldSpaceMatrix.set(matrixStack.last().pose());
         Render3DEngine.rendering3D = true;
         try {
            Managers.MODULE.onRender3D(matrixStack);
            BlockAnimationUtility.onRender(matrixStack);
            Render3DEngine.onRender3D(matrixStack);
         } finally {
            Render3DEngine.rendering3D = false;
         }
         RenderSystem.getModelViewStack().popMatrix();
         RenderSystem.applyModelViewMatrix();
      }
   }

   @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"))
   private float renderWorldHook(float delta, float first, float second) {
      return ModuleManager.noRender.isEnabled() && ModuleManager.noRender.nausea.getValue() ? 0.0F : Mth.lerp(delta, first, second);
   }

   @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
   private void bobViewHook(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noBob.getValue()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
   private void tiltViewWhenHurtHook(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.hurtCam.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "pick(F)V", at = @At("HEAD"), cancellable = true)
   private void onPick(float partialTick, CallbackInfo ci) {
      if (!Module.fullNullCheck() && ModuleManager.freeCam.isEnabled()) {
         ci.cancel();
         Module.mc.hitResult = Managers.PLAYER.getRtxTarget(
            ModuleManager.freeCam.getFakeYaw(),
            ModuleManager.freeCam.getFakePitch(),
            ModuleManager.freeCam.getFakeX(),
            ModuleManager.freeCam.getFakeY(),
            ModuleManager.freeCam.getFakeZ()
         );
      }
   }

   @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
   private void getFovHook(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
      float fragZoom = ModuleManager.fragEffects.getMulCameraZoomValue();
      if (fragZoom != 1.0F) {
         cir.setReturnValue(cir.getReturnValue() / fragZoom);
      }
   }

   @Inject(method = "getProjectionMatrix", at = @At("RETURN"), cancellable = true)
   private void onGetProjectionMatrix(float fov, CallbackInfoReturnable<Matrix4f> cir) {
      if (ModuleManager.aspectRatio.isEnabled()) {
         float ratio = ModuleManager.aspectRatio.getRatioValue();
         Matrix4f matrix4f = new Matrix4f();
         matrix4f.perspective(
            fov * (float) (Math.PI / 180.0),
            ratio,
            0.05F,
            this.getDepthFar()
         );
         cir.setReturnValue(matrix4f);
      }
   }
}
