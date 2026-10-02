package vcore.features.modules.render;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.utility.Timer;
import vcore.utility.player.PlayerUtility;
import vcore.utility.render.Render3DEngine;

public class BlockESP extends Module {
   public final Setting<ItemSelectSetting> selectedBlocks = new Setting<>("SelectedBlocks", new ItemSelectSetting(new ArrayList<>()));
   public static List<BlockESP.BlockVec> blocks = new CopyOnWriteArrayList<>();
   private final Setting<Integer> range = new Setting<>("Range", 100, 1, 128);
   private final Setting<BooleanSettingGroup> limit = new Setting<>("Limit", new BooleanSettingGroup(true));
   private final Setting<Integer> limitCount = new Setting<>("LimitCount", 50, 1, 2048).addToGroup(this.limit);
   private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(-16711681));
   private final Setting<Boolean> illegals = new Setting<>("Illegals", true);
   private final Setting<Boolean> tracers = new Setting<>("Tracers", false);
   private final Setting<Boolean> fill = new Setting<>("Fill", true);
   private final Setting<Boolean> outline = new Setting<>("Outline", true);
   private final ExecutorService searchThread = Executors.newSingleThreadExecutor();
   private final Timer searchTimer = new Timer();
   private long lastFrameTime;
   private boolean canContinue;

   public BlockESP() {
      super("BlockESP", "Highlights selected blocks.", Module.Category.RENDER);
   }

   @Override
   public void onEnable() {
      blocks.clear();
      this.lastFrameTime = System.currentTimeMillis();
      this.canContinue = true;
   }

   @Override
   public void onUpdate() {
      if (fullNullCheck()) {
         blocks.clear();
         this.canContinue = true;
         return;
      }

      if (this.searchTimer.every(1000L) && this.canContinue) {
         this.canContinue = false;
         CompletableFuture.supplyAsync(this::scan, this.searchThread)
            .thenAcceptAsync(this::sync, Util.backgroundExecutor())
            .exceptionally(t -> {
               this.canContinue = true;
               return null;
            });
      }
   }

   private ArrayList<BlockESP.BlockVec> scan() {
      ArrayList<BlockESP.BlockVec> found = new ArrayList<>();
      if (mc.player == null || mc.level == null) {
         return found;
      }

      int r = this.range.getValue();
      int px = (int)Math.floor(mc.player.getX());
      int py = (int)Math.floor(mc.player.getY());
      int pz = (int)Math.floor(mc.player.getZ());

      int startX = px - r;
      int endX = px + r;
      int startY = Math.max(mc.level.getMinY() + 1, py - r);
      int endY = Math.min(mc.level.getMaxY(), py + r);
      int startZ = pz - r;
      int endZ = pz + r;

      for (int x = startX; x <= endX; x++) {
         for (int z = startZ; z <= endZ; z++) {
            if (!mc.level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
               continue;
            }
            for (int y = startY; y <= endY; y++) {
               BlockPos pos = new BlockPos(x, y, z);
               BlockState bs = mc.level.getBlockState(pos);
               if (this.shouldAdd(bs.getBlock(), pos)) {
                  found.add(new BlockESP.BlockVec(pos.getX(), pos.getY(), pos.getZ()));
               }
            }
         }
      }

      return found;
   }

   private void sync(ArrayList<BlockESP.BlockVec> b) {
      if (b != null) {
         blocks = b;
      }
      this.canContinue = true;
   }

   @Override
   public void onRender3D(PoseStack stack) {
      if (!fullNullCheck() && !blocks.isEmpty()) {
         int count = 0;
         if (mc.getFps() < 8 && mc.player.tickCount > 100) {
            this.disable("Saving ur pc :)");
         } else {
            if (this.fill.getValue() || this.outline.getValue()) {
               List<BlockESP.BlockVec> currentBlocks = new ArrayList<>(blocks);
               for (BlockESP.BlockVec vec : currentBlocks) {
                  if (count <= this.limitCount.getValue() || !this.limit.getValue().isEnabled()) {
                     if (vec.getDistance(mc.player.position()) <= this.range.getPow2Value()) {
                        AABB b = new AABB(vec.x, vec.y, vec.z, vec.x + 1.0, vec.y + 1.0, vec.z + 1.0);
                        if (this.fill.getValue()) {
                           Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(b, this.color.getValue().getColorObject()));
                        }

                        if (this.outline.getValue()) {
                           Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(b, this.color.getValue().getColorObject(), 2.0F));
                        }

                        if (this.tracers.getValue() && mc.getCameraEntity() != null) {
                           Vec3 vec2 = new Vec3(0.0, 0.0, 75.0)
                              .xRot(-((float)Math.toRadians(mc.gameRenderer.getMainCamera().xRot())))
                              .yRot(-((float)Math.toRadians(mc.gameRenderer.getMainCamera().yRot())))
                              .add(mc.getCameraEntity().getEyePosition());
                           Render3DEngine.drawLineDebug(vec2, vec.getVector(), this.color.getValue().getColorObject());
                        }

                        count++;
                     }
                  }
               }
            }

            this.lastFrameTime = System.currentTimeMillis();
         }
      }
   }

   private boolean shouldAdd(Block block, BlockPos pos) {
      if (block instanceof AirBlock) {
         return false;
      } else if (this.selectedBlocks.getValue().contains(block)) {
         return true;
      } else {
         return this.illegals.getValue() ? this.isIllegal(block, pos) : false;
      }
   }

   private boolean isIllegal(Block block, BlockPos pos) {
      if (block instanceof CommandBlock || block instanceof BarrierBlock) {
         return true;
      } else if (block != Blocks.BEDROCK) {
         return false;
      } else {
         return !PlayerUtility.isInHell() ? pos.getY() > 4 : pos.getY() > 127 || pos.getY() < 123 && pos.getY() > 4;
      }
   }

   public record BlockVec(double x, double y, double z) {
      public double getDistance(@NotNull Vec3 v) {
         double dx = this.x - v.x;
         double dy = this.y - v.y;
         double dz = this.z - v.z;
         return dx * dx + dy * dy + dz * dz;
      }

      public Vec3 getVector() {
         return new Vec3(this.x + 0.5, this.y + 0.5, this.z + 0.5);
      }
   }
}
