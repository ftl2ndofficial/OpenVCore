package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.LocalPlayer;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventKeyPress;
import vcore.events.impl.EventKeyRelease;
import vcore.events.impl.EventMouse;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.movement.freelook.CameraOverriddenEntity;
import vcore.features.modules.movement.freelook.FreeLookState;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;

public class FreeLook extends Module {
   private final Setting<Bind> bind = new Setting<>("Bind", new Bind(-1, false, false));
   private final Setting<Boolean> toggle = new Setting<>("Toggle", true);
   private boolean active;
   private CameraType previousPerspective;

   public FreeLook() {
      super("FreeLook", "Free look camera.", Module.Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      this.setActive(false);
   }

   @EventHandler
   public void onKeyPress(EventKeyPress event) {
      if (!fullNullCheck()) {
         if (this.isKeyMatch(event.getKey())) {
            if (!this.isAuraBlocking()) {
               if (this.toggle.getValue()) {
                  this.setActive(!this.active);
               } else {
                  this.setActive(true);
               }
            }
         }
      }
   }

   @EventHandler
   public void onKeyRelease(EventKeyRelease event) {
      if (!fullNullCheck()) {
         if (!this.toggle.getValue()) {
            if (this.isKeyMatch(event.getKey())) {
               this.setActive(false);
            }
         }
      }
   }

   @EventHandler
   public void onMouse(EventMouse event) {
      if (!fullNullCheck()) {
         if (this.isMouseMatch(event.getButton())) {
            if (event.getAction() != 1 || !this.isAuraBlocking()) {
               if (event.getAction() == 1) {
                  if (this.toggle.getValue()) {
                     this.setActive(!this.active);
                  } else {
                     this.setActive(true);
                  }
               } else if (event.getAction() == 0 && !this.toggle.getValue()) {
                  this.setActive(false);
               }
            }
         }
      }
   }

   private boolean isKeyMatch(int key) {
      Bind b = this.bind.getValue();
      return b != null && !b.isMouse() && b.getKey() == key && key != -1;
   }

   private boolean isMouseMatch(int button) {
      Bind b = this.bind.getValue();
      return b != null && b.isMouse() && b.getKey() == button && button != -1;
   }

   private void setActive(boolean value) {
      if (this.active != value) {
         this.active = value;
         FreeLookState.setManualActive(value);
         if (mc.options != null) {
            if (this.active) {
               if (mc.player instanceof LocalPlayer && mc.player instanceof CameraOverriddenEntity camera) {
                  camera.setCameraYaw(mc.player.getYRot());
                  camera.setCameraPitch(mc.player.getXRot());
               }

               this.previousPerspective = mc.options.getCameraType();
               if (this.previousPerspective != CameraType.THIRD_PERSON_FRONT) {
                  mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
               }
            } else {
               mc.options.setCameraType(this.previousPerspective != null ? this.previousPerspective : CameraType.FIRST_PERSON);
               this.previousPerspective = null;
            }
         }
      }
   }

   private boolean isAuraBlocking() {
      if (ModuleManager.aura == null) {
         return false;
      } else if (ModuleManager.aura.isEnabled() && Aura.target != null) {
         this.sendMessage("Cannot be used with Aura.");
         return true;
      } else {
         return false;
      }
   }
}
