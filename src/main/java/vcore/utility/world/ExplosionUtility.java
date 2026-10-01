package vcore.utility.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;
import vcore.features.modules.Module;
import vcore.utility.math.PredictUtility;

public final class ExplosionUtility {
   public static boolean terrainIgnore = false;
   public static DummyExplosion explosion = new DummyExplosion();
   private static final boolean IGNORE_TERRAIN = false;
   private static final boolean ASSUME_BEST_ARMOR = false;

   public static class DummyExplosion implements Explosion {
      public Vec3 center = Vec3.ZERO;

      @Override
      public net.minecraft.server.level.ServerLevel level() {
         return null;
      }

      @Override
      public Explosion.BlockInteraction getBlockInteraction() {
         return Explosion.BlockInteraction.DESTROY;
      }

      @Override
      public @Nullable LivingEntity getIndirectSourceEntity() {
         return Module.mc.player;
      }

      @Override
      public @Nullable Entity getDirectSourceEntity() {
         return Module.mc.player;
      }

      @Override
      public float radius() {
         return 6.0F;
      }

      @Override
      public Vec3 center() {
         return center;
      }

      @Override
      public boolean canTriggerBlocks() {
         return false;
      }

      @Override
      public boolean shouldAffectBlocklikeEntities() {
         return true;
      }
   }

