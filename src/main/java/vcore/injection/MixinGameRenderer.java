package vcore.injection;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.player.NoEntityTrace;
import vcore.utility.math.FrameRateCounter;
import vcore.utility.render.BlockAnimationUtility;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.shaders.satin.impl.ReloadableShaderEffectManager;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
   @Shadow
   private float zoom;
   @Shadow
   private float zoomX;
   @Shadow
   private float zoomY;
   @Shadow
   private float renderDistance;
   @Unique
   private boolean vcore$renderingHand;

   @Shadow
   public abstract void tick();

   @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 1, shift = Shift.BEFORE), method = "render")
   void postHudRenderHook(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
      FrameRateCounter.INSTANCE.recordFrame();
   }

   @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z", opcode = 180, ordinal = 0), method = "renderLevel")
   void render3dHook(DeltaTracker tickCounter, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         Camera camera = Module.mc.gameRenderer.getMainCamera();
         PoseStack matrixStack = new PoseStack();
         RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.last().pose());
         matrixStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
         matrixStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
         RenderSystem.applyModelViewMatrix();
         Render3DEngine.lastProjMat.set(RenderSystem.getProjectionMatrix());
         Render3DEngine.lastModMat.set(RenderSystem.getModelViewMatrix());
         Render3DEngine.lastWorldSpaceMatrix.set(matrixStack.last().pose());
         Managers.MODULE.onRender3D(matrixStack);
         BlockAnimationUtility.onRender(matrixStack);
         Render3DEngine.onRender3D(matrixStack);
         RenderSystem.getModelViewStack().popMatrix();
         RenderSystem.applyModelViewMatrix();
      }
   }

   @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"))
   private float renderWorldHook(float delta, float first, float second) {
      return ModuleManager.noRender.isEnabled() && ModuleManager.noRender.nausea.getValue() ? 0.0F : Mth.lerp(delta, first, second);
   }

   @Inject(method = "reloadShaders", at = @At("RETURN"))
   private void loadSatinPrograms(ResourceProvider factory, CallbackInfo ci) {
      ReloadableShaderEffectManager.INSTANCE.reload(factory);
   }

   @Inject(
      method = "pick(F)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GameRenderer;pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;"
      ),
      cancellable = true
   )
   private void onUpdateTargetedEntity(float tickDelta, CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         if (ModuleManager.freeCam.isEnabled()) {
            Module.mc.getProfiler().pop();
            info.cancel();
            Module.mc.hitResult = Managers.PLAYER
               .getRtxTarget(
                  ModuleManager.freeCam.getFakeYaw(),
                  ModuleManager.freeCam.getFakePitch(),
                  ModuleManager.freeCam.getFakeX(),
                  ModuleManager.freeCam.getFakeY(),
                  ModuleManager.freeCam.getFakeZ()
               );
         }
      }
   }

   @Inject(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At("HEAD"), cancellable = true)
   private void findCrosshairTargetHook(
      Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta, CallbackInfoReturnable<HitResult> cir
   ) {
      boolean disableEntityTrace = this.shouldDisableEntityTrace();
      if (disableEntityTrace) {
         double d = Math.max(blockInteractionRange, entityInteractionRange);
         Vec3 vec3d = camera.getEyePosition(tickDelta);
         HitResult hitResult = camera.pick(d, tickDelta, false);
         cir.setReturnValue(this.ensureTargetInRangeCustom(hitResult, vec3d, blockInteractionRange));
      }
   }

   @Unique
   private boolean shouldDisableEntityTrace() {
      if (ModuleManager.noEntityTrace.isEnabled() && Module.mc.player != null) {
         Item mainItem = Module.mc.player.getMainHandItem().getItem();
         return mainItem == Items.COBWEB && NoEntityTrace.cobweb.getValue()
            || mainItem instanceof PickaxeItem && NoEntityTrace.pickaxe.getValue()
            || mainItem instanceof AxeItem && NoEntityTrace.axe.getValue();
      } else {
         return false;
      }
   }

   @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
   private void getFovHook(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
      float fragZoom = ModuleManager.fragEffects.getMulCameraZoomValue();
      if (fragZoom != 1.0F) {
         cir.setReturnValue((Double)cir.getReturnValue() / fragZoom);
      }
   }

   @Inject(method = "getProjectionMatrix", at = @At("TAIL"), cancellable = true)
   public void getBasicProjectionMatrixHook(double fov, CallbackInfoReturnable<Matrix4f> cir) {
      if (ModuleManager.aspectRatio.isEnabled() && !this.vcore$renderingHand) {
         PoseStack matrixStack = new PoseStack();
         matrixStack.last().pose().identity();
         if (this.zoom != 1.0F) {
            matrixStack.translate(this.zoomX, -this.zoomY, 0.0F);
            matrixStack.scale(this.zoom, this.zoom, 1.0F);
         }

         float aspect = ModuleManager.aspectRatio.isEnabled()
            ? ModuleManager.aspectRatio.getRatioValue()
            : (float)Module.mc.getWindow().getWidth() / Module.mc.getWindow().getHeight();
         matrixStack.last()
            .pose()
            .mul(new Matrix4f().setPerspective((float)(fov * (float) (Math.PI / 180.0)), aspect, 0.05F, this.renderDistance * 4.0F));
         cir.setReturnValue(matrixStack.last().pose());
      }
   }

   @Inject(method = "renderItemInHand", at = @At("HEAD"))
   private void renderHandHead(Camera camera, float tickDelta, Matrix4f matrix4f, CallbackInfo ci) {
      this.vcore$renderingHand = true;
   }

   @Inject(method = "renderItemInHand", at = @At("RETURN"))
   private void renderHandReturn(Camera camera, float tickDelta, Matrix4f matrix4f, CallbackInfo ci) {
      this.vcore$renderingHand = false;
   }

   @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
   private void bobViewHook(PoseStack matrices, float tickDelta, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noBob.getValue()) {
            ci.cancel();
         } else {
            Vcore.core.bobView(matrices, tickDelta);
            ci.cancel();
         }
      }
   }

   @Unique
   private HitResult ensureTargetInRangeCustom(HitResult hitResult, Vec3 cameraPos, double interactionRange) {
      Vec3 vec3d = hitResult.getLocation();
      if (!vec3d.closerThan(cameraPos, interactionRange)) {
         Vec3 vec3d2 = hitResult.getLocation();
         Direction direction = Direction.getNearest(vec3d2.x - cameraPos.x, vec3d2.y - cameraPos.y, vec3d2.z - cameraPos.z);
         return BlockHitResult.miss(vec3d2, direction, BlockPos.containing(vec3d2));
      } else {
         return hitResult;
      }
   }

   @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
   private void showFloatingItemHook(ItemStack floatingItem, CallbackInfo info) {
      if (ModuleManager.totemAnimation.isEnabled()) {
         ModuleManager.totemAnimation.showFloatingItem(floatingItem);
         info.cancel();
      }
   }

   @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
   private void renderFloatingItemHook(GuiGraphics context, float tickDelta, CallbackInfo ci) {
      if (ModuleManager.totemAnimation.isEnabled()) {
         ModuleManager.totemAnimation.renderFloatingItem(tickDelta);
         ci.cancel();
      }
   }

   @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
   private void tiltViewWhenHurtHook(PoseStack matrices, float tickDelta, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.hurtCam.getValue()) {
         ci.cancel();
      }
   }
}
