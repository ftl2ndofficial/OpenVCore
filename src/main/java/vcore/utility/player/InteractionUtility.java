package vcore.utility.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vcore.features.modules.Module;
import vcore.injection.accesors.IClientWorldMixin;
import vcore.utility.world.ExplosionUtility;

public final class InteractionUtility {
   private static final List<Block> SHIFT_BLOCKS;

   static {
      List<Block> list = new ArrayList<>(Arrays.asList(
         Blocks.ENDER_CHEST,
         Blocks.CHEST,
         Blocks.TRAPPED_CHEST,
         Blocks.CRAFTING_TABLE,
         Blocks.BIRCH_TRAPDOOR,
         Blocks.BAMBOO_TRAPDOOR,
         Blocks.DARK_OAK_TRAPDOOR,
         Blocks.CHERRY_TRAPDOOR,
         Blocks.ANVIL,
         Blocks.BREWING_STAND,
         Blocks.HOPPER,
         Blocks.DROPPER,
         Blocks.DISPENSER,
         Blocks.ACACIA_TRAPDOOR,
         Blocks.ENCHANTING_TABLE,
         Blocks.SHULKER_BOX
      ));
      list.addAll(List.of(
         Blocks.WHITE_SHULKER_BOX,
         Blocks.ORANGE_SHULKER_BOX,
         Blocks.MAGENTA_SHULKER_BOX,
         Blocks.LIGHT_BLUE_SHULKER_BOX,
         Blocks.YELLOW_SHULKER_BOX,
         Blocks.LIME_SHULKER_BOX,
         Blocks.PINK_SHULKER_BOX,
         Blocks.GRAY_SHULKER_BOX,
         Blocks.LIGHT_GRAY_SHULKER_BOX,
         Blocks.CYAN_SHULKER_BOX,
         Blocks.PURPLE_SHULKER_BOX,
         Blocks.BLUE_SHULKER_BOX,
         Blocks.BROWN_SHULKER_BOX,
         Blocks.GREEN_SHULKER_BOX,
         Blocks.RED_SHULKER_BOX,
         Blocks.BLACK_SHULKER_BOX
      ));
      SHIFT_BLOCKS = Collections.unmodifiableList(list);
   }
   public static Map<BlockPos, Long> awaiting = new HashMap<>();

   public static boolean canSee(Vec3 vec) {
      return canSee(vec, vec);
   }

   public static boolean canSee(Entity entity) {
      Vec3 entityEyes = getEyesPos(entity);
      Vec3 entityPos = entity.position();
      return canSee(entityEyes, entityPos);
   }

   public static boolean canSee(Vec3 entityEyes, Vec3 entityPos) {
      if (Module.mc.player != null && Module.mc.level != null) {
         Vec3 playerEyes = getEyesPos(Module.mc.player);
         if (ExplosionUtility.raycast(playerEyes, entityEyes, false) == Type.MISS) {
            return true;
         } else {
            return playerEyes.y() > entityPos.y() ? ExplosionUtility.raycast(playerEyes, entityEyes, false) == Type.MISS : false;
         }
      } else {
         return false;
      }
   }

   public static Vec3 getEyesPos(@NotNull Entity entity) {
      return entity.position().add(0.0, entity.getEyeHeight(entity.getPose()), 0.0);
   }

   public static float @NotNull [] calculateAngle(Vec3 to) {
      return calculateAngle(getEyesPos(Module.mc.player), to);
   }

