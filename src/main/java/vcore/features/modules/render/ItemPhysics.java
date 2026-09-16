package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import vcore.features.modules.Module;

public class ItemPhysics extends Module {
   private static final float PIXEL_SIZE = 0.0625F;
   private static final float ROTATE_SPEED_Y = 0.5F;
   private static final float ROTATE_SPEED_X = 0.5F;
   private static final float STACK_GROUND_OFFSET = 0.015F;
   private static final float STACK_GROUND_XZ_OFFSET = 0.015F;
   private final Random random = new Random();
   private static final Map<UUID, Float> rotationMapY = new HashMap<>();
   private static final Map<UUID, Float> rotationMapX = new HashMap<>();
   private static final Map<UUID, Float> groundYawMap = new HashMap<>();

   public ItemPhysics() {
      super("ItemPhysics", "Realistic item physics.", Module.Category.RENDER);
   }

   public void onRenderItemEntity(
      ItemEntity entity, PoseStack matrices, MultiBufferSource vertexConsumers, int light, float tickDelta, List<BakedQuad> quads
   ) {
      if (quads != null) {
         ItemPhysics.ModelInfo info = this.getInfo(quads);
         matrices.pushPose();
         this.offsetInFluidOrBlock(matrices, entity, info);
         boolean isFlat = info.flat;
         boolean isBlock = !isFlat;
         Minecraft mc = Minecraft.getInstance();
         ItemStack stack = entity.getItem();
         BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, null, entity.getId());
         ItemPhysics.RotationPair rotation = this.getRotationPair(entity, tickDelta, isBlock);
         double centerOffsetX = (entity.getBoundingBox().maxX + entity.getBoundingBox().minX) / 2.0 - entity.getX();
         double centerOffsetY = (entity.getBoundingBox().maxY + entity.getBoundingBox().minY) / 2.0 - entity.getY();
         double centerOffsetZ = (entity.getBoundingBox().maxZ + entity.getBoundingBox().minZ) / 2.0 - entity.getZ();
         matrices.translate(centerOffsetX, centerOffsetY, centerOffsetZ);
         if (entity.onGround() && !entity.isInWater() && !entity.isInLava()) {
            if (isFlat) {
               matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
               float groundYaw = groundYawMap.getOrDefault(entity.getUUID(), 0.0F);
               matrices.mulPose(Axis.ZP.rotationDegrees(groundYaw));
            } else {
               matrices.mulPose(Axis.XP.rotationDegrees(0.0F));
               matrices.mulPose(Axis.YP.rotationDegrees(0.0F));
            }
         } else if (isFlat) {
            matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
            if (!entity.isInWater() && !entity.isInLava()) {
               matrices.mulPose(Axis.ZP.rotationDegrees(rotation.rotZ));
               matrices.mulPose(Axis.YP.rotationDegrees(rotation.rotY));
            }
         } else {
            matrices.mulPose(Axis.XP.rotationDegrees(rotation.rotX));
            matrices.mulPose(Axis.YP.rotationDegrees(rotation.rotY));
         }

         matrices.translate(-centerOffsetX, -centerOffsetY, -centerOffsetZ);
         int count = stack.getCount();
         int renderedAmount = count > 48 ? 5 : (count > 32 ? 4 : (count > 16 ? 3 : (count > 1 ? 2 : 1)));

         for (int j = 0; j < renderedAmount; j++) {
            matrices.pushPose();
            this.random.setSeed(entity.getUUID().getMostSignificantBits() + j);
            if ((entity.onGround() && !entity.isInWater() && !entity.isInLava() || entity.isInWater() || entity.isInLava()) && j > 0) {
               float x = (this.random.nextFloat() * 2.0F - 1.0F) * 0.015F;
               float z = (this.random.nextFloat() * 2.0F - 1.0F) * 0.015F;
               float y = j * 0.015F;
               matrices.translate(x, y, z);
            }

            mc.getItemRenderer().render(stack, ItemDisplayContext.GROUND, false, matrices, vertexConsumers, light, OverlayTexture.NO_OVERLAY, model);
            matrices.popPose();
         }

         matrices.popPose();
      }
   }

   private ItemPhysics.RotationPair getRotationPair(ItemEntity entity, float tickDelta, boolean isBlock) {
      UUID id = entity.getUUID();
      float rotY = rotationMapY.getOrDefault(id, 0.0F);
      float rotX = rotationMapX.getOrDefault(id, 0.0F);
      float rotZ = rotY;
      boolean inWater = entity.isInWater() || entity.isInLava();
      boolean onGround = entity.onGround();
      if (!inWater && onGround) {
         groundYawMap.computeIfAbsent(id, uuid -> {
            Random r = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
            return r.nextFloat() * 360.0F;
         });
         float groundYaw = groundYawMap.get(id);
         rotationMapY.put(id, 0.0F);
         rotationMapX.put(id, 0.0F);
         return new ItemPhysics.RotationPair(groundYaw, 0.0F, groundYaw);
      } else if (inWater && !isBlock) {
         rotationMapY.put(id, 0.0F);
         rotationMapX.put(id, 0.0F);
         return new ItemPhysics.RotationPair(0.0F, 0.0F, 0.0F);
      } else {
         float speedY = this.getRotateByY(tickDelta);
         float speedX = this.getRotateByX(tickDelta);
         rotY += speedY * 2.0F;
         rotX += speedX * 2.3F;
         rotZ += speedY * 2.0F;
         rotationMapY.put(id, rotY);
         rotationMapX.put(id, rotX);
         return new ItemPhysics.RotationPair(rotY, rotX, rotZ);
      }
   }

   private float getRotateByY(float tickDelta) {
      return tickDelta * 3.0F * 0.5F;
   }

   private float getRotateByX(float tickDelta) {
      return tickDelta * 3.0F * 0.5F;
   }

   private void offsetInFluidOrBlock(PoseStack matrices, ItemEntity entity, ItemPhysics.ModelInfo info) {
      if (entity.isInWater()) {
         if (!info.flat) {
            matrices.translate(0.0F, 0.025F, 0.0F);
         }
      } else if (info.flat && !entity.onGround()) {
         matrices.translate(0.0F, info.offsetY, 0.0F);
      }
   }

   private ItemPhysics.ModelInfo getInfo(List<BakedQuad> quads) {
      float minX = Float.MAX_VALUE;
      float maxX = Float.MIN_VALUE;
      float minY = Float.MAX_VALUE;
      float maxY = Float.MIN_VALUE;
      float minZ = Float.MAX_VALUE;
      float maxZ = Float.MIN_VALUE;

      for (BakedQuad _quad : quads) {
         for (int i = 0; i < 4; i++) {
            Direction face = _quad.getDirection();
            float vx = Float.intBitsToFloat(_quad.getVertices()[i * 8]);
            float vy = Float.intBitsToFloat(_quad.getVertices()[i * 8 + 1]);
            float vz = Float.intBitsToFloat(_quad.getVertices()[i * 8 + 2]);
            switch (face) {
               case DOWN:
                  minY = Math.min(minY, vy);
                  break;
               case UP:
                  maxY = Math.max(maxY, vy);
                  break;
               case NORTH:
                  minZ = Math.min(minZ, vz);
                  break;
               case SOUTH:
                  maxZ = Math.max(maxZ, vz);
                  break;
               case WEST:
                  minX = Math.min(minX, vx);
                  break;
               case EAST:
                  maxX = Math.max(maxX, vx);
            }
         }
      }

      if (minX == Float.MAX_VALUE) {
         minX = 0.0F;
      }

      if (minY == Float.MAX_VALUE) {
         minY = 0.0F;
      }

      if (minZ == Float.MAX_VALUE) {
         minZ = 0.0F;
      }

      if (maxX == Float.MIN_VALUE) {
         maxX = 1.0F;
      }

      if (maxY == Float.MIN_VALUE) {
         maxY = 1.0F;
      }

      if (maxZ == Float.MIN_VALUE) {
         maxZ = 1.0F;
      }

      float x = maxX - minX;
      float y = maxY - minY;
      float z = maxZ - minZ;
      boolean flat = x > 0.0625F && y > 0.0625F && z <= 0.0625F;
      float offsetY = flat ? 0.5F - minY : 0.0F;
      return new ItemPhysics.ModelInfo(flat, offsetY, minZ - minY);
   }

   private record ModelInfo(boolean flat, float offsetY, float offsetZ) {
   }

   private record RotationPair(float rotY, float rotX, float rotZ) {
   }
}
