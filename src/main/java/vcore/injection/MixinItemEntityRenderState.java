package vcore.injection;

import java.util.UUID;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import vcore.utility.interfaces.IItemEntityRenderState;

@Mixin(ItemEntityRenderState.class)
public class MixinItemEntityRenderState implements IItemEntityRenderState {
   @Unique
   private UUID vcore$uuid;
   @Unique
   private int vcore$id;
   @Unique
   private boolean vcore$onGround;
   @Unique
   private boolean vcore$inWater;
   @Unique
   private boolean vcore$inLava;
   @Unique
   private float vcore$tickDelta;

   @Override
   public UUID vcore$getUUID() {
      return this.vcore$uuid;
   }

   @Override
   public void vcore$setUUID(UUID uuid) {
      this.vcore$uuid = uuid;
   }

   @Override
   public int vcore$getId() {
      return this.vcore$id;
   }

   @Override
   public void vcore$setId(int id) {
      this.vcore$id = id;
   }

   @Override
   public boolean vcore$isOnGround() {
      return this.vcore$onGround;
   }

   @Override
   public void vcore$setOnGround(boolean onGround) {
      this.vcore$onGround = onGround;
   }

   @Override
   public boolean vcore$isInWater() {
      return this.vcore$inWater;
   }

   @Override
   public void vcore$setInWater(boolean inWater) {
      this.vcore$inWater = inWater;
   }

   @Override
   public boolean vcore$isInLava() {
      return this.vcore$inLava;
   }

   @Override
   public void vcore$setInLava(boolean inLava) {
      this.vcore$inLava = inLava;
   }

   @Override
   public float vcore$getTickDelta() {
      return this.vcore$tickDelta;
   }

   @Override
   public void vcore$setTickDelta(float tickDelta) {
      this.vcore$tickDelta = tickDelta;
   }
}
