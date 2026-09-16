package vcore.injection;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.Tooltips;

@Mixin(ShulkerBoxBlock.class)
public class MixinShulkerBoxBlock {
   @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
   private void onAppendTooltip(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag options, CallbackInfo ci) {
      if (ModuleManager.tooltips != null) {
         if (Tooltips.storage.getValue()) {
            ci.cancel();
         }
      }
   }
}
