package vcore.events.impl;

import net.minecraft.client.player.Input;
import vcore.events.Event;

public class EventKeyboardInput extends Event {
   private final Input input;
   private boolean clearMovementInput;

   public EventKeyboardInput(Input input) {
      this.input = input;
   }

   public Input getInput() {
      return this.input;
   }

   public void clearMovementInput() {
      this.clearMovementInput = true;
   }

   public boolean shouldClearMovementInput() {
      return this.clearMovementInput;
   }

   public static void clearMovementInput(Input input) {
      input.up = false;
      input.down = false;
      input.left = false;
      input.right = false;
      input.forwardImpulse = 0.0F;
      input.leftImpulse = 0.0F;
      input.jumping = false;
      input.shiftKeyDown = false;
   }
}
