package vcore.events.impl;

import net.minecraft.world.level.chunk.LevelChunk;
import vcore.events.Event;

public class EventChunkData extends Event {
   private final LevelChunk chunk;

   public EventChunkData(LevelChunk chunk) {
      this.chunk = chunk;
   }

   public LevelChunk getChunk() {
      return this.chunk;
   }
}
