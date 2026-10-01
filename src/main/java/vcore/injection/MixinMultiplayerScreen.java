package vcore.injection;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.features.modules.misc.UnHook;
import vcore.utility.AccountUtility;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MixinMultiplayerScreen extends Screen {
   private static final int EDGE_PADDING = 5;
   private static final int RANDOM_ALT_BUTTON_Y = 8;
   private static final int RANDOM_ALT_BUTTON_WIDTH = 70;
   private static final int RANDOM_ALT_BUTTON_HEIGHT = 20;
   private static final int ACCOUNT_TEXT_X = 5;
   private static final int ACCOUNT_TEXT_Y = 14;
   private static final int ACCOUNT_TEXT_COLOR = -1;
   private Button randomAltButton;

   protected MixinMultiplayerScreen(Component title) {
      super(title);
   }

   @Inject(method = "init", at = @At("TAIL"))
   private void addRandomAltButton(CallbackInfo ci) {
      if (UnHook.isActive()) {
         this.randomAltButton = null;
      } else {
         this.randomAltButton = (Button)this.addRenderableWidget(
            Button.builder(Component.nullToEmpty("Random Alt"), button -> AccountUtility.loginRandomAlt()).bounds(this.width - 70 - 5, 8, 70, 20).build()
         );
      }
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      if (UnHook.isActive() && this.randomAltButton != null) {
         this.removeWidget(this.randomAltButton);
         this.randomAltButton = null;
      }
      super.render(context, mouseX, mouseY, delta);
      if (!UnHook.isActive() && this.minecraft != null) {
         context.drawString(this.font, "Account: " + this.minecraft.getUser().getName(), 5, 14, -1, false);
      }
   }
}
