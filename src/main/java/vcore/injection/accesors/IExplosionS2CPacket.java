package vcore.injection.accesors;

import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundExplodePacket.class)
public interface IExplosionS2CPacket {
   @Mutable
   @Accessor("knockbackX")
   void setMotionX(float var1);

   @Mutable
   @Accessor("knockbackY")
   void setMotionY(float var1);

   @Mutable
   @Accessor("knockbackZ")
   void setMotionZ(float var1);

   @Accessor("knockbackX")
   float getMotionX();

   @Accessor("knockbackY")
   float getMotionY();

   @Accessor("knockbackZ")
   float getMotionZ();
}
