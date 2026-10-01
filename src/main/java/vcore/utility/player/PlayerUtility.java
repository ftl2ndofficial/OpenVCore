package vcore.utility.player;

import java.util.Objects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import vcore.features.modules.Module;
import vcore.utility.world.ExplosionUtility;

public final class PlayerUtility {
   public static boolean isInHell() {
      return Module.mc.level != null && Module.mc.level.dimension() == Level.NETHER;
   }

   public static boolean isInEnd() {
      return Module.mc.level != null && Module.mc.level.dimension() == Level.END;
   }

   public static boolean isInOver() {
      return Module.mc.level != null && Module.mc.level.dimension() == Level.OVERWORLD;
   }

   public static boolean isEating() {
      return Module.mc.player == null
         ? false
         : (
               Module.mc.player.getMainHandItem().getComponents().has(DataComponents.FOOD)
                  || Module.mc.player.getOffhandItem().getComponents().has(DataComponents.FOOD)
            )
            && Module.mc.player.isUsingItem();
   }

   public static boolean isMining() {
      return Module.mc.gameMode == null ? false : Module.mc.gameMode.isDestroying();
   }

   public static float squaredDistanceFromEyes(@NotNull Vec3 targetPos) {
      if (Module.mc.player == null) {
         return 0.0F;
      }

      double dx = targetPos.x - Module.mc.player.getX();
      double dy = targetPos.y - (Module.mc.player.getY() + Module.mc.player.getEyeHeight(Module.mc.player.getPose()));
      double dz = targetPos.z - Module.mc.player.getZ();
      return (float)(dx * dx + dy * dy + dz * dz);
   }

   public static float squaredDistance2d(@NotNull Vec2 point) {
      if (Module.mc.player == null) {
         return 0.0F;
      }

      double d = Module.mc.player.getX() - point.x;
      double f = Module.mc.player.getZ() - point.y;
      return (float)(d * d + f * f);
   }

   public static LocalPlayer getPlayer() {
      return Module.mc.player;
   }

   public static float calculatePercentage(@NotNull ItemStack stack) {
      float durability = stack.getMaxDamage() - stack.getDamageValue();
      return durability / stack.getMaxDamage() * 100.0F;
   }

   public static float fixAngle(float angle) {
      return Math.round(angle / (float)(getGCD() * 0.15)) * (float)(getGCD() * 0.15);
   }

   public static float getGCD() {
      double sensitivity = (Double)Module.mc.options.sensitivity().get();
      double value = sensitivity * 0.6 + 0.2;
      double result = Math.pow(value, 3.0) * 8.0;
      return (float)result;
   }

   public static float squaredDistance2d(double x, double z) {
      if (Module.mc.player == null) {
         return 0.0F;
      }

      double d = Module.mc.player.getX() - x;
      double f = Module.mc.player.getZ() - z;
      return (float)(d * d + f * f);
   }

   public static float getSquaredDistance2D(Vec3 vec) {
      double d0 = Module.mc.player.getX() - vec.x();
      double d2 = Module.mc.player.getZ() - vec.z();
      return (float)(d0 * d0 + d2 * d2);
   }

   public static boolean canSee(Vec3 pos) {
      Vec3 vec3d = new Vec3(Module.mc.player.getX(), Module.mc.player.getEyeY(), Module.mc.player.getZ());
      return pos.distanceTo(vec3d) > 128.0 ? false : ExplosionUtility.raycast(vec3d, pos, false) == Type.MISS;
   }

   public static boolean isFalling() {
      return Module.mc.player == null ? false : !Module.mc.player.onGround() && !Module.mc.player.isCreative() && Module.mc.player.getDeltaMovement().y < 0.0;
   }
}
