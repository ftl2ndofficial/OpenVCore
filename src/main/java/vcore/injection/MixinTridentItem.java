package vcore.injection;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.UseTridentEvent;

@Mixin(TridentItem.class)
public abstract class MixinTridentItem {
   @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
   public void onStoppedUsingHook(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
      if (user == Vcore.mc.player && EnchantmentHelper.getTridentSpinAttackStrength(stack, Vcore.mc.player) > 0.0F) {
         UseTridentEvent e = new UseTridentEvent();
         Vcore.EVENT_BUS.post(e);
         if (e.isCancelled()) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "use", at = @At("HEAD"), cancellable = true)
   public void useHook(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      ItemStack itemStack = user.getItemInHand(hand);
      if (EnchantmentHelper.getTridentSpinAttackStrength(itemStack, user) > 0.0F
         && !user.isInWaterOrRain()
         && ModuleManager.tridentBoost.isEnabled()
         && ModuleManager.tridentBoost.anyWeather.getValue()) {
         user.startUsingItem(hand);
         cir.setReturnValue(InteractionResult.CONSUME);
      }
   }
}
