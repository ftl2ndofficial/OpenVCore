package vcore.utility.interfaces;

import java.util.UUID;

public interface IItemEntityRenderState {
   UUID vcore$getUUID();

   void vcore$setUUID(UUID uuid);

   int vcore$getId();

   void vcore$setId(int id);

   boolean vcore$isOnGround();

   void vcore$setOnGround(boolean onGround);

   boolean vcore$isInWater();

   void vcore$setInWater(boolean inWater);

   boolean vcore$isInLava();

   void vcore$setInLava(boolean inLava);

   float vcore$getTickDelta();

   void vcore$setTickDelta(float tickDelta);
}