   public static float @NotNull [] calculateAngle(@NotNull Vec3 from, @NotNull Vec3 to) {
      double difX = to.x - from.x;
      double difY = (to.y - from.y) * -1.0;
      double difZ = to.z - from.z;
      double dist = Mth.sqrt((float)(difX * difX + difZ * difZ));
      float yD = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0);
      float pD = (float)Mth.clamp(Mth.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist))), -90.0, 90.0);
      return new float[]{yD, pD};
   }

   public static boolean placeBlock(
      BlockPos bp,
      InteractionUtility.Rotate rotate,
      InteractionUtility.Interact interact,
      InteractionUtility.PlaceMode mode,
      int slot,
      boolean returnSlot,
      boolean ignoreEntities
   ) {
      int prevItem = Module.mc.player.getInventory().getSelectedSlot();
      if (slot != -1) {
         InventoryUtility.switchTo(slot);
         boolean result = placeBlock(bp, rotate, interact, mode, ignoreEntities);
         if (returnSlot) {
            InventoryUtility.switchTo(prevItem);
         }

         return result;
      } else {
         return false;
      }
   }

   public static boolean placeBlock(
      BlockPos bp,
      InteractionUtility.Rotate rotate,
      InteractionUtility.Interact interact,
      InteractionUtility.PlaceMode mode,
      @NotNull SearchInvResult invResult,
      boolean returnSlot,
      boolean ignoreEntities
   ) {
      int prevItem = Module.mc.player.getInventory().getSelectedSlot();
      invResult.switchTo();
      boolean result = placeBlock(bp, rotate, interact, mode, ignoreEntities);
      if (returnSlot) {
         InventoryUtility.switchTo(prevItem);
      }

      return result;
   }

   public static boolean placeBlock(
      BlockPos bp, InteractionUtility.Rotate rotate, InteractionUtility.Interact interact, InteractionUtility.PlaceMode mode, boolean ignoreEntities
   ) {
      BlockHitResult result = getPlaceResult(bp, interact, ignoreEntities);
      if (result != null && Module.mc.level != null && Module.mc.gameMode != null && Module.mc.player != null) {
         boolean sprint = Module.mc.player.isSprinting();
         boolean sneak = needSneak(Module.mc.level.getBlockState(result.getBlockPos()).getBlock()) && !Module.mc.player.isShiftKeyDown();
         Input input = Module.mc.player.input.keyPresses;
         if (sprint) {
            Module.mc.player.connection.send(new ServerboundPlayerCommandPacket(Module.mc.player, Action.STOP_SPRINTING));
         }

         if (sneak) {
            Module.mc.player.connection.send(new ServerboundPlayerInputPacket(new Input(
               input.forward(), input.backward(), input.left(), input.right(), input.jump(), true, input.sprint()
            )));
         }

         float[] angle = calculateAngle(result.getLocation());
         switch (rotate) {
            case None:
            default:
               break;
            case Default:
               Module.mc.player.connection.send(new Rot(angle[0], angle[1], Module.mc.player.onGround(), Module.mc.player.horizontalCollision));
               break;
            case Grim:
               Module.mc
                  .player
                  .connection
                  .send(
                     new PosRot(Module.mc.player.getX(), Module.mc.player.getY(), Module.mc.player.getZ(), angle[0], angle[1], Module.mc.player.onGround(), Module.mc.player.horizontalCollision)
                  );
         }

         if (mode == InteractionUtility.PlaceMode.Normal) {
            Module.mc.gameMode.useItemOn(Module.mc.player, InteractionHand.MAIN_HAND, result);
         }

         if (mode == InteractionUtility.PlaceMode.Packet) {
            sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, result, id));
         }

         awaiting.put(bp, System.currentTimeMillis());
         if (rotate == InteractionUtility.Rotate.Grim) {
            Module.mc
               .player
               .connection
               .send(
                  new PosRot(
                     Module.mc.player.getX(),
                     Module.mc.player.getY(),
                     Module.mc.player.getZ(),
                     Module.mc.player.getYRot(),
                     Module.mc.player.getXRot(),
                     Module.mc.player.onGround(),
                     Module.mc.player.horizontalCollision
                  )
               );
         }

         if (sneak) {
            Module.mc.player.connection.send(new ServerboundPlayerInputPacket(new Input(
               input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.shift(), input.sprint()
            )));
         }

         if (sprint) {
            Module.mc.player.connection.send(new ServerboundPlayerCommandPacket(Module.mc.player, Action.START_SPRINTING));
         }

         Module.mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
         return true;
      } else {
         return false;
      }
   }

   public static boolean canPlaceBlock(@NotNull BlockPos bp, InteractionUtility.Interact interact, boolean ignoreEntities) {
      return awaiting.containsKey(bp) ? false : getPlaceResult(bp, interact, ignoreEntities) != null;
   }

   public static float @Nullable [] getPlaceAngle(@NotNull BlockPos bp, InteractionUtility.Interact interact, boolean ignoreEntities) {
      BlockHitResult result = getPlaceResult(bp, interact, ignoreEntities);
      return result != null ? calculateAngle(result.getLocation()) : null;
   }

   public static void sendSequencedPacket(PredictiveAction packetCreator) {
      if (Module.mc.getConnection() != null && Module.mc.level != null) {
         BlockStatePredictionHandler pendingUpdateManager = ((IClientWorldMixin)Module.mc.level).getPendingUpdateManager().startPredicting();

         try {
            int i = pendingUpdateManager.currentSequence();
            Module.mc.getConnection().send(packetCreator.predict(i));
         } catch (Throwable var5) {
            if (pendingUpdateManager != null) {
               try {
                  pendingUpdateManager.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (pendingUpdateManager != null) {
            pendingUpdateManager.close();
         }
      }
   }

   @Nullable
   public static BlockHitResult getPlaceResult(@NotNull BlockPos bp, InteractionUtility.Interact interact, boolean ignoreEntities) {
      if (!ignoreEntities) {
         for (Entity entity : Module.mc.level.getEntitiesOfClass(Entity.class, new AABB(bp))) {
            if (!(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb)) {
               return null;
            }
         }
      }

      if (!Module.mc.level.getBlockState(bp).canBeReplaced()) {
         return null;
      }

      if (interact == InteractionUtility.Interact.AirPlace) {
         return ExplosionUtility.rayCastBlock(
            new ClipContext(getEyesPos(Module.mc.player), Vec3.atCenterOf(bp), net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, Module.mc.player), bp
         );
      }

      ArrayList<InteractionUtility.BlockPosWithFacing> supports = getSupportBlocks(bp);
      Iterator var9 = supports.iterator();

      InteractionUtility.BlockPosWithFacing support;
      List<Direction> dirs;
      do {
         if (!var9.hasNext()) {
            return null;
         }

         support = (InteractionUtility.BlockPosWithFacing)var9.next();
         if (interact == InteractionUtility.Interact.Vanilla) {
            break;
         }

         dirs = getStrictDirections(bp);
         if (dirs.isEmpty()) {
            return null;
         }
      } while (!dirs.contains(support.facing));

      BlockHitResult result = null;
      if (interact == InteractionUtility.Interact.Legit) {
         Vec3 p = getVisibleDirectionPoint(support.facing, support.position, 0.0F, 6.0F);
         if (p != null) {
            return new BlockHitResult(p, support.facing, support.position, false);
         }
      } else {
         Vec3 directionVec = new Vec3(
            support.position.getX() + 0.5 + support.facing.getUnitVec3i().getX() * 0.5,
            support.position.getY() + 0.5 + support.facing.getUnitVec3i().getY() * 0.5,
            support.position.getZ() + 0.5 + support.facing.getUnitVec3i().getZ() * 0.5
         );
         result = new BlockHitResult(directionVec, support.facing, support.position, false);
      }

      return result;
   }

   @NotNull
   public static ArrayList<InteractionUtility.BlockPosWithFacing> getSupportBlocks(@NotNull BlockPos bp) {
      ArrayList<InteractionUtility.BlockPosWithFacing> list = new ArrayList<>();
      if (Module.mc.level.getBlockState(bp.offset(0, -1, 0)).isSolid() || awaiting.containsKey(bp.offset(0, -1, 0))) {
         list.add(new InteractionUtility.BlockPosWithFacing(bp.offset(0, -1, 0), Direction.UP));
      }

      if (Module.mc.level.getBlockState(bp.offset(0, 1, 0)).isSolid() || awaiting.containsKey(bp.offset(0, 1, 0))) {
         list.add(new InteractionUtility.BlockPosWithFacing(bp.offset(0, 1, 0), Direction.DOWN));
      }

      if (Module.mc.level.getBlockState(bp.offset(-1, 0, 0)).isSolid() || awaiting.containsKey(bp.offset(-1, 0, 0))) {
         list.add(new InteractionUtility.BlockPosWithFacing(bp.offset(-1, 0, 0), Direction.EAST));
      }

      if (Module.mc.level.getBlockState(bp.offset(1, 0, 0)).isSolid() || awaiting.containsKey(bp.offset(1, 0, 0))) {
         list.add(new InteractionUtility.BlockPosWithFacing(bp.offset(1, 0, 0), Direction.WEST));
      }

      if (Module.mc.level.getBlockState(bp.offset(0, 0, 1)).isSolid() || awaiting.containsKey(bp.offset(0, 0, 1))) {
         list.add(new InteractionUtility.BlockPosWithFacing(bp.offset(0, 0, 1), Direction.NORTH));
      }

      if (Module.mc.level.getBlockState(bp.offset(0, 0, -1)).isSolid() || awaiting.containsKey(bp.offset(0, 0, -1))) {
         list.add(new InteractionUtility.BlockPosWithFacing(bp.offset(0, 0, -1), Direction.SOUTH));
      }

      return list;
   }

   @Nullable
   public static InteractionUtility.BlockPosWithFacing checkNearBlocks(@NotNull BlockPos blockPos) {
      if (Module.mc.level.getBlockState(blockPos.offset(0, -1, 0)).isSolid()) {
         return new InteractionUtility.BlockPosWithFacing(blockPos.offset(0, -1, 0), Direction.UP);
      } else if (Module.mc.level.getBlockState(blockPos.offset(-1, 0, 0)).isSolid()) {
         return new InteractionUtility.BlockPosWithFacing(blockPos.offset(-1, 0, 0), Direction.EAST);
      } else if (Module.mc.level.getBlockState(blockPos.offset(1, 0, 0)).isSolid()) {
         return new InteractionUtility.BlockPosWithFacing(blockPos.offset(1, 0, 0), Direction.WEST);
      } else if (Module.mc.level.getBlockState(blockPos.offset(0, 0, 1)).isSolid()) {
         return new InteractionUtility.BlockPosWithFacing(blockPos.offset(0, 0, 1), Direction.NORTH);
      } else {
         return Module.mc.level.getBlockState(blockPos.offset(0, 0, -1)).isSolid()
            ? new InteractionUtility.BlockPosWithFacing(blockPos.offset(0, 0, -1), Direction.SOUTH)
            : null;
      }
   }

   public static float squaredDistanceFromEyes(@NotNull Vec3 vec) {
      double d0 = vec.x - Module.mc.player.getX();
      double d1 = vec.z - Module.mc.player.getZ();
      double d2 = vec.y - (Module.mc.player.getY() + Module.mc.player.getEyeHeight(Module.mc.player.getPose()));
      return (float)(d0 * d0 + d1 * d1 + d2 * d2);
   }

   public static float squaredDistanceFromEyes2d(@NotNull Vec3 vec) {
      double d0 = vec.x - Module.mc.player.getX();
      double d1 = vec.z - Module.mc.player.getZ();
      return (float)(d0 * d0 + d1 * d1);
   }

   @NotNull
   public static List<Direction> getStrictDirections(@NotNull BlockPos bp) {
      List<Direction> visibleSides = new ArrayList<>();
      Vec3 positionVector = Vec3.atCenterOf(bp);
      double westDelta = getEyesPos(Module.mc.player).x - positionVector.add(0.5, 0.0, 0.0).x;
      double eastDelta = getEyesPos(Module.mc.player).x - positionVector.add(-0.5, 0.0, 0.0).x;
      double northDelta = getEyesPos(Module.mc.player).z - positionVector.add(0.0, 0.0, 0.5).z;
      double southDelta = getEyesPos(Module.mc.player).z - positionVector.add(0.0, 0.0, -0.5).z;
      double upDelta = getEyesPos(Module.mc.player).y - positionVector.add(0.0, 0.5, 0.0).y;
      double downDelta = getEyesPos(Module.mc.player).y - positionVector.add(0.0, -0.5, 0.0).y;
      if (westDelta > 0.0 && isSolid(bp.west())) {
         visibleSides.add(Direction.EAST);
      }

      if (westDelta < 0.0 && isSolid(bp.east())) {
         visibleSides.add(Direction.WEST);
      }

      if (eastDelta < 0.0 && isSolid(bp.east())) {
         visibleSides.add(Direction.WEST);
      }

      if (eastDelta > 0.0 && isSolid(bp.west())) {
         visibleSides.add(Direction.EAST);
      }

      if (northDelta > 0.0 && isSolid(bp.north())) {
         visibleSides.add(Direction.SOUTH);
      }

      if (northDelta < 0.0 && isSolid(bp.south())) {
         visibleSides.add(Direction.NORTH);
      }

      if (southDelta < 0.0 && isSolid(bp.south())) {
         visibleSides.add(Direction.NORTH);
      }

      if (southDelta > 0.0 && isSolid(bp.north())) {
         visibleSides.add(Direction.SOUTH);
      }

      if (upDelta > 0.0 && isSolid(bp.below())) {
         visibleSides.add(Direction.UP);
      }

      if (upDelta < 0.0 && isSolid(bp.above())) {
         visibleSides.add(Direction.DOWN);
      }

      if (downDelta < 0.0 && isSolid(bp.above())) {
         visibleSides.add(Direction.DOWN);
      }

      if (downDelta > 0.0 && isSolid(bp.below())) {
         visibleSides.add(Direction.UP);
      }

      return visibleSides;
   }

   public static boolean isSolid(BlockPos bp) {
      return Module.mc.level.getBlockState(bp).isSolid() || awaiting.containsKey(bp);
   }

   @NotNull
   public static List<Direction> getStrictBlockDirections(@NotNull BlockPos bp) {
      List<Direction> visibleSides = new ArrayList<>();
      Vec3 pV = Vec3.atCenterOf(bp);
      double westDelta = getEyesPos(Module.mc.player).x - pV.add(0.5, 0.0, 0.0).x;
      double eastDelta = getEyesPos(Module.mc.player).x - pV.add(-0.5, 0.0, 0.0).x;
      double northDelta = getEyesPos(Module.mc.player).z - pV.add(0.0, 0.0, 0.5).z;
      double southDelta = getEyesPos(Module.mc.player).z - pV.add(0.0, 0.0, -0.5).z;
      double upDelta = getEyesPos(Module.mc.player).y - pV.add(0.0, 0.5, 0.0).y;
      double downDelta = getEyesPos(Module.mc.player).y - pV.add(0.0, -0.5, 0.0).y;
      if (westDelta > 0.0 && Module.mc.level.getBlockState(bp.east()).canBeReplaced()) {
         visibleSides.add(Direction.EAST);
      }

      if (eastDelta < 0.0 && Module.mc.level.getBlockState(bp.west()).canBeReplaced()) {
         visibleSides.add(Direction.WEST);
      }

      if (northDelta > 0.0 && Module.mc.level.getBlockState(bp.south()).canBeReplaced()) {
         visibleSides.add(Direction.SOUTH);
      }

      if (southDelta < 0.0 && Module.mc.level.getBlockState(bp.north()).canBeReplaced()) {
         visibleSides.add(Direction.NORTH);
      }

      if (upDelta > 0.0 && Module.mc.level.getBlockState(bp.above()).canBeReplaced()) {
         visibleSides.add(Direction.UP);
      }

      if (downDelta < 0.0 && Module.mc.level.getBlockState(bp.below()).canBeReplaced()) {
         visibleSides.add(Direction.DOWN);
      }

      return visibleSides;
   }

   @Nullable
   public static InteractionUtility.BreakData getBreakData(BlockPos bp, InteractionUtility.Interact interact) {
      if (interact == InteractionUtility.Interact.Vanilla) {
         return new InteractionUtility.BreakData(Direction.UP, Vec3.atCenterOf(bp).add(0.0, 0.5, 0.0));
      }

      if (interact == InteractionUtility.Interact.Strict) {
         float bestDistance = 999.0F;
         Direction bestDirection = Direction.UP;
         Vec3 bestVector = null;

         for (Direction dir : Direction.values()) {
            Vec3 directionVec = new Vec3(
               bp.getX() + 0.5 + dir.getUnitVec3i().getX() * 0.5, bp.getY() + 0.5 + dir.getUnitVec3i().getY() * 0.5, bp.getZ() + 0.5 + dir.getUnitVec3i().getZ() * 0.5
            );
            float distance = squaredDistanceFromEyes(directionVec);
            if (bestDistance > distance) {
               bestDirection = dir;
               bestVector = directionVec;
               bestDistance = distance;
            }
         }

         return bestVector == null ? null : new InteractionUtility.BreakData(bestDirection, bestVector);
      } else {
         if (interact != InteractionUtility.Interact.Legit) {
            return null;
         }

         float bestDistance = 999.0F;
         InteractionUtility.BreakData bestData = null;

         for (float x = 0.0F; x <= 1.0F; x += 0.2F) {
            for (float y = 0.0F; y <= 1.0F; y += 0.2F) {
               for (float z = 0.0F; z <= 1.0F; z += 0.2F) {
                  Vec3 point = new Vec3(bp.getX() + x, bp.getY() + y, bp.getZ() + z);
                  BlockHitResult wallCheck = Module.mc
                     .level
                     .clip(new ClipContext(getEyesPos(Module.mc.player), point, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, Module.mc.player));
                  if (wallCheck == null || wallCheck.getType() != Type.BLOCK || wallCheck.getBlockPos().equals(bp)) {
                     BlockHitResult result = ExplosionUtility.rayCastBlock(
                        new ClipContext(getEyesPos(Module.mc.player), point, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, Module.mc.player), bp
                     );
                     if (squaredDistanceFromEyes(point) < bestDistance && result != null && result.getType() == Type.BLOCK) {
                        bestData = new InteractionUtility.BreakData(result.getDirection(), result.getLocation());
                     }
                  }
               }
            }
         }

         if (bestData == null) {
            return null;
         } else {
            return bestData.vector != null && bestData.dir != null ? bestData : null;
         }
      }
   }

   @Nullable
   public static Vec3 getVisibleDirectionPoint(@NotNull Direction dir, @NotNull BlockPos bp, float wallRange, float range) {
      AABB brutBox = getDirectionBox(dir);
      if (brutBox.maxX - brutBox.minX == 0.0) {
         for (double y = brutBox.minY; y < brutBox.maxY; y += 0.1F) {
            for (double z = brutBox.minZ; z < brutBox.maxZ; z += 0.1F) {
               Vec3 point = new Vec3(bp.getX() + brutBox.minX, bp.getY() + y, bp.getZ() + z);
               if (!shouldSkipPoint(point, bp, dir, wallRange, range)) {
                  return point;
               }
            }
         }
      }

      if (brutBox.maxY - brutBox.minY == 0.0) {
         for (double x = brutBox.minX; x < brutBox.maxX; x += 0.1F) {
            for (double z = brutBox.minZ; z < brutBox.maxZ; z += 0.1F) {
               Vec3 point = new Vec3(bp.getX() + x, bp.getY() + brutBox.minY, bp.getZ() + z);
               if (!shouldSkipPoint(point, bp, dir, wallRange, range)) {
                  return point;
               }
            }
         }
      }

      if (brutBox.maxZ - brutBox.minZ == 0.0) {
         for (double x = brutBox.minX; x < brutBox.maxX; x += 0.1F) {
            for (double y = brutBox.minY; y < brutBox.maxY; y += 0.1F) {
               Vec3 point = new Vec3(bp.getX() + x, bp.getY() + y, bp.getZ() + brutBox.minZ);
               if (!shouldSkipPoint(point, bp, dir, wallRange, range)) {
                  return point;
               }
            }
         }
      }

      return null;
   }

   @NotNull
   private static AABB getDirectionBox(Direction dir) {
      return switch (dir) {
         case UP -> new AABB(0.15F, 1.0, 0.15F, 0.85F, 1.0, 0.85F);
         case DOWN -> new AABB(0.15F, 0.0, 0.15F, 0.85F, 0.0, 0.85F);
         case EAST -> new AABB(1.0, 0.15F, 0.15F, 1.0, 0.85F, 0.85F);
         case WEST -> new AABB(0.0, 0.15F, 0.15F, 0.0, 0.85F, 0.85F);
         case NORTH -> new AABB(0.15F, 0.15F, 0.0, 0.85F, 0.85F, 0.0);
         case SOUTH -> new AABB(0.15F, 0.15F, 1.0, 0.85F, 0.85F, 1.0);
         default -> throw new MatchException(null, null);
      };
   }

   private static boolean shouldSkipPoint(Vec3 point, BlockPos bp, Direction dir, float wallRange, float range) {
      ClipContext context = new ClipContext(getEyesPos(Module.mc.player), point, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, Module.mc.player);
      BlockHitResult result = Module.mc.level.clip(context);
      float dst = squaredDistanceFromEyes(point);
      return result != null && result.getType() == Type.BLOCK && !result.getBlockPos().equals(bp) && dst > wallRange * wallRange ? true : dst > range * range;
   }

   public static boolean needSneak(Block in) {
      return SHIFT_BLOCKS.contains(in);
   }

   public static void lookAt(BlockPos bp) {
      if (bp != null) {
         float[] angle = calculateAngle(Vec3.atCenterOf(bp));
         Module.mc.player.setYRot(angle[0]);
         Module.mc.player.setXRot(angle[1]);
      }
   }

   public static boolean isVecInFOV(Vec3 pos, Integer fov) {
      double deltaX = pos.x() - Module.mc.player.getX();
      double deltaZ = pos.z() - Module.mc.player.getZ();
      float yawDelta = Mth.wrapDegrees(
         (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0) - Mth.wrapDegrees(Module.mc.player.getYRot())
      );
      return Math.abs(yawDelta) <= fov.intValue();
   }

   public record BlockPosWithFacing(BlockPos position, Direction facing) {
   }

   public record BreakData(Direction dir, Vec3 vector) {
   }

   public enum Interact {
      Vanilla,
      Strict,
      Legit,
      AirPlace;
   }

   public enum PlaceMode {
      Packet,
      Normal;
   }

   public enum Rotate {
      None,
      Default,
      Grim;
   }
}
