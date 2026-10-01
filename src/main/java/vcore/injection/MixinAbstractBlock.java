package vcore.injection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.IManager;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.player.AutoTool;

@Mixin(BlockBehaviour.class)
public abstract class MixinAbstractBlock {
   @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
   public void calcBlockBreakingDeltaHook(BlockState state, Player player, BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> ci) {
      if (ModuleManager.autoTool.isEnabled() && AutoTool.silent.getValue()) {
         float f = state.getDestroySpeed(world, pos);
         if (f < 0.0F) {
            ci.setReturnValue(0.0F);
         } else {
            float dig_speed = this.getDigSpeed(state, player.getInventory().getItem(AutoTool.itemIndex)) / f;
            ci.setReturnValue(player.getInventory().getItem(AutoTool.itemIndex).isCorrectToolForDrops(state) ? dig_speed / 30.0F : dig_speed / 100.0F);
         }
      }
   }

   public float getDigSpeed(BlockState state, ItemStack stack) {
      double str = stack.getDestroySpeed(state);
      int effect = 0;
      if (IManager.mc.level != null) {
         effect = IManager.mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .get(Enchantments.EFFICIENCY)
            .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
            .orElse(0);
      }
      return (float)Math.max(str + (str > 1.0 ? effect * effect + 1.0 : 0.0), 0.0);
   }
}
