package vcore.utility.player;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public final class InputUtility {
   private InputUtility() {}

   public static float getForward(ClientInput input) {
      return input == null ? 0.0F : input.getMoveVector().y;
   }

   public static float getLeft(ClientInput input) {
      return input == null ? 0.0F : input.getMoveVector().x;
   }

   public static void setForward(ClientInput input, float forward) {
      if (input != null) {
         input.moveVector = new Vec2(input.moveVector.x, forward);
      }
   }

   public static void setLeft(ClientInput input, float left) {
      if (input != null) {
         input.moveVector = new Vec2(left, input.moveVector.y);
      }
   }

   public static void setMoveVector(ClientInput input, float left, float forward) {
      if (input != null) {
         input.moveVector = new Vec2(left, forward);
      }
   }

   public static boolean isJumping(ClientInput input) {
      return input != null && input.keyPresses.jump();
   }

   public static void setJumping(ClientInput input, boolean jumping) {
      if (input != null) {
         Input p = input.keyPresses;
         input.keyPresses = new Input(p.forward(), p.backward(), p.left(), p.right(), jumping, p.shift(), p.sprint());
      }
   }

   public static boolean isSneaking(ClientInput input) {
      return input != null && input.keyPresses.shift();
   }

   public static void setSneaking(ClientInput input, boolean shift) {
      if (input != null) {
         Input p = input.keyPresses;
         input.keyPresses = new Input(p.forward(), p.backward(), p.left(), p.right(), p.jump(), shift, p.sprint());
      }
   }

   public static void clearMovement(ClientInput input) {
      if (input != null) {
         input.keyPresses = Input.EMPTY;
         input.moveVector = Vec2.ZERO;
      }
   }
}