   public static List<ItemStack> getArmorItems(LivingEntity entity) {
      List<ItemStack> list = new ArrayList<>();
      if (entity != null) {
         for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            list.add(entity.getItemBySlot(slot));
         }
      }
      return list;
   }

   public static float getAutoCrystalDamage(Vec3 crystalPos, Player target, int predictTicks, boolean optimized) {
      return predictTicks == 0
         ? getExplosionDamage(crystalPos, target, optimized)
         : getExplosionDamageWPredict(crystalPos, target, PredictUtility.predictBox(target, predictTicks), optimized);
   }

   public static float getSelfExplosionDamage(Vec3 explosionPos, int predictTicks, boolean optimized) {
      return getAutoCrystalDamage(explosionPos, Module.mc.player, predictTicks, optimized);
   }

   public static float getExplosionDamage(Vec3 explosionPos, Player target, boolean optimized) {
      if (Module.mc.level.getDifficulty() != Difficulty.PEACEFUL && target != null) {
         explosion.center = explosionPos;

         if (!new AABB(
               Mth.floor(explosionPos.x - 11.0),
               Mth.floor(explosionPos.y - 11.0),
               Mth.floor(explosionPos.z - 11.0),
               Mth.floor(explosionPos.x + 13.0),
               Mth.floor(explosionPos.y + 13.0),
               Mth.floor(explosionPos.z + 13.0)
            )
            .intersects(target.getBoundingBox())) {
            return 0.0F;
         }

         if (!target.ignoreExplosion(explosion) && !target.isInvulnerable()) {
            double distExposure = (float)target.distanceToSqr(explosionPos) / 144.0;
            if (distExposure <= 1.0) {
               terrainIgnore = false;
               double exposure = getExposure(explosionPos, target.getBoundingBox(), optimized);
               terrainIgnore = false;
               double finalExposure = (1.0 - distExposure) * exposure;
               float toDamage = (float)Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * 12.0 + 1.0);
               if (Module.mc.level.getDifficulty() == Difficulty.EASY) {
                  toDamage = Math.min(toDamage / 2.0F + 1.0F, toDamage);
               } else if (Module.mc.level.getDifficulty() == Difficulty.HARD) {
                  toDamage = toDamage * 3.0F / 2.0F;
               }

               toDamage = CombatRules.getDamageAfterAbsorb(
                  target,
                  toDamage,
                  Module.mc.level.damageSources().explosion(Module.mc.player, Module.mc.player),
                  target.getArmorValue(),
                  (float)target.getAttribute(Attributes.ARMOR_TOUGHNESS).getValue()
               );
               if (target.hasEffect(MobEffects.RESISTANCE)) {
                  int resistance = 25 - (target.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
                  float resistance_1 = toDamage * resistance;
                  toDamage = Math.max(resistance_1 / 25.0F, 0.0F);
               }

               if (toDamage <= 0.0F) {
                  toDamage = 0.0F;
               } else {
                  float protAmount = getProtectionAmount(getArmorItems(target));
                  if (protAmount > 0.0F) {
                     toDamage = CombatRules.getDamageAfterMagicAbsorb(toDamage, protAmount);
                  }
               }

               return toDamage;
            }
         }

         return 0.0F;
      } else {
         return 0.0F;
      }
   }

   public static float getExplosionDamageWPredict(Vec3 explosionPos, Player target, AABB predict, boolean optimized) {
      if (Module.mc.level.getDifficulty() == Difficulty.PEACEFUL) {
         return 0.0F;
      }

      if (target != null && predict != null) {
         explosion.center = explosionPos;

         if (!new AABB(
               Mth.floor(explosionPos.x - 11.0),
               Mth.floor(explosionPos.y - 11.0),
               Mth.floor(explosionPos.z - 11.0),
               Mth.floor(explosionPos.x + 13.0),
               Mth.floor(explosionPos.y + 13.0),
               Mth.floor(explosionPos.z + 13.0)
            )
            .intersects(predict)) {
            return 0.0F;
         }

         if (!target.ignoreExplosion(explosion) && !target.isInvulnerable()) {
            double distExposure = predict.getCenter().add(0.0, -0.9, 0.0).distanceToSqr(explosionPos) / 144.0;
            if (distExposure <= 1.0) {
               terrainIgnore = false;
               double exposure = getExposure(explosionPos, predict, optimized);
               terrainIgnore = false;
               double finalExposure = (1.0 - distExposure) * exposure;
               float toDamage = (float)Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * 12.0 + 1.0);
               if (Module.mc.level.getDifficulty() == Difficulty.EASY) {
                  toDamage = Math.min(toDamage / 2.0F + 1.0F, toDamage);
               } else if (Module.mc.level.getDifficulty() == Difficulty.HARD) {
                  toDamage = toDamage * 3.0F / 2.0F;
               }

               toDamage = CombatRules.getDamageAfterAbsorb(
                  target,
                  toDamage,
                  Module.mc.level.damageSources().explosion(Module.mc.player, Module.mc.player),
                  target.getArmorValue(),
                  (float)Objects.requireNonNull(target.getAttribute(Attributes.ARMOR_TOUGHNESS)).getValue()
               );
               if (target.hasEffect(MobEffects.RESISTANCE)) {
                  int resistance = 25 - (Objects.requireNonNull(target.getEffect(MobEffects.RESISTANCE)).getAmplifier() + 1) * 5;
                  float resistance_1 = toDamage * resistance;
                  toDamage = Math.max(resistance_1 / 25.0F, 0.0F);
               }

               if (toDamage <= 0.0F) {
                  toDamage = 0.0F;
               } else {
                  float protAmount = getProtectionAmount(getArmorItems(target));
                  if (protAmount > 0.0F) {
                     toDamage = CombatRules.getDamageAfterMagicAbsorb(toDamage, protAmount);
                  }
               }

               return toDamage;
            }
         }

         return 0.0F;
      } else {
         return 0.0F;
      }
   }

   public static BlockHitResult rayCastBlock(ClipContext context, BlockPos block) {
      return (BlockHitResult)BlockGetter.traverseBlocks(
         context.getFrom(),
         context.getTo(),
         context,
         (raycastContext, blockPos) -> {
            BlockState blockState;
            if (!blockPos.equals(block)) {
               blockState = Blocks.AIR.defaultBlockState();
            } else {
               blockState = Blocks.OBSIDIAN.defaultBlockState();
            }

            Vec3 vec3d = raycastContext.getFrom();
            Vec3 vec3d2 = raycastContext.getTo();
            VoxelShape voxelShape = raycastContext.getBlockShape(blockState, Module.mc.level, blockPos);
            BlockHitResult blockHitResult = Module.mc.level.clipWithInteractionOverride(vec3d, vec3d2, blockPos, voxelShape, blockState);
            VoxelShape voxelShape2 = Shapes.empty();
            BlockHitResult blockHitResult2 = voxelShape2.clip(vec3d, vec3d2, blockPos);
            double d = blockHitResult == null ? Double.MAX_VALUE : raycastContext.getFrom().distanceToSqr(blockHitResult.getLocation());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : raycastContext.getFrom().distanceToSqr(blockHitResult2.getLocation());
            return d <= e ? blockHitResult : blockHitResult2;
         },
         raycastContext -> {
            Vec3 vec3d = raycastContext.getFrom().subtract(raycastContext.getTo());
            return BlockHitResult.miss(
               raycastContext.getTo(), Direction.getApproximateNearest(vec3d.x, vec3d.y, vec3d.z), BlockPos.containing(raycastContext.getTo())
            );
         }
      );
   }

   public static float getDamageOfGhostBlock(Vec3 explosionPos, Player target, BlockPos bp) {
      if (Module.mc.level.getDifficulty() == Difficulty.PEACEFUL) {
         return 0.0F;
      }

      explosion.center = explosionPos;

      double maxDist = 12.0;
      if (!new AABB(
            Mth.floor(explosionPos.x - maxDist - 1.0),
            Mth.floor(explosionPos.y - maxDist - 1.0),
            Mth.floor(explosionPos.z - maxDist - 1.0),
            Mth.floor(explosionPos.x + maxDist + 1.0),
            Mth.floor(explosionPos.y + maxDist + 1.0),
            Mth.floor(explosionPos.z + maxDist + 1.0)
         )
         .intersects(target.getBoundingBox())) {
         return 0.0F;
      }

      if (!target.ignoreExplosion(explosion) && !target.isInvulnerable()) {
         double distExposure = target.distanceToSqr(explosionPos) / 144.0;
         if (distExposure <= 1.0) {
            terrainIgnore = false;
            double exposure = getExposureGhost(explosionPos, target, bp);
            terrainIgnore = false;
            double finalExposure = (1.0 - distExposure) * exposure;
            float toDamage = (float)Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * maxDist + 1.0);
            if (Module.mc.level.getDifficulty() == Difficulty.EASY) {
               toDamage = Math.min(toDamage / 2.0F + 1.0F, toDamage);
            } else if (Module.mc.level.getDifficulty() == Difficulty.HARD) {
               toDamage = toDamage * 3.0F / 2.0F;
            }

            toDamage = CombatRules.getDamageAfterAbsorb(
               target,
               toDamage,
               Module.mc.level.damageSources().explosion(Module.mc.player, Module.mc.player),
               target.getArmorValue(),
               (float)target.getAttribute(Attributes.ARMOR_TOUGHNESS).getValue()
            );
            if (target.hasEffect(MobEffects.RESISTANCE)) {
               int resistance = 25 - (target.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
               float resistance_1 = toDamage * resistance;
               toDamage = Math.max(resistance_1 / 25.0F, 0.0F);
            }

            if (toDamage <= 0.0F) {
               toDamage = 0.0F;
            } else {
               float protAmount = getProtectionAmount(getArmorItems(target));
               if (protAmount > 0.0F) {
                  toDamage = CombatRules.getDamageAfterMagicAbsorb(toDamage, protAmount);
               }
            }

            return toDamage;
         }
      }

      return 0.0F;
   }

   private static float getExposureGhost(Vec3 source, Entity entity, BlockPos pos) {
      AABB box = entity.getBoundingBox();
      double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
      double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
      double f = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
      double g = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
      double h = (1.0 - Math.floor(1.0 / f) * f) / 2.0;
      if (!(d < 0.0) && !(e < 0.0) && !(f < 0.0)) {
         int i = 0;
         int j = 0;

         for (double k = 0.0; k <= 1.0; k += d) {
            for (double l = 0.0; l <= 1.0; l += e) {
               for (double m = 0.0; m <= 1.0; m += f) {
                  double n = Mth.lerp(k, box.minX, box.maxX);
                  double o = Mth.lerp(l, box.minY, box.maxY);
                  double p = Mth.lerp(m, box.minZ, box.maxZ);
                  Vec3 vec3d = new Vec3(n + g, o, p + h);
                  if (raycastGhost(new ClipContext(vec3d, source, Block.COLLIDER, Fluid.NONE, entity), pos).getType() == Type.MISS) {
                     i++;
                  }

                  j++;
               }
            }
         }

         return (float)i / j;
      } else {
         return 0.0F;
      }
   }

   public static float getExposure(Vec3 source, AABB box, boolean optimized) {
      if (!optimized) {
         return getExposure(source, box);
      }

      int miss = 0;
      int hit = 0;

      for (int k = 0; k <= 1; k++) {
         for (int l = 0; l <= 1; l++) {
            for (int m = 0; m <= 1; m++) {
               double n = Mth.lerp(k, box.minX, box.maxX);
               double o = Mth.lerp(l, box.minY, box.maxY);
               double p = Mth.lerp(m, box.minZ, box.maxZ);
               Vec3 vec3d = new Vec3(n, o, p);
               if (raycast(vec3d, source, false) == Type.MISS) {
                  miss++;
               }

               hit++;
            }
         }
      }

      return (float)miss / hit;
   }

   public static float getExposure(Vec3 source, AABB box) {
      double d = 0.4545454446934474;
      double e = 0.21739130885479366;
      double f = 0.4545454446934474;
      int i = 0;
      int j = 0;

      for (double k = 0.0; k <= 1.0; k += d) {
         for (double l = 0.0; l <= 1.0; l += e) {
            for (double m = 0.0; m <= 1.0; m += f) {
               double n = Mth.lerp(k, box.minX, box.maxX);
               double o = Mth.lerp(l, box.minY, box.maxY);
               double p = Mth.lerp(m, box.minZ, box.maxZ);
               Vec3 vec3d = new Vec3(n + 0.045454555306552624, o, p + 0.045454555306552624);
               if (raycast(vec3d, source, false) == Type.MISS) {
                  i++;
               }

               j++;
            }
         }
      }

      return (float)i / j;
   }

   private static BlockHitResult raycastGhost(ClipContext context, BlockPos bPos) {
      return (BlockHitResult)BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (innerContext, pos) -> {
         Vec3 vec3d = innerContext.getFrom();
         Vec3 vec3d2 = innerContext.getTo();
         BlockState blockState;
         if (!pos.equals(bPos)) {
            blockState = Module.mc.level.getBlockState(bPos);
         } else {
            blockState = Blocks.OBSIDIAN.defaultBlockState();
         }

         VoxelShape voxelShape = innerContext.getBlockShape(blockState, Module.mc.level, pos);
         BlockHitResult blockHitResult = Module.mc.level.clipWithInteractionOverride(vec3d, vec3d2, pos, voxelShape, blockState);
         BlockHitResult blockHitResult2 = Shapes.empty().clip(vec3d, vec3d2, pos);
         double d = blockHitResult == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(blockHitResult.getLocation());
         double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(blockHitResult2.getLocation());
         return d <= e ? blockHitResult : blockHitResult2;
      }, innerContext -> {
         Vec3 vec3d = innerContext.getFrom().subtract(innerContext.getTo());
         return BlockHitResult.miss(innerContext.getTo(), Direction.getApproximateNearest(vec3d.x, vec3d.y, vec3d.z), BlockPos.containing(innerContext.getTo()));
      });
   }

   public static Type raycast(Vec3 start, Vec3 end, boolean ignoreTerrain) {
      return (Type)BlockGetter.traverseBlocks(start, end, null, (innerContext, blockPos) -> {
         BlockState blockState = Module.mc.level.getBlockState(blockPos);
         if (blockState.getBlock().getExplosionResistance() < 600.0F && ignoreTerrain) {
            return null;
         }

         BlockHitResult hitResult = blockState.getCollisionShape(Module.mc.level, blockPos).clip(start, end, blockPos);
         return hitResult == null ? null : hitResult.getType();
      }, innerContext -> Type.MISS);
   }

   public static int getProtectionAmount(Iterable<ItemStack> equipment) {
      MutableInt mutableInt = new MutableInt();
      equipment.forEach(i -> mutableInt.add(getProtectionAmount(i)));
      return mutableInt.intValue();
   }

   public static int getProtectionAmount(ItemStack stack) {
      if (Module.mc.level == null || stack == null || stack.isEmpty()) {
         return 0;
      }
      var enchantRegistry = Module.mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
      Holder<Enchantment> blastProt = enchantRegistry.get(Enchantments.BLAST_PROTECTION).orElse(null);
      Holder<Enchantment> prot = enchantRegistry.get(Enchantments.PROTECTION).orElse(null);
      int modifierBlast = blastProt != null ? EnchantmentHelper.getItemEnchantmentLevel(blastProt, stack) : 0;
      int modifier = prot != null ? EnchantmentHelper.getItemEnchantmentLevel(prot, stack) : 0;
      return modifierBlast * 2 + modifier;
   }
}
