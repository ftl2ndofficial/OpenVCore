package vcore.injection;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.Tooltips;

@Mixin(ItemContainerContents.class)
public class MixinShulkerBoxBlock {
   @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
   private void onAppendTooltip(TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components, CallbackInfo ci) {
      if (ModuleManager.tooltips != null) {
         if (Tooltips.storage.getValue()) {
            ci.cancel();
         }
      }
   }
}
