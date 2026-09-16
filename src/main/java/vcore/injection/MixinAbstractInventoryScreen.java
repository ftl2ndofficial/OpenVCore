package vcore.injection;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.features.modules.Module;

@Mixin(EffectRenderingInventoryScreen.class)
public abstract class MixinAbstractInventoryScreen<T extends AbstractContainerMenu> {
   @Unique
   private static final float EFFECTS_ANIM_DURATION = 250.0F;
   @Unique
   private long effectsOpenTime;
   @Unique
   private boolean statusEffectsAnimPushed;

   @Inject(method = "renderEffects", at = @At("HEAD"))
   private void onDrawStatusEffectsHead(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
      if (!this.isSurvivalMode()) {
         this.statusEffectsAnimPushed = false;
      } else {
         if (this.effectsOpenTime == 0L) {
            this.effectsOpenTime = System.currentTimeMillis();
         }

         float progress = this.getProgress();
         if (progress < 1.0F) {
            float anim = this.getAnim(progress);
            int centerX = context.guiWidth() / 2;
            int centerY = context.guiHeight() / 2;
            context.pose().pushPose();
            context.pose().translate(centerX, centerY, 0.0F);
            context.pose().scale(anim, anim, 1.0F);
            context.pose().translate(-centerX, -centerY, 0.0F);
            this.statusEffectsAnimPushed = true;
         } else {
            this.statusEffectsAnimPushed = false;
         }
      }
   }

   @Inject(method = "renderEffects", at = @At("TAIL"))
   private void onDrawStatusEffectsTail(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
      if (this.statusEffectsAnimPushed) {
         context.pose().popPose();
         this.statusEffectsAnimPushed = false;
      }
   }

   @Unique
   private float getProgress() {
      return Math.min(1.0F, (float)(System.currentTimeMillis() - this.effectsOpenTime) / 250.0F);
   }

   @Unique
   private float getAnim(float progress) {
      float x = progress - 1.0F;
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow(x, 3.0) + c1 * (float)Math.pow(x, 2.0);
   }

   @Unique
   private boolean isSurvivalMode() {
      return Module.mc.gameMode != null && Module.mc.gameMode.getPlayerMode() == GameType.SURVIVAL;
   }
}
