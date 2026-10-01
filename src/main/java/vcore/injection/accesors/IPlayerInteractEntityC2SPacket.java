package vcore.injection.accesors;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerboundInteractPacket.class)
public interface IPlayerInteractEntityC2SPacket {
   @Accessor("entityId")
   int getEntityId();

   @Invoker("write")
   void write(FriendlyByteBuf var1);
}
