package vcore.injection;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventFixVelocity;
import vcore.features.modules.Module;
import vcore.features.modules.combat.HitBox;
import vcore.features.modules.movement.freelook.CameraOverriddenEntity;
import vcore.features.modules.movement.freelook.FreeLookState;
import vcore.utility.interfaces.IEntity;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEntity, CameraOverriddenEntity {
   @Unique
   private float cameraYaw;
   @Unique
   private float cameraPitch;
   @Shadow
   private AABB bb;

   @Shadow
   protected abstract BlockPos getBlockPosBelowThatAffectsMyMovement();

   @Override
   public BlockPos Vcore$getVelocityBP() {
      return this.getBlockPosBelowThatAffectsMyMovement();
   }

   @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   public void pushHook(Entity entity, CallbackInfo ci) {
      if (ModuleManager.noPush != null && ModuleManager.noPush.isEnabled() && ModuleManager.noPush.players.getValue()) {
         if ((Object)this == Module.mc.player || entity == Module.mc.player) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
   public void updateVelocityHook(float speed, Vec3 movementInput, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if ((Object)this == Module.mc.player) {
            ci.cancel();
            EventFixVelocity event = new EventFixVelocity(
               movementInput, speed, Module.mc.player.getYRot(), movementInputToVelocityC(movementInput, speed, Module.mc.player.getYRot())
            );
            Vcore.EVENT_BUS.post(event);
            Module.mc.player.setDeltaMovement(Module.mc.player.getDeltaMovement().add(event.getVelocity()));
         }
      }
   }

   @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
   private void onChangeLookDirection(double deltaX, double deltaY, CallbackInfo ci) {
      Entity self = (Entity)(Object)this;
      if (FreeLookState.active && self instanceof LocalPlayer) {
         this.cameraYaw += (float)deltaX * 0.15F;
         this.cameraPitch = Mth.clamp(this.cameraPitch + (float)deltaY * 0.15F, -90.0F, 90.0F);
         ci.cancel();
      }
   }

   @Override
   public float getCameraYaw() {
      return this.cameraYaw;
   }

   @Override
   public float getCameraPitch() {
      return this.cameraPitch;
   }

   @Override
   public void setCameraYaw(float yaw) {
      this.cameraYaw = yaw;
   }

   @Override
   public void setCameraPitch(float pitch) {
      this.cameraPitch = pitch;
   }

   @Unique
   private static Vec3 movementInputToVelocityC(Vec3 movementInput, float speed, float yaw) {
      double d = movementInput.lengthSqr();
      if (d < 1.0E-7) {
         return Vec3.ZERO;
      }

      Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
      float f = Mth.sin(yaw * (float) (Math.PI / 180.0));
      float g = Mth.cos(yaw * (float) (Math.PI / 180.0));
      return new Vec3(vec3d.x * g - vec3d.z * f, vec3d.y, vec3d.z * g + vec3d.x * f);
   }

   @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
   public final void getBoundingBox(CallbackInfoReturnable<AABB> cir) {
      Entity self = (Entity)(Object)this;
      if (!HitBox.isSkippingExpand()
         && ModuleManager.hitBox.isEnabled()
         && Module.mc != null
         && Module.mc.player != null
         && self instanceof Player
         && self.getId() != Module.mc.player.getId()
         && ModuleManager.hitBox.shouldExpand(self)) {
         cir.setReturnValue(
            new AABB(
               this.bb.minX - HitBox.XZExpand.getValue() / 2.0F,
               this.bb.minY - HitBox.YExpand.getValue() / 2.0F,
               this.bb.minZ - HitBox.XZExpand.getValue() / 2.0F,
               this.bb.maxX + HitBox.XZExpand.getValue() / 2.0F,
               this.bb.maxY + HitBox.YExpand.getValue() / 2.0F,
               this.bb.maxZ + HitBox.XZExpand.getValue() / 2.0F
            )
         );
      }
   }

   @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
   public void isOnFireHook(CallbackInfoReturnable<Boolean> cir) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fireEntity.getValue()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "isInLava", at = @At("HEAD"), cancellable = true)
   public void isInLavaHook(CallbackInfoReturnable<Boolean> cir) {
   }

   @Inject(method = "isInWater", at = @At("HEAD"), cancellable = true)
   public void isTouchingWaterHook(CallbackInfoReturnable<Boolean> cir) {
   }

   @Inject(method = "setSwimming", at = @At("HEAD"), cancellable = true)
   public void setSwimmingHook(boolean swimming, CallbackInfo ci) {
   }
}
