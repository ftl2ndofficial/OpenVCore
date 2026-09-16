package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.gui.font.FontRenderers;

@Mixin(GuiGraphics.class)
public class MixinDrawContext {
   @Shadow
   @Final
   private PoseStack pose;

   public void drawTextHook(Font textRenderer, FormattedCharSequence text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
      MutableComponent text1 = Component.empty();
      text.accept((i, style, codePoint) -> {
         text1.append(Component.literal(new String(Character.toChars(codePoint))).setStyle(style));
         return true;
      });
      FontRenderers.sf_medium.drawString(this.pose, text1.getString(), x, y, color);
      cir.setReturnValue((int)FontRenderers.sf_medium.getStringWidth(text.toString()));
   }
}
