package vcore.events.impl;

import net.minecraft.client.player.ClientInput;
import vcore.events.Event;
import vcore.utility.player.InputUtility;

public class EventKeyboardInput extends Event {
   private final ClientInput input;
   private boolean clearMovementInput;

   public EventKeyboardInput(ClientInput input) {
      this.input = input;
   }

   public ClientInput getInput() {
      return this.input;
   }

   public void clearMovementInput() {
      this.clearMovementInput = true;
   }

   public boolean shouldClearMovementInput() {
      return this.clearMovementInput;
   }

   public static void clearMovementInput(ClientInput input) {
      InputUtility.clearMovement(input);
   }
}
