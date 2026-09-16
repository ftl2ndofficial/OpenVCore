package vcore.injection.accesors;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface IInteractionManager {
   @Accessor("destroyProgress")
   float getCurBlockDamageMP();

   @Accessor("destroyProgress")
   void setCurBlockDamageMP(float var1);

   @Invoker("ensureHasSentCarriedItem")
   void syncSlot();
}
