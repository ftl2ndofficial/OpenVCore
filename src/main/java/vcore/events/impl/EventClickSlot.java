package vcore.events.impl;

import net.minecraft.world.inventory.ClickType;
import vcore.events.Event;

public class EventClickSlot extends Event {
   private final ClickType slotActionType;
   private final int slot;
   private final int button;
   private final int id;

   public EventClickSlot(ClickType slotActionType, int slot, int button, int id) {
      this.slot = slot;
      this.button = button;
      this.id = id;
      this.slotActionType = slotActionType;
   }

   public ClickType getSlotActionType() {
      return this.slotActionType;
   }

   public int getSlot() {
      return this.slot;
   }

   public int getButton() {
      return this.button;
   }

   public int getId() {
      return this.id;
   }
}
