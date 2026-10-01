package vcore.injection;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.features.modules.Module;

@Mixin(EffectsInInventory.class)
public abstract class MixinAbstractInventoryScreen {
   @Unique
   private static final float EFFECTS_ANIM_DURATION = 250.0F;
   @Unique
   private long effectsOpenTime;
   @Unique
   private boolean statusEffectsAnimPushed;

   @Inject(method = "render", at = @At("HEAD"))
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
            context.pose().pushMatrix();
            context.pose().translate(centerX, centerY);
            context.pose().scale(anim, anim);
            context.pose().translate(-centerX, -centerY);
            this.statusEffectsAnimPushed = true;
         } else {
            this.statusEffectsAnimPushed = false;
         }
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void onDrawStatusEffectsTail(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
      if (this.statusEffectsAnimPushed) {
         context.pose().popMatrix();
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
