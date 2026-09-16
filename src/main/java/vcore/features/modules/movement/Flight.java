package vcore.features.modules.movement;

import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.player.LocalPlayer;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.player.MovementUtility;

public class Flight extends Module {
   private final Setting<Flight.Mode> mode = new Setting<>("Mode", Flight.Mode.Vanilla);
   private final Setting<Float> hSpeed = new Setting<>("Horizontal", 1.0F, 0.0F, 10.0F, v -> this.mode.is(Flight.Mode.Vanilla));
   private final Setting<Float> vSpeed = new Setting<>("Vertical", 0.78F, 0.0F, 5.0F, v -> this.mode.is(Flight.Mode.Vanilla));
   private final Setting<Boolean> antiKick = new Setting<>("AntiKick", false, v -> this.mode.is(Flight.Mode.Vanilla));

   public Flight() {
      super("Flight", "Creative-like flight.", Module.Category.MOVEMENT);
   }

   @Override
   public void onUpdate() {
      if (!fullNullCheck()) {
         LocalPlayer player = mc.player;
         if (player != null && mc.options != null) {
            switch ((Flight.Mode)this.mode.getValue()) {
               case Shulker:
                  if (mc.screen instanceof ShulkerBoxScreen) {
                     player.push(0.0, 0.9F, 0.0);
                  }
                  break;
               case Vanilla:
                  if (MovementUtility.isMoving()) {
                     double[] dir = MovementUtility.forward(this.hSpeed.getValue().floatValue());
                     player.setDeltaMovement(dir[0], 0.0, dir[1]);
                  } else {
                     player.setDeltaMovement(0.0, 0.0, 0.0);
                  }

                  if (mc.options.keyJump.isDown()) {
                     player.setDeltaMovement(player.getDeltaMovement().add(0.0, this.vSpeed.getValue().floatValue(), 0.0));
                  }

                  if (mc.options.keyShift.isDown()) {
                     player.setDeltaMovement(player.getDeltaMovement().add(0.0, -this.vSpeed.getValue(), 0.0));
                  }

                  if (this.antiKick.getValue()) {
                     player.setDeltaMovement(player.getDeltaMovement().add(0.0, -0.08, 0.0));
                  }
            }
         }
      }
   }

   @Override
   public void onDisable() {
      if (!fullNullCheck()) {
         LocalPlayer player = mc.player;
         if (player != null) {
            player.getAbilities().flying = false;
            player.getAbilities().setFlyingSpeed(0.05F);
         }
      }
   }

   private enum Mode {
      Shulker,
      Vanilla;
   }
}
