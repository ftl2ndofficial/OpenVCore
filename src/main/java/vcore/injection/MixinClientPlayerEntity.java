package vcore.injection;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
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
import vcore.core.Core;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAfterRotate;
import vcore.events.impl.EventMove;
import vcore.events.impl.EventPostSync;
import vcore.events.impl.EventSprint;
import vcore.events.impl.EventSync;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.events.impl.PostPlayerUpdateEvent;
import vcore.features.modules.Module;

@Mixin(value = LocalPlayer.class, priority = 800)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayer {
   @Unique
   boolean pre_sprint_state = false;
   @Unique
   private boolean updateLock = false;
   @Unique
   private Runnable postAction;

   @Shadow
   public abstract float getViewXRot(float tickDelta);

   @Shadow
   protected abstract void sendPosition();

   public MixinClientPlayerEntity(ClientLevel world, GameProfile profile) {
      super(world, profile);
   }

   @Inject(method = "tick", at = @At("HEAD"))
   public void tickHook(CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         Vcore.EVENT_BUS.post(new PlayerUpdateEvent());
      }
   }

   @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"), require = 0)
   private boolean tickMovementHook(LocalPlayer player) {
      return ModuleManager.noSlow.isEnabled() && ModuleManager.noSlow.canNoSlow() ? false : player.isUsingItem();
   }

   @Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
   public void shouldSlowDownHook(CallbackInfoReturnable<Boolean> cir) {
      if (ModuleManager.noSlow.isEnabled()) {
         if (this.isVisuallyCrawling()) {
            if (ModuleManager.noSlow.crawl.getValue()) {
               cir.setReturnValue(false);
            }
         } else if (ModuleManager.noSlow.sneak.getValue()) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(
      method = "move",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
      ),
      cancellable = true
   )
   public void onMoveHook(MoverType movementType, Vec3 movement, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventMove event = new EventMove(movement.x, movement.y, movement.z);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            super.move(movementType, new Vec3(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
         }
      }
   }

   @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
   private void sendMovementPacketsHook(CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         EventSync event = new EventSync(this.getYRot(), this.getXRot());
         Vcore.EVENT_BUS.post(event);
         this.postAction = event.getPostAction();
         EventSprint e = new EventSprint(this.isSprinting());
         Vcore.EVENT_BUS.post(e);
         Vcore.EVENT_BUS.post(new EventAfterRotate());
         if (e.getSprintState() != Module.mc.player.wasSprinting) {
            if (e.getSprintState()) {
               Module.mc.player.connection.send(new ServerboundPlayerCommandPacket(this, Action.START_SPRINTING));
            } else {
               Module.mc.player.connection.send(new ServerboundPlayerCommandPacket(this, Action.STOP_SPRINTING));
            }

            Module.mc.player.wasSprinting = e.getSprintState();
         }

         this.pre_sprint_state = Module.mc.player.wasSprinting;
         Core.lockSprint = true;
         if (event.isCancelled()) {
            info.cancel();
         }
      }
   }

   @Inject(method = "sendPosition", at = @At("RETURN"), cancellable = true)
   private void sendMovementPacketsPostHook(CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         Module.mc.player.wasSprinting = this.pre_sprint_state;
         Core.lockSprint = false;
         EventPostSync event = new EventPostSync();
         Vcore.EVENT_BUS.post(event);
         if (this.postAction != null) {
            this.postAction.run();
            this.postAction = null;
         }

         if (event.isCancelled()) {
            info.cancel();
         }
      }
   }

   @Inject(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V", ordinal = 0, shift = Shift.AFTER),
      cancellable = true
   )
   private void PostUpdateHook(CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         if (!this.updateLock) {
            PostPlayerUpdateEvent playerUpdateEvent = new PostPlayerUpdateEvent();
            Vcore.EVENT_BUS.post(playerUpdateEvent);
            if (playerUpdateEvent.isCancelled()) {
               info.cancel();
               if (playerUpdateEvent.getIterations() > 0) {
                  for (int i = 0; i < playerUpdateEvent.getIterations(); i++) {
                     this.updateLock = true;
                     this.tick();
                     this.updateLock = false;
                     this.sendPosition();
                  }
               }
            }
         }
      }
   }

   @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
   private void onPushOutOfBlocksHook(double x, double d, CallbackInfo info) {
      if (ModuleManager.noPush.isEnabled() && ModuleManager.noPush.blocks.getValue()) {
         info.cancel();
      }
   }

   @Inject(method = "handleConfusionTransitionEffect", at = @At("HEAD"), cancellable = true)
   private void updateNauseaHook(CallbackInfo ci) {
      if (ModuleManager.portalInventory.isEnabled()) {
         ci.cancel();
      }
   }
}
