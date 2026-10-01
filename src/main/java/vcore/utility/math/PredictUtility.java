package vcore.utility.math;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vcore.features.modules.Module;

public class PredictUtility {
   public static Player movePlayer(Player entity, Vec3 newPos) {
      return entity != null && newPos != null ? equipAndReturn(entity, newPos) : null;
   }

   public static Player predictPlayer(Player entity, int ticks) {
      Vec3 posVec = predictPosition(entity, ticks);
      return posVec == null ? null : equipAndReturn(entity, posVec);
   }

   public static Vec3 predictPosition(Player entity, int ticks) {
      if (entity == null) {
         return null;
      }

      Vec3 posVec = new Vec3(entity.getX(), entity.getY(), entity.getZ());
      double motionX = entity.getDeltaMovement().x();
      double motionZ = entity.getDeltaMovement().z();

      for (int i = 0; i < ticks; i++) {
         float hbDeltaX = motionX > 0.0 ? 0.3F : -0.3F;
         float hbDeltaZ = motionZ > 0.0 ? 0.3F : -0.3F;
         if (!Module.mc.level.isEmptyBlock(BlockPos.containing(posVec.add(motionX + hbDeltaX, 0.1, motionZ + hbDeltaZ)))
            || !Module.mc.level.isEmptyBlock(BlockPos.containing(posVec.add(motionX + hbDeltaX, 1.0, motionZ + hbDeltaZ)))) {
            motionX = 0.0;
            motionZ = 0.0;
         }

         posVec = posVec.add(motionX, 0.0, motionZ);
      }

      return posVec;
   }

   public static AABB predictBox(Player entity, int ticks) {
      Vec3 posVec = predictPosition(entity, ticks);
      return posVec == null ? null : createBox(posVec, entity);
   }

   public static Player equipAndReturn(Player original, Vec3 posVec) {
      Player copyEntity = new Player(
         Module.mc.level,
         new GameProfile(UUID.fromString("66123666-1234-5432-6666-667563866600"), "PredictEntity339")
      ) {
         @Override
         public GameType gameMode() {
            return GameType.SURVIVAL;
         }
      };
      copyEntity.setYRot(original.getYRot());
      copyEntity.setPos(posVec);
      copyEntity.setHealth(original.getHealth());
      copyEntity.xo = original.xo;
      copyEntity.zo = original.zo;
      copyEntity.yo = original.yo;
      copyEntity.getInventory().replaceWith(original.getInventory());

      for (MobEffectInstance se : original.getActiveEffects()) {
         copyEntity.addEffect(se);
      }

      return copyEntity;
   }

   public static AABB createBox(Vec3 vec, Entity entity) {
      return entity.getBoundingBox().move(entity.position().vectorTo(vec));
   }
}
