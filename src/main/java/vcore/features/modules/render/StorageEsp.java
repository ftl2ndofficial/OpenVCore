package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import vcore.core.Managers;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.SettingGroup;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public class StorageEsp extends Module {
   public final Setting<Boolean> outline = new Setting<>("Outline", false);
   public final Setting<Boolean> fill = new Setting<>("Fill", true);
   public final Setting<Boolean> tracers = new Setting<>("Tracers", false);
   public final Setting<SettingGroup> storage = new Setting<>("Storage", new SettingGroup(false, 0));
   public final Setting<Boolean> chest = new Setting<>("Chest", true).addToGroup(this.storage);
   public final Setting<Boolean> trappedChest = new Setting<>("Trapped Chest", false).addToGroup(this.storage);
   public final Setting<Boolean> dispenser = new Setting<>("Dispenser", false).addToGroup(this.storage);
   public final Setting<Boolean> shulker = new Setting<>("Shulker", true).addToGroup(this.storage);
   public final Setting<Boolean> echest = new Setting<>("Ender Chest", true).addToGroup(this.storage);
   public final Setting<Boolean> furnace = new Setting<>("Furnace", false).addToGroup(this.storage);
   public final Setting<Boolean> hopper = new Setting<>("Hopper", false).addToGroup(this.storage);
   public final Setting<Boolean> barrels = new Setting<>("Barrel", true).addToGroup(this.storage);
   public final Setting<Boolean> cart = new Setting<>("Minecart", false).addToGroup(this.storage);
   public final Setting<Boolean> frame = new Setting<>("ItemFrame", false).addToGroup(this.storage);
   public final Setting<Boolean> spawner = new Setting<>("Spawner", true).addToGroup(this.storage);
   public final Setting<SettingGroup> color = new Setting<>("Color", new SettingGroup(false, 0));
   private final Setting<ColorSetting> chestColor = new Setting<>("ChestColor", new ColorSetting(new Color(-1711300608, true))).addToGroup(this.color);
   private final Setting<ColorSetting> trappedChestColor = new Setting<>("TrappedChestColor", new ColorSetting(new Color(-1711341568, true)))
      .addToGroup(this.color);
   private final Setting<ColorSetting> shulkColor = new Setting<>("ShulkerColor", new ColorSetting(new Color(-1711300608, true))).addToGroup(this.color);
   private final Setting<ColorSetting> echestColor = new Setting<>("EChestColor", new ColorSetting(new Color(-1720188673, true))).addToGroup(this.color);
   private final Setting<ColorSetting> frameColor = new Setting<>("FrameColor", new ColorSetting(new Color(-1718842228, true))).addToGroup(this.color);
   private final Setting<ColorSetting> shulkerframeColor = new Setting<>("ShulkFrameColor", new ColorSetting(new Color(-1718842228, true)))
      .addToGroup(this.color);
   private final Setting<ColorSetting> furnaceColor = new Setting<>("FurnaceColor", new ColorSetting(new Color(-1718842228, true))).addToGroup(this.color);
   private final Setting<ColorSetting> hopperColor = new Setting<>("HopperColor", new ColorSetting(new Color(-1718842228, true))).addToGroup(this.color);
   private final Setting<ColorSetting> dispenserColor = new Setting<>("DispenserColor", new ColorSetting(new Color(-1718842228, true))).addToGroup(this.color);
   private final Setting<ColorSetting> barrelColor = new Setting<>("BarrelColor", new ColorSetting(new Color(-1711300608, true))).addToGroup(this.color);
   private final Setting<ColorSetting> minecartColor = new Setting<>("MinecartColor", new ColorSetting(new Color(-1711300608, true))).addToGroup(this.color);
   private final Setting<ColorSetting> spawnerColor = new Setting<>("SpawnerColor", new ColorSetting(new Color(-1727987713, true))).addToGroup(this.color);

   public StorageEsp() {
      super("StorageEsp", "Highlights storage blocks.", Module.Category.RENDER);
   }

   @Override
   public void onRender3D(PoseStack stack) {
      if (fullNullCheck() || mc.options.hideGui) {
         return;
      }

      List<BlockEntity> blockEntities = getBlockEntities();
      for (BlockEntity blockEntity : blockEntities) {
         Color color = this.getColor(blockEntity);
            if (color != null) {
               if (blockEntity instanceof ChestBlockEntity) {
                  BlockState state = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
                  ChestType chestType = (ChestType)state.getValue(ChestBlock.TYPE);
                  int originalAlpha = color.getAlpha();
                  Color drawColor = color;
                  if (chestType != ChestType.SINGLE) {
                     drawColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(1, originalAlpha / 2 + 17));
                  }

                  AABB chestBox;
                  if (chestType != ChestType.SINGLE) {
                     Direction facing = (Direction)state.getValue(ChestBlock.FACING);
                     BlockPos pos1 = blockEntity.getBlockPos();
                     BlockPos pos2 = pos1.relative(chestType == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
                     double minX = Math.min(pos1.getX(), pos2.getX()) + 0.06;
                     double minY = Math.min(pos1.getY(), pos2.getY());
                     double minZ = Math.min(pos1.getZ(), pos2.getZ()) + 0.06;
                     double maxX = Math.max(pos1.getX(), pos2.getX()) + 0.94;
                     double maxY = Math.max(pos1.getY(), pos2.getY()) - 0.125 + 1.0;
                     double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 0.94;
                     chestBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
                  } else {
                     chestBox = new AABB(
                        blockEntity.getBlockPos().getX() + 0.06,
                        blockEntity.getBlockPos().getY(),
                        blockEntity.getBlockPos().getZ() + 0.06,
                        blockEntity.getBlockPos().getX() + 0.94,
                        blockEntity.getBlockPos().getY() - 0.125 + 1.0,
                        blockEntity.getBlockPos().getZ() + 0.94
                     );
                  }

                  if (this.fill.getValue()) {
                     Render3DEngine.drawFilledBox(stack, chestBox, drawColor);
                  }

                  if (this.outline.getValue()) {
                     Render3DEngine.drawBoxOutline(chestBox, Render2DEngine.injectAlpha(drawColor, 255), 1.0F);
                  }
               } else {
                  AABB chestbox = new AABB(
                     blockEntity.getBlockPos().getX() + 0.06,
                     blockEntity.getBlockPos().getY(),
                     blockEntity.getBlockPos().getZ() + 0.06,
                     blockEntity.getBlockPos().getX() + 0.94,
                     blockEntity.getBlockPos().getY() - 0.125 + 1.0,
                     blockEntity.getBlockPos().getZ() + 0.94
                  );
                  if (this.fill.getValue()) {
                     if (blockEntity instanceof EnderChestBlockEntity) {
                        Render3DEngine.drawFilledBox(stack, chestbox, color);
                     } else {
                        Render3DEngine.drawFilledBox(stack, new AABB(blockEntity.getBlockPos()), color);
                     }
                  }

                  if (this.outline.getValue()) {
                     if (blockEntity instanceof EnderChestBlockEntity) {
                        Render3DEngine.drawBoxOutline(chestbox, Render2DEngine.injectAlpha(color, 255), 1.0F);
                     } else {
                        Render3DEngine.drawBoxOutline(new AABB(blockEntity.getBlockPos()), Render2DEngine.injectAlpha(color, 255), 1.0F);
                     }
                  }
               }
            }
         }

         for (Entity ent : Managers.ASYNC.getAsyncEntities()) {
            if (ent instanceof ItemFrame iframe && this.frame.getValue()) {
               Color frameColor1 = this.frameColor.getValue().getColorObject();
               if (iframe.getItem().getItem() instanceof BlockItem bitem && bitem.getBlock() instanceof ShulkerBoxBlock) {
                  frameColor1 = this.shulkerframeColor.getValue().getColorObject();
               }

               if (this.fill.getValue()) {
                  Render3DEngine.drawFilledBox(stack, iframe.getBoundingBox(), frameColor1);
               }

               if (this.outline.getValue()) {
                  Render3DEngine.drawBoxOutline(iframe.getBoundingBox(), Render2DEngine.injectAlpha(frameColor1, 255), 1.0F);
               }
            }

            if (ent instanceof MinecartChest mcart && this.cart.getValue()) {
               if (this.fill.getValue()) {
                  Render3DEngine.drawFilledBox(stack, mcart.getBoundingBox(), this.minecartColor.getValue().getColorObject());
               }

               if (this.outline.getValue()) {
                  Render3DEngine.drawBoxOutline(mcart.getBoundingBox(), Render2DEngine.injectAlpha(this.minecartColor.getValue().getColorObject(), 255), 1.0F);
               }
            }
         }

         if (this.tracers.getValue()) {
            double x1 = mc.player.xo + (mc.player.getX() - mc.player.xo) * Render3DEngine.getTickDelta();
            double y1 = mc.player.getEyeHeight(mc.player.getPose()) + mc.player.yo + (mc.player.getY() - mc.player.yo) * Render3DEngine.getTickDelta();
            double z1 = mc.player.zo + (mc.player.getZ() - mc.player.zo) * Render3DEngine.getTickDelta();
            Vec3 start = new Vec3(0.0, 0.0, 75.0)
               .xRot(-((float)Math.toRadians(mc.gameRenderer.getMainCamera().xRot())))
               .yRot(-((float)Math.toRadians(mc.gameRenderer.getMainCamera().yRot())))
               .add(x1, y1, z1);

            for (BlockEntity blockEntity : blockEntities) {
               Color color = this.getColor(blockEntity);
               if (color != null) {
                  if (blockEntity instanceof ChestBlockEntity) {
                     BlockState state = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
                     ChestType chestType = (ChestType)state.getValue(ChestBlock.TYPE);
                     Direction facing = (Direction)state.getValue(ChestBlock.FACING);
                     BlockPos pos1 = blockEntity.getBlockPos();
                     if (chestType != ChestType.SINGLE) {
                        BlockPos pos2 = pos1.relative(chestType == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
                        double midX = (pos1.getX() + pos2.getX()) / 2.0 + 0.5;
                        double midY = (pos1.getY() + pos2.getY()) / 2.0 + 0.5;
                        double midZ = (pos1.getZ() + pos2.getZ()) / 2.0 + 0.5;
                        Render3DEngine.drawLineDebug(start, new Vec3(midX, midY, midZ), color);
                        continue;
                     }
                  }

                  double x = blockEntity.getBlockPos().getX() + 0.5;
                  double y = blockEntity.getBlockPos().getY() + 0.5;
                  double z = blockEntity.getBlockPos().getZ() + 0.5;
                  Render3DEngine.drawLineDebug(start, new Vec3(x, y, z), color);
               }
            }

            for (Entity ent : Managers.ASYNC.getAsyncEntities()) {
               if (ent instanceof ItemFrame iframe && this.frame.getValue()) {
                  AABB box = iframe.getBoundingBox();
                  double x = (box.minX + box.maxX) / 2.0;
                  double y = (box.minY + box.maxY) / 2.0;
                  double z = (box.minZ + box.maxZ) / 2.0;
                  Color frameColor1 = this.frameColor.getValue().getColorObject();
                  if (iframe.getItem().getItem() instanceof BlockItem bitem && bitem.getBlock() instanceof ShulkerBoxBlock) {
                     frameColor1 = this.shulkerframeColor.getValue().getColorObject();
                  }

                  Render3DEngine.drawLineDebug(start, new Vec3(x, y, z), frameColor1);
               }

               if (ent instanceof MinecartChest mcart && this.cart.getValue()) {
                  AABB box = mcart.getBoundingBox();
                  double x = (box.minX + box.maxX) / 2.0;
                  double y = (box.minY + box.maxY) / 2.0;
                  double z = (box.minZ + box.maxZ) / 2.0;
                  Render3DEngine.drawLineDebug(start, new Vec3(x, y, z), this.minecartColor.getValue().getColorObject());
               }
            }
         }
      }

   @Nullable
   private Color getColor(BlockEntity bEnt) {
      Color color = null;
      if (bEnt instanceof TrappedChestBlockEntity && this.trappedChest.getValue()) {
         color = this.trappedChestColor.getValue().getColorObject();
      } else if (bEnt instanceof ChestBlockEntity && this.chest.getValue() && bEnt.getType() != BlockEntityType.TRAPPED_CHEST) {
         color = this.chestColor.getValue().getColorObject();
      } else if (bEnt instanceof EnderChestBlockEntity && this.echest.getValue()) {
         color = this.echestColor.getValue().getColorObject();
      } else if (bEnt instanceof BarrelBlockEntity && this.barrels.getValue()) {
         color = this.barrelColor.getValue().getColorObject();
      } else if (bEnt instanceof ShulkerBoxBlockEntity && this.shulker.getValue()) {
         color = this.shulkColor.getValue().getColorObject();
      } else if (bEnt instanceof AbstractFurnaceBlockEntity && this.furnace.getValue()) {
         color = this.furnaceColor.getValue().getColorObject();
      } else if (bEnt instanceof DispenserBlockEntity && this.dispenser.getValue()) {
         color = this.dispenserColor.getValue().getColorObject();
      } else if (bEnt instanceof HopperBlockEntity && this.hopper.getValue()) {
         color = this.hopperColor.getValue().getColorObject();
      } else if (bEnt instanceof SpawnerBlockEntity && this.spawner.getValue()) {
         color = this.spawnerColor.getValue().getColorObject();
      }

      return color;
   }

   public static List<BlockEntity> getBlockEntities() {
      List<BlockEntity> list = new ArrayList<>();
      if (mc.player == null || mc.level == null) {
         return list;
      }

      for (LevelChunk chunk : getLoadedChunks()) {
         try {
            list.addAll(chunk.getBlockEntities().values());
         } catch (Exception ignored) {}
      }

      return list;
   }

   public static List<LevelChunk> getLoadedChunks() {
      List<LevelChunk> chunks = new ArrayList<>();
      if (mc.player == null || mc.level == null) {
         return chunks;
      }
      int viewDist = Math.min((Integer)mc.options.renderDistance().get(), 16);
      int playerChunkX = ((int)Math.floor(mc.player.getX())) >> 4;
      int playerChunkZ = ((int)Math.floor(mc.player.getZ())) >> 4;

      for (int x = -viewDist; x <= viewDist; x++) {
         for (int z = -viewDist; z <= viewDist; z++) {
            LevelChunk chunk = mc.level.getChunkSource().getChunkNow(playerChunkX + x, playerChunkZ + z);
            if (chunk != null) {
               chunks.add(chunk);
            }
         }
      }

      return chunks;
   }
}
