package vcore.events.impl;

import net.minecraft.world.entity.player.Player;
import vcore.events.Event;

public class EventDeath extends Event {
   private final Player player;

   public EventDeath(Player player) {
      this.player = player;
   }

   public Player getPlayer() {
      return this.player;
   }
}
