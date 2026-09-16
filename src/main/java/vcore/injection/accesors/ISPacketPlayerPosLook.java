package vcore.injection.accesors;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundPlayerPositionPacket.class)
public interface ISPacketPlayerPosLook {
   @Accessor("yRot")
   void setYaw(float var1);

   @Accessor("xRot")
   void setPitch(float var1);
}
