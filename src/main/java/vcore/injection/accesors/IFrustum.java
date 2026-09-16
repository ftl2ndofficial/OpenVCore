package vcore.injection.accesors;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Frustum.class)
public interface IFrustum {
   @Accessor
   FrustumIntersection getIntersection();

   @Accessor("camX")
   double getX();

   @Accessor("camX")
   void setX(double var1);

   @Accessor("camY")
   double getY();

   @Accessor("camY")
   void setY(double var1);

   @Accessor("camZ")
   double getZ();

   @Accessor("camZ")
   void setZ(double var1);
}
