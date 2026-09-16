package vcore.injection.accesors;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Explosion.class)
public interface IExplosion {
   @Mutable
   @Accessor("x")
   void setX(double var1);

   @Mutable
   @Accessor("y")
   void setY(double var1);

   @Mutable
   @Accessor("z")
   void setZ(double var1);

   @Mutable
   @Accessor("source")
   void setEntity(Entity var1);

   @Mutable
   @Accessor("level")
   void setWorld(Level var1);

   @Mutable
   @Accessor("level")
   Level getWorld();

   @Mutable
   @Accessor("damageSource")
   DamageSource getDamageSource();
}
