package vcore.events.impl;

import net.minecraft.world.entity.Entity;
import vcore.events.Event;

public class EventEntityRemoved extends Event {
   public Entity entity;

   public EventEntityRemoved(Entity entity) {
      this.entity = entity;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
