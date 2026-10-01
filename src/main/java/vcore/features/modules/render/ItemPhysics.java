package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import vcore.features.modules.Module;
import vcore.utility.interfaces.IItemEntityRenderState;

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
      ItemEntityRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState camera
   ) {
      if (state.item.isEmpty()) {
         return;
      }

      AABB aabb = state.item.getModelBoundingBox();
      boolean isFlat = aabb.getZsize() <= 0.0625F;
      boolean isBlock = !isFlat;

      UUID uuid;
      boolean onGround;
      boolean inWater;
      boolean inLava;
      float tickDelta;

      if (state instanceof IItemEntityRenderState accessor && accessor.vcore$getUUID() != null) {
         uuid = accessor.vcore$getUUID();
         onGround = accessor.vcore$isOnGround();
         inWater = accessor.vcore$isInWater();
         inLava = accessor.vcore$isInLava();
         tickDelta = accessor.vcore$getTickDelta();
      } else {
         uuid = new UUID(state.seed, state.seed);
         onGround = false;
         inWater = false;
         inLava = false;
         tickDelta = 1.0F;
      }

      matrices.pushPose();

      if (inWater) {
         if (isBlock) {
            matrices.translate(0.0F, 0.025F, 0.0F);
         }
      } else if (isFlat && !onGround) {
         float offsetY = 0.5F - (float) aabb.minY;
         matrices.translate(0.0F, offsetY, 0.0F);
      }

      ItemPhysics.RotationPair rotation = this.getRotationPair(uuid, onGround, inWater, inLava, tickDelta, isBlock);
      double centerOffsetY = state.boundingBoxHeight / 2.0;
      matrices.translate(0.0, centerOffsetY, 0.0);

      if (onGround && !inWater && !inLava) {
         if (isFlat) {
            matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
            float groundYaw = groundYawMap.getOrDefault(uuid, 0.0F);
            matrices.mulPose(Axis.ZP.rotationDegrees(groundYaw));
         } else {
            matrices.mulPose(Axis.XP.rotationDegrees(0.0F));
            matrices.mulPose(Axis.YP.rotationDegrees(0.0F));
         }
      } else if (isFlat) {
         matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
         if (!inWater && !inLava) {
            matrices.mulPose(Axis.ZP.rotationDegrees(rotation.rotZ));
            matrices.mulPose(Axis.YP.rotationDegrees(rotation.rotY));
         }
      } else {
         matrices.mulPose(Axis.XP.rotationDegrees(rotation.rotX));
         matrices.mulPose(Axis.YP.rotationDegrees(rotation.rotY));
      }

      matrices.translate(0.0, -centerOffsetY, 0.0);

      int count = Math.max(1, state.count);
      for (int j = 0; j < count; j++) {
         matrices.pushPose();
         this.random.setSeed(uuid.getMostSignificantBits() + j);
         if ((onGround && !inWater && !inLava || inWater || inLava) && j > 0) {
            float x = (this.random.nextFloat() * 2.0F - 1.0F) * 0.015F;
            float z = (this.random.nextFloat() * 2.0F - 1.0F) * 0.015F;
            float y = j * 0.015F;
            matrices.translate(x, y, z);
         }

         state.item.submit(matrices, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
         matrices.popPose();
      }

      matrices.popPose();
   }

   private ItemPhysics.RotationPair getRotationPair(
      UUID id, boolean onGround, boolean inWater, boolean inLava, float tickDelta, boolean isBlock
   ) {
      float rotY = rotationMapY.getOrDefault(id, 0.0F);
      float rotX = rotationMapX.getOrDefault(id, 0.0F);
      float rotZ = rotY;
      boolean inFluid = inWater || inLava;
      if (!inFluid && onGround) {
         groundYawMap.computeIfAbsent(id, uuid -> {
            Random r = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
            return r.nextFloat() * 360.0F;
         });
         float groundYaw = groundYawMap.get(id);
         rotationMapY.put(id, 0.0F);
         rotationMapX.put(id, 0.0F);
         return new ItemPhysics.RotationPair(groundYaw, 0.0F, groundYaw);
      } else if (inFluid && !isBlock) {
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

   private record RotationPair(float rotY, float rotX, float rotZ) {
   }
}
