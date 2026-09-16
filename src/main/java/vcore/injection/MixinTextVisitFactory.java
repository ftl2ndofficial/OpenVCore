package vcore.injection;

import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.misc.NameProtect;

@Mixin(StringDecomposer.class)
public class MixinTextVisitFactory {
   @ModifyArg(
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
         ordinal = 0
      ),
      method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
      index = 0
   )
   private static String adjustText(String text) {
      return !ModuleManager.nameProtect.isEnabled() ? text : NameProtect.protectText(text);
   }
}
