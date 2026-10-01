package vcore.injection;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAttack;
import vcore.events.impl.EventPlayerTravel;
import vcore.features.modules.Module;
import vcore.features.modules.movement.AutoSprint;
import vcore.features.modules.movement.Speed;

@Mixin(value = Player.class, priority = 800)
public class MixinPlayerEntity {
   @Inject(
      method = "causeExtraKnockback",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V", shift = Shift.AFTER)
   )
   public void attackAHook(
      Entity entity,
      float knockbackAmount,
      Vec3 oldMovement,
      CallbackInfo callbackInfo
   ) {
      if (ModuleManager.autoSprint.isEnabled() && AutoSprint.sprint.getValue()) {
         float multiplier = 0.6F + 0.4F * AutoSprint.motion.getValue();
         Module.mc
            .player
            .setDeltaMovement(
               Module.mc.player.getDeltaMovement().x / 0.6 * multiplier, Module.mc.player.getDeltaMovement().y, Module.mc.player.getDeltaMovement().z / 0.6 * multiplier
            );
         Module.mc.player.setSprinting(true);
      }
   }

   @Inject(method = "getSpeed", at = @At("HEAD"), cancellable = true)
   public void getMovementSpeedHook(CallbackInfoReturnable<Float> cir) {
      if (ModuleManager.speed.isEnabled() && ModuleManager.speed.mode.is(Speed.Mode.Vanilla)) {
         cir.setReturnValue(ModuleManager.speed.boostFactor.getValue());
      }
   }

   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void attackAHook2(Entity target, CallbackInfo ci) {
      EventAttack event = new EventAttack(target, false);
      Vcore.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
   private void onTravelhookPre(Vec3 movementInput, CallbackInfo ci) {
      if (Module.mc.player != null) {
         EventPlayerTravel event = new EventPlayerTravel(movementInput, true);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            Module.mc.player.move(MoverType.SELF, Module.mc.player.getDeltaMovement());
            ci.cancel();
         }
      }
   }

   @Inject(method = "travel", at = @At("RETURN"), cancellable = true)
   private void onTravelhookPost(Vec3 movementInput, CallbackInfo ci) {
      if (Module.mc.player != null) {
         EventPlayerTravel event = new EventPlayerTravel(movementInput, false);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            Module.mc.player.move(MoverType.SELF, Module.mc.player.getDeltaMovement());
            ci.cancel();
         }
      }
   }



   @Inject(method = "blockInteractionRange", at = @At("HEAD"), cancellable = true)
   public void getBlockInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
      if (ModuleManager.reach.isEnabled()) {
         if (ModuleManager.reach.Creative.getValue() && Module.mc.player.isCreative()) {
            cir.setReturnValue((double)ModuleManager.reach.creativeBlocksRange.getValue().floatValue());
         } else {
            cir.setReturnValue((double)ModuleManager.reach.blocksRange.getValue().floatValue());
         }
      }
   }

   @Inject(method = "entityInteractionRange", at = @At("HEAD"), cancellable = true)
   public void getEntityInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
      if (ModuleManager.reach.isEnabled()) {
         if (ModuleManager.reach.Creative.getValue() && Module.mc.player.isCreative()) {
            cir.setReturnValue((double)ModuleManager.reach.creativeEntityRange.getValue().floatValue());
         } else {
            cir.setReturnValue((double)ModuleManager.reach.entityRange.getValue().floatValue());
         }
      }
   }
}
