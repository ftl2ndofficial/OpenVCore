package vcore.injection;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.events.impl.EventKeyPress;
import vcore.events.impl.EventKeyRelease;
import vcore.features.modules.Module;
import vcore.gui.clickui.ClickGUI;

@Mixin(KeyboardHandler.class)
public class MixinKeyboard {
   @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
   private void onKey(long windowPointer, int action, KeyEvent event, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         boolean whitelist = Module.mc.screen == null || Module.mc.screen instanceof ClickGUI;
         if (whitelist) {
            int key = event.key();
            int scanCode = event.scancode();
            if (action == 0) {
               Managers.MODULE.onKeyReleased(key);
            }

            if (action == 1) {
               Managers.MODULE.onKeyPressed(key);
            }

            if (action == 2) {
               action = 1;
            }

            switch (action) {
               case 0:
                  EventKeyRelease eventx = new EventKeyRelease(key, scanCode);
                  Vcore.EVENT_BUS.post(eventx);
                  if (eventx.isCancelled()) {
                     ci.cancel();
                  }
                  break;
               case 1:
                  EventKeyPress eventk = new EventKeyPress(key, scanCode);
                  Vcore.EVENT_BUS.post(eventk);
                  if (eventk.isCancelled()) {
                     ci.cancel();
                  }
            }
         }
      }
   }
}
