package vcore.injection.accesors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface IEntity {
   @Mutable
   @Accessor("position")
   void setPos(Vec3 var1);

   @Mutable
   @Accessor("blockPosition")
   void setBlockPos(BlockPos var1);
}
