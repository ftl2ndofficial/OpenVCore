package vcore.events.impl;

import net.minecraft.network.chat.ClickEvent;

public record ClientClickEvent(Action action, String value) implements ClickEvent {
   @Override
   public Action action() {
      return this.action;
   }
}
