package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventCollision;
import vcore.events.impl.EventPostSync;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.player.InteractionUtility;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.MovementUtility;

public class Phase extends Module {
   private final Setting<Phase.Mode> mode = new Setting<>("Mode", Phase.Mode.Pearl);
   private final Setting<Boolean> onlyOnGround = new Setting<>("OnlyOnGround", false, v -> this.mode.is(Phase.Mode.Pearl));
   private final Setting<Boolean> autoDisable = new Setting<>("AutoDisable", true, v -> this.mode.getValue() == Phase.Mode.Pearl);
   private final Setting<Integer> afterPearl = new Setting<>("PearlTimeout", 0, 0, 60, v -> this.mode.getValue() == Phase.Mode.Pearl);
   private final Setting<Float> pitch = new Setting<>("Pitch", 85.0F, 0.0F, 90.0F, v -> this.mode.getValue() == Phase.Mode.Pearl);
   public int clipTimer;
   public int afterPearlTime;

   public Phase() {
      super("Phase", "Walk through walls.", Module.Category.MOVEMENT);
   }

   @EventHandler
   public void onCollide(EventCollision e) {
      if (!fullNullCheck()) {
         if (this.afterPearlTime > 0) {
            BlockPos playerPos = BlockPos.containing(mc.player.position());
            if (!e.getPos().equals(playerPos.below()) || mc.options.keyShift.isDown()) {
               e.setState(Blocks.AIR.defaultBlockState());
            }
         }
      }
   }

   @Override
   public void onEnable() {
      this.afterPearlTime = 0;
      this.clipTimer = 0;
      if (mc.player.onGround() && this.mode.is(Phase.Mode.CCClip)) {
         double[] diagonalOffset = MovementUtility.forwardWithoutStrafe(0.44);
         boolean diagonal = mc.player.getYRot() % 90.0F > 35.0F && mc.player.getYRot() % 90.0F < 55.0F;
         this.sendPacket(new ServerboundPlayerCommandPacket(mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_SPRINTING));
         if (diagonal) {
            double[] directionVec = MovementUtility.forwardWithoutStrafe(0.51);
            int height = mc.level
                  .clip(
                     new ClipContext(
                        mc.player.getEyePosition(),
                        mc.player.getEyePosition().add(diagonalOffset[0], 0.0, diagonalOffset[1]),
                        Block.COLLIDER,
                        Fluid.NONE,
                        mc.player
                     )
                  )
                  .getType()
                  .equals(Type.MISS)
               ? 1
               : 2;
            mc.player.setPos(mc.player.getX() + directionVec[0], mc.player.getY() + height, mc.player.getZ() + directionVec[1]);
            this.sendPacket(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));
            height = mc.level.isEmptyBlock(BlockPos.containing(mc.player.position().add(diagonalOffset[0], -2.0, diagonalOffset[1]))) ? 2 : 1;
            mc.player.setPos(mc.player.getX() + directionVec[0], mc.player.getY() - height, mc.player.getZ() + directionVec[1]);
            this.sendPacket(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));
            this.disable("diagonal");
         } else {
            double[] directionVec = MovementUtility.forwardWithoutStrafe(0.57);
            int height = mc.level
                  .clip(
                     new ClipContext(
                        mc.player.getEyePosition(),
                        mc.player.getEyePosition().add(diagonalOffset[0], 0.0, diagonalOffset[1]),
                        Block.COLLIDER,
                        Fluid.NONE,
                        mc.player
                     )
                  )
                  .getType()
                  .equals(Type.MISS)
               ? 1
               : 2;
            mc.player.setPos(mc.player.getX() + directionVec[0], mc.player.getY() + height, mc.player.getZ() + directionVec[1]);
            this.sendPacket(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));
            mc.player.setPos(mc.player.getX() + directionVec[0], mc.player.getY(), mc.player.getZ() + directionVec[1]);
            this.sendPacket(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));
            height = mc.level.isEmptyBlock(BlockPos.containing(mc.player.position().add(diagonalOffset[0], -2.0, diagonalOffset[1]))) ? 2 : 1;
            mc.player.setPos(mc.player.getX() + directionVec[0], mc.player.getY() - height, mc.player.getZ() + directionVec[1]);
            this.sendPacket(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));
            this.disable("normal");
         }
      }
   }

   @EventHandler
   public void onSync(EventSync e) {
      if (!fullNullCheck()) {
         if (this.clipTimer > 0) {
            this.clipTimer--;
         }

         if (this.afterPearlTime > 0) {
            this.afterPearlTime--;
         }

         if (this.mode.getValue() == Phase.Mode.Pearl
            && (mc.player.onGround() || !this.onlyOnGround.getValue())
            && mc.player.horizontalCollision
            && !this.playerInsideBlock()
            && this.clipTimer <= 0
            && mc.player.tickCount > 60) {
            if (mc.options.keyShift.isDown()) {
               return;
            }

            double[] dir = MovementUtility.forward(0.5);
            BlockPos block = BlockPos.containing(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1]);
            float[] angle = InteractionUtility.calculateAngle(Vec3.atCenterOf(block));
            int epSlot = this.findEPSlot();
            if (epSlot != -1) {
               ModuleManager.aura.pause();
               mc.player.setYRot(angle[0]);
               mc.player.setXRot(this.pitch.getValue());
            }
         }
      }
   }

   @EventHandler
   public void onPostSync(EventPostSync e) {
      if (this.mode.getValue() == Phase.Mode.Pearl
         && (mc.player.onGround() || !this.onlyOnGround.getValue())
         && mc.player.horizontalCollision
         && !this.playerInsideBlock()
         && this.clipTimer <= 0
         && mc.player.tickCount > 60) {
         if (mc.options.keyShift.isDown()) {
            return;
         }

         int epSlot = this.findEPSlot();
         int prevItem = mc.player.getInventory().getSelectedSlot();
         if (epSlot != -1) {
            InventoryUtility.switchTo(epSlot);
            this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
            this.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            InventoryUtility.switchTo(prevItem);
            if (this.autoDisable.getValue()) {
               this.disable();
            }
         }

         this.clipTimer = 20;
         this.afterPearlTime = this.afterPearl.getValue();
      }
   }

   private int findEPSlot() {
      int epSlot = -1;
      if (mc.player.getMainHandItem().getItem() == Items.ENDER_PEARL) {
         epSlot = mc.player.getInventory().getSelectedSlot();
      }

      if (epSlot == -1) {
         for (int l = 0; l < 9; l++) {
            if (mc.player.getInventory().getItem(l).getItem() == Items.ENDER_PEARL) {
               epSlot = l;
               break;
            }
         }
      }

      return epSlot;
   }

   public boolean playerInsideBlock() {
      BlockPos pos = BlockPos.containing(mc.player.position());
      BlockState state = mc.level.getBlockState(pos);
      return !state.isAir() && state.isCollisionShapeFullBlock(mc.level, pos);
   }

   private enum Mode {
      Pearl,
      CCClip;
   }
}
