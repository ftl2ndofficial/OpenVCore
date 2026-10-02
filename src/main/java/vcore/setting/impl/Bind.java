package vcore.setting.impl;

import vcore.utility.KeyboardUtility;

public class Bind {
   private final int key;
   private boolean hold;
   private final boolean mouse;

   public Bind(int key, boolean mouse, boolean hold) {
      this.key = key;
      this.mouse = mouse;
      this.hold = hold;
   }

   public int getKey() {
      return this.key;
   }

   public String getBind() {
      return KeyboardUtility.getBindName(this.key, this.mouse);
   }

   public boolean isHold() {
      return this.hold;
   }

   public boolean isMouse() {
      return this.mouse;
   }

   public void setHold(boolean hold) {
      this.hold = hold;
   }

   public boolean isBound() {
      if (this.mouse) {
         return this.key >= 0;
      }
      return this.key > 0;
   }

   public boolean isEmpty() {
      return !this.isBound();
   }
}
