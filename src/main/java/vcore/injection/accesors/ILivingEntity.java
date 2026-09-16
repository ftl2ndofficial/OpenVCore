package vcore.injection.accesors;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface ILivingEntity {
   @Accessor("attackStrengthTicker")
   int getLastAttackedTicks();

   @Accessor("noJumpDelay")
   int getLastJumpCooldown();

   @Accessor("noJumpDelay")
   void setLastJumpCooldown(int var1);
}
