package vcore.events.impl;

import net.minecraft.network.chat.ClickEvent;

public class ClientClickEvent extends ClickEvent {
   public ClientClickEvent(Action action, String value) {
      super(action, value);
   }
}
