package vcore.injection;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventTravel;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.movement.WaterSpeed;
import vcore.utility.interfaces.IEntityLiving;

@Mixin(LivingEntity.class)
public class MixinEntityLiving implements IEntityLiving {
   @Shadow
   protected double lerpX;
   @Shadow
   protected double lerpY;
   @Shadow
   protected double lerpZ;
   @Unique
   double prevServerX;
   @Unique
   double prevServerY;
   @Unique
   double prevServerZ;
   @Unique
   public List<Aura.Position> positonHistory = new ArrayList<>();
   @Unique
   private boolean prevFlying = false;

   @Override
   public List<Aura.Position> getPositionHistory() {
      return this.positonHistory;
   }

   @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
   private void getArmSwingAnimationEnd(CallbackInfoReturnable<Integer> info) {
      if ((LivingEntity)(Object)this == Module.mc.player && ModuleManager.animations.shouldChangeAnimationDuration()) {
         info.setReturnValue(ModuleManager.animations.getHandSwingDuration());
      }
   }

   @Inject(method = "lerpTo", at = @At("HEAD"))
   private void updateTrackedPositionAndAnglesHook(double x, double y, double z, float yaw, float pitch, int interpolationSteps, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         this.prevServerX = this.lerpX;
         this.prevServerY = this.lerpY;
         this.prevServerZ = this.lerpZ;
         this.positonHistory.add(new Aura.Position(this.lerpX, this.lerpY, this.lerpZ));
         this.positonHistory.removeIf(Aura.Position::shouldRemove);
      }
   }

   @Override
   public double getPrevServerX() {
      return this.prevServerX;
   }

   @Override
   public double getPrevServerY() {
      return this.prevServerY;
   }

   @Override
   public double getPrevServerZ() {
      return this.prevServerZ;
   }

   @Inject(method = "isFallFlying", at = @At("TAIL"), cancellable = true)
   public void isFallFlyingHook(CallbackInfoReturnable<Boolean> cir) {
   }

   @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
   public void travelHook(Vec3 movementInput, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if ((LivingEntity)(Object)this == Module.mc.player) {
            EventTravel event = new EventTravel(Module.mc.player.getDeltaMovement(), true);
            Vcore.EVENT_BUS.post(event);
            if (event.isCancelled()) {
               Module.mc.player.move(MoverType.SELF, event.getmVec());
               ci.cancel();
            }
         }
      }
   }

   @Inject(method = "travel", at = @At("RETURN"), cancellable = true)
   public void travelPostHook(Vec3 movementInput, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if ((LivingEntity)(Object)this == Module.mc.player) {
            EventTravel event = new EventTravel(movementInput, false);
            Vcore.EVENT_BUS.post(event);
            if (event.isCancelled()) {
               Module.mc.player.move(MoverType.SELF, Module.mc.player.getDeltaMovement());
               ci.cancel();
            }
         }
      }
   }

   @ModifyVariable(method = "setSprinting", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private boolean setSprintingHook(boolean sprinting) {
      return Module.mc.player == null
            || Module.mc.level == null
            || !ModuleManager.waterSpeed.isEnabled()
            || !ModuleManager.waterSpeed.mode.is(WaterSpeed.Mode.CancelResurface)
            || !Module.mc.player.isInWater()
               && !(Module.mc.level.getBlockState(BlockPos.containing(Module.mc.player.position().add(0.0, -0.5, 0.0))).getBlock() instanceof LiquidBlock)
         ? sprinting
         : true;
   }
}
