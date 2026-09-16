package vcore.utility.render.chunk;

import java.util.HashMap;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.ChunkAnimation;
import vcore.utility.render.chunk.impl.Cubic;
import vcore.utility.render.chunk.impl.Linear;
import vcore.utility.render.chunk.impl.Quad;
import vcore.utility.render.chunk.impl.Quart;
import vcore.utility.render.chunk.impl.Quint;

public class ChunkAnimations {
   public static final ChunkAnimations INSTANCE = new ChunkAnimations();
   public int mode;
   private final Minecraft mc = Minecraft.getInstance();
   private final WeakHashMap<RenderSection, BlockPos> chunkOrigins = new WeakHashMap<>();
   private final HashMap<Long, ChunkAnimations.AnimationData> timeStamps = new HashMap<>();

   public Vec3 getOffset(RenderSection chunk) {
      return chunk == null ? Vec3.ZERO : this.getOffset(chunk.getOrigin());
   }

   public Vec3 getOffset(BlockPos origin) {
      if (origin == null) {
         return Vec3.ZERO;
      }

      ChunkAnimation chunkAnimation = ModuleManager.chunkAnimation;
      if (chunkAnimation != null && chunkAnimation.isEnabled()) {
         long key = origin.asLong();
         ChunkAnimations.AnimationData animationData = this.timeStamps.get(key);
         if (animationData == null) {
            return Vec3.ZERO;
         }

         int animationDuration = chunkAnimation.time.getValue();
         this.mode = chunkAnimation.modes.getValue().getIndex();
         long time = animationData.timeStamp;
         if (time == -1L) {
            animationData.timeStamp = time = System.currentTimeMillis();
            if (this.mode == 4) {
               animationData.chunkFacing = this.mc.player != null
                  ? this.getChunkFacing(this.getZeroedPlayerPos(this.mc.player).subtract(this.getZeroedCenteredChunkPos(origin)))
                  : Direction.NORTH;
            }
         }

         long timeDif = System.currentTimeMillis() - time;
         if (timeDif < animationDuration) {
            int chunkY = origin.getY();
            int animationMode = this.mode == 2 ? (chunkY < this.getVoidFogHeight() ? 0 : 1) : (this.mode == 4 ? 3 : this.mode);
            switch (animationMode) {
               case 0: {
                  double dy = -chunkY + this.getFunctionValue((float)timeDif, 0.0F, chunkY, animationDuration);
                  return new Vec3(0.0, dy, 0.0);
               }
               case 1: {
                  double dy = 256 - chunkY - this.getFunctionValue((float)timeDif, 0.0F, 256 - chunkY, animationDuration);
                  return new Vec3(0.0, dy, 0.0);
               }
               case 2:
               default:
                  break;
               case 3:
                  Direction chunkFacing = animationData.chunkFacing;
                  if (chunkFacing != null) {
                     Vec3i vec = chunkFacing.getNormal();
                     double mod = -(200.0F - this.getFunctionValue((float)timeDif, 0.0F, 200.0F, animationDuration));
                     return new Vec3(vec.getX() * mod, 0.0, vec.getZ() * mod);
                  }
            }
         } else {
            this.timeStamps.remove(key);
         }

         return Vec3.ZERO;
      } else {
         return Vec3.ZERO;
      }
   }

   public void setPosition(RenderSection renderChunk, BlockPos position) {
      if (this.mc.player != null) {
         BlockPos immutablePos = position.immutable();
         BlockPos previousOrigin = this.chunkOrigins.put(renderChunk, immutablePos);
         if (previousOrigin != null && !previousOrigin.equals(immutablePos)) {
            this.timeStamps.remove(previousOrigin.asLong());
         }

         this.setPosition(immutablePos);
      }
   }

   public void setPosition(BlockPos position) {
      if (this.mc.player != null && position != null) {
         BlockPos immutablePos = position.immutable();
         BlockPos zeroedPlayerPos = this.getZeroedPlayerPos(this.mc.player);
         BlockPos zeroedCenteredChunkPos = this.getZeroedCenteredChunkPos(immutablePos);
         double distSq = zeroedPlayerPos.distToLowCornerSqr(zeroedCenteredChunkPos.getX(), zeroedCenteredChunkPos.getY(), zeroedCenteredChunkPos.getZ());
         long key = immutablePos.asLong();
         if (distSq > 1024.0) {
            this.timeStamps
               .put(key, new ChunkAnimations.AnimationData(-1L, this.mode == 3 ? this.getChunkFacing(zeroedPlayerPos.subtract(zeroedCenteredChunkPos)) : null));
         } else {
            this.timeStamps.remove(key);
         }
      }
   }

   private BlockPos getZeroedPlayerPos(LocalPlayer player) {
      BlockPos playerPos = player.blockPosition();
      return playerPos.offset(0, -playerPos.getY(), 0);
   }

   private BlockPos getZeroedCenteredChunkPos(BlockPos position) {
      return position.offset(8, -position.getY(), 8);
   }

   private Direction getChunkFacing(Vec3i dif) {
      int difX = Math.abs(dif.getX());
      int difZ = Math.abs(dif.getZ());
      return difX > difZ ? (dif.getX() > 0 ? Direction.EAST : Direction.WEST) : (dif.getZ() > 0 ? Direction.SOUTH : Direction.NORTH);
   }

   private float getFunctionValue(float t, float b, float c, float d) {
      switch (this.mode) {
         case 0:
            return Linear.easeOut(t, b, c, d);
         case 1:
            return Quad.easeOut(t, b, c, d);
         case 2:
            return Cubic.easeOut(t, b, c, d);
         case 3:
            return Quart.easeOut(t, b, c, d);
         case 4:
            return Quint.easeOut(t, b, c, d);
         default:
            return Linear.easeOut(t, b, c, d);
      }
   }

   private int getVoidFogHeight() {
      return this.mc.level == null ? 0 : this.mc.level.getMinBuildHeight() + 64;
   }

   public void clear() {
      this.chunkOrigins.clear();
      this.timeStamps.clear();
   }

   private static class AnimationData {
      public long timeStamp;
      public Direction chunkFacing;

      public AnimationData(long timeStamp, Direction chunkFacing) {
         this.timeStamp = timeStamp;
         this.chunkFacing = chunkFacing;
      }
   }
}
