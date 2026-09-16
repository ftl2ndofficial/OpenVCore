package vcore.injection.accesors;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface IClientPlayerEntity {
   @Invoker("sendPosition")
   void iSendMovementPackets();

   @Accessor("yRotLast")
   float getLastYaw();

   @Accessor("xRotLast")
   float getLastPitch();

   @Accessor("yRotLast")
   void setLastYaw(float var1);

   @Accessor("xRotLast")
   void setLastPitch(float var1);

   @Accessor("jumpRidingScale")
   void setMountJumpStrength(float var1);
}
