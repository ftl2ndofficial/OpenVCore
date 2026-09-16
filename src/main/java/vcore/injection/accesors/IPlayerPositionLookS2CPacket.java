package vcore.injection.accesors;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundPlayerPositionPacket.class)
public interface IPlayerPositionLookS2CPacket {
   @Mutable
   @Accessor("yRot")
   void setYaw(float var1);

   @Mutable
   @Accessor("xRot")
   void setPitch(float var1);
}
