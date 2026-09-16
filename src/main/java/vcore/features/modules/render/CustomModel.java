package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;
import vcore.core.Managers;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class CustomModel extends Module {
   private final Setting<CustomModel.Mode> mode = new Setting<>("Mode", CustomModel.Mode.Rabbit);
   private final Setting<Boolean> self = new Setting<>("Self", true);
   private final Setting<Boolean> friends = new Setting<>("Friends", true);
   private final Setting<Boolean> others = new Setting<>("Others", true);
   private static final float RABBIT_HEIGHT_FACTOR = 0.76F;
   private static final float VANILLA_HEIGHT_FACTOR = 0.96F;
   private static final float VANILLA_MODEL_HEIGHT = 16.0F;
   private static final float RABBIT_MODEL_HEIGHT = 12.666667F;
   private static final float RABBIT_VERTICAL_OFFSET = 0.7F;
   private static final ResourceLocation RABBIT_TEXTURE = ResourceLocation.fromNamespaceAndPath("vcore", "textures/custommodel/rabbit.png");
   private static final ResourceLocation FREDDY_TEXTURE = ResourceLocation.fromNamespaceAndPath("vcore", "textures/custommodel/freddy.png");
   private final CustomModel.RabbitModel rabbitModel = new CustomModel.RabbitModel();
   private final CustomModel.FreddyModel freddyModel = new CustomModel.FreddyModel();
   private final ModelPart rabbitCape = this.createCapePart(12.666667F);
   private AbstractClientPlayer renderingPlayer;

   public CustomModel() {
      super("CustomModel", "Renders selected custom model variants.", Module.Category.RENDER);
   }

   public ResourceLocation getTexture(AbstractClientPlayer player, ResourceLocation fallback) {
      if (!this.shouldApply(player)) {
         return fallback;
      }

      return switch ((CustomModel.Mode)this.mode.getValue()) {
         case Rabbit -> RABBIT_TEXTURE;
         case FreddyBear -> FREDDY_TEXTURE;
      };
   }

   public void beginPlayerRender(AbstractClientPlayer player) {
      this.renderingPlayer = player;
   }

   public void endPlayerRender(AbstractClientPlayer player) {
      if (this.renderingPlayer == player) {
         this.renderingPlayer = null;
      }
   }

   public AbstractClientPlayer getRenderingPlayer() {
      return this.renderingPlayer;
   }

   public boolean renderCustomPlayerModel(
      AbstractClientPlayer player,
      PlayerModel<?> vanillaModel,
      PoseStack matrices,
      VertexConsumer vertexConsumer,
      int light,
      int overlay,
      int color
   ) {
      if (!this.shouldApply(player)) {
         return false;
      }

      switch ((CustomModel.Mode)this.mode.getValue()) {
         case Rabbit:
            this.renderRabbit(vanillaModel, matrices, vertexConsumer, light, overlay, color);
            break;
         case FreddyBear:
            this.renderFreddy(vanillaModel, matrices, vertexConsumer, light, overlay, color);
      }

      return true;
   }

   public boolean renderCape(PoseStack matrices, MultiBufferSource vertexConsumers, int light, AbstractClientPlayer player, float tickDelta) {
      if (!this.shouldRenderRabbitCape(player)) {
         return false;
      }

      PlayerSkin skinTextures = player.getSkin();
      ResourceLocation capeTexture = skinTextures.capeTexture();
      if (capeTexture == null) {
         return false;
      }

      matrices.pushPose();
      matrices.translate(0.0F, 0.0F, 0.125F);
      matrices.translate(0.0F, 0.7F, 0.0F);
      this.applyVanillaCapeRotation(matrices, player, tickDelta);
      VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderType.entityCutout(capeTexture));
      this.rabbitCape.render(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
      matrices.popPose();
      return true;
   }

   public boolean shouldHideArmorSlotRender(LivingEntity entity) {
      return entity instanceof AbstractClientPlayer player && this.shouldApply(player);
   }

   public boolean shouldTransformHeldItem(LivingEntity entity) {
      return entity instanceof AbstractClientPlayer player && this.shouldApply(player);
   }

   public void applyHeldItemRootTransform(PoseStack matrices, LivingEntity entity) {
      if (this.shouldTransformHeldItem(entity)) {
         switch ((CustomModel.Mode)this.mode.getValue()) {
            case Rabbit:
               matrices.translate(0.0, 0.3, 0.0);
               matrices.scale(0.8F, 0.8F, 0.8F);
               break;
            case FreddyBear:
               matrices.translate(0.07, -0.3, 0.06);
               matrices.scale(0.79F, 0.9F, 1.15F);
         }
      }
   }

   private void renderRabbit(PlayerModel<?> vanillaModel, PoseStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
      this.copyRotation(this.rabbitModel.head, vanillaModel.head);
      this.copyRotation(this.rabbitModel.leftArm, vanillaModel.leftArm);
      this.copyRotation(this.rabbitModel.rightArm, vanillaModel.rightArm);
      this.copyRotation(this.rabbitModel.leftLeg, vanillaModel.leftLeg);
      this.copyRotation(this.rabbitModel.rightLeg, vanillaModel.rightLeg);
      matrices.pushPose();
      this.rabbitModel.root.render(matrices, vertexConsumer, light, overlay, color);
      matrices.popPose();
   }

   private void renderFreddy(PlayerModel<?> vanillaModel, PoseStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
      this.copyRotation(this.freddyModel.head, vanillaModel.head);
      this.copyRotation(this.freddyModel.leftArm, vanillaModel.leftArm);
      this.copyRotation(this.freddyModel.rightArm, vanillaModel.rightArm);
      this.copyRotation(this.freddyModel.leftLeg, vanillaModel.leftLeg);
      this.copyRotation(this.freddyModel.rightLeg, vanillaModel.rightLeg);
      matrices.pushPose();
      matrices.scale(0.75F, 0.65F, 0.75F);
      matrices.translate(0.0, 0.85F, 0.0);
      this.freddyModel.root.render(matrices, vertexConsumer, light, overlay, color);
      matrices.popPose();
   }

   private void copyRotation(ModelPart target, ModelPart source) {
      target.xRot = source.xRot;
      target.yRot = source.yRot;
      target.zRot = source.zRot;
   }

   private boolean shouldRenderRabbitCape(AbstractClientPlayer player) {
      if (this.mode.getValue() != CustomModel.Mode.Rabbit || !this.shouldApply(player)) {
         return false;
      } else {
         return !player.isInvisible() && player.isModelPartShown(PlayerModelPart.CAPE) ? !player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) : false;
      }
   }

   private boolean shouldApply(AbstractClientPlayer player) {
      if (fullNullCheck() || player == null || !this.isEnabled()) {
         return false;
      } else if (player == mc.player) {
         return this.self.getValue();
      } else {
         return Managers.FRIEND.isFriend(player) ? this.friends.getValue() : this.others.getValue();
      }
   }

   private void applyVanillaCapeRotation(PoseStack matrices, AbstractClientPlayer player, float tickDelta) {
      double capeX = Mth.lerp(tickDelta, player.xCloakO, player.xCloak) - Mth.lerp(tickDelta, player.xo, player.getX());
      double capeY = Mth.lerp(tickDelta, player.yCloakO, player.yCloak) - Mth.lerp(tickDelta, player.yo, player.getY());
      double capeZ = Mth.lerp(tickDelta, player.zCloakO, player.zCloak) - Mth.lerp(tickDelta, player.zo, player.getZ());
      float bodyYaw = Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot);
      double sinYaw = Mth.sin(bodyYaw * (float) (Math.PI / 180.0));
      double negCosYaw = -Mth.cos(bodyYaw * (float) (Math.PI / 180.0));
      float verticalSwing = (float)capeY * 10.0F;
      verticalSwing = Mth.clamp(verticalSwing, -6.0F, 32.0F);
      float backwardSwing = (float)(capeX * sinYaw + capeZ * negCosYaw) * 100.0F;
      backwardSwing = Mth.clamp(backwardSwing, 0.0F, 150.0F);
      float sideSwing = (float)(capeX * negCosYaw - capeZ * sinYaw) * 100.0F;
      sideSwing = Mth.clamp(sideSwing, -20.0F, 20.0F);
      if (backwardSwing < 0.0F) {
         backwardSwing = 0.0F;
      }

      float stride = Mth.lerp(tickDelta, player.oBob, player.bob);
      verticalSwing += Mth.sin(Mth.lerp(tickDelta, player.walkDistO, player.walkDist) * 6.0F) * 32.0F * stride;
      matrices.mulPose(Axis.XP.rotationDegrees(6.0F + backwardSwing / 2.0F + verticalSwing));
      matrices.mulPose(Axis.ZP.rotationDegrees(sideSwing / 2.0F));
      matrices.mulPose(Axis.YP.rotationDegrees(180.0F - sideSwing / 2.0F));
   }

   private ModelPart createCapePart(float height) {
      MeshDefinition modelData = new MeshDefinition();
      modelData.getRoot()
         .addOrReplaceChild(
            "cape", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -1.0F, 10.0F, height, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, 0.0F)
         );
      return LayerDefinition.create(modelData, 64, 32).bakeRoot().getChild("cape");
   }

   private static CubeListBuilder box(int textureX, int textureY, float x, float y, float z, float width, float height, float depth) {
      return CubeListBuilder.create().texOffs(textureX, textureY).addBox(x, y, z, width, height, depth, CubeDeformation.NONE);
   }

   private static final class FreddyModel {
      private final ModelPart root = createRoot();
      private final ModelPart head = this.root.getChild("fredhead");
      private final ModelPart leftArm = this.root.getChild("armLeft");
      private final ModelPart rightArm = this.root.getChild("armRight");
      private final ModelPart leftLeg = this.root.getChild("legLeft");
      private final ModelPart rightLeg = this.root.getChild("legRight");

      private static ModelPart createRoot() {
         MeshDefinition modelData = new MeshDefinition();
         PartDefinition root = modelData.getRoot();
         PartDefinition fredbody = root.addOrReplaceChild(
            "fredbody", CustomModel.box(0, 0, -1.0F, -14.0F, -1.0F, 2.0F, 24.0F, 2.0F), PartPose.offset(0.0F, -9.0F, 0.0F)
         );
         fredbody.addOrReplaceChild(
            "torso", CustomModel.box(8, 0, -6.0F, -9.0F, -4.0F, 12.0F, 18.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) (Math.PI / 180.0), 0.0F, 0.0F)
         );
         fredbody.addOrReplaceChild("crotch", CustomModel.box(56, 0, -5.5F, 0.0F, -3.5F, 11.0F, 3.0F, 7.0F), PartPose.offset(0.0F, 9.5F, 0.0F));
         PartDefinition armRight = fredbody.addOrReplaceChild(
            "armRight",
            CustomModel.box(48, 0, -1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
            PartPose.offsetAndRotation(-6.5F, -8.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 12))
         );
         armRight.addOrReplaceChild("armRightpad", CustomModel.box(70, 10, -2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
         PartDefinition armRight2 = armRight.addOrReplaceChild(
            "armRight2",
            CustomModel.box(90, 20, -1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, (float) (-Math.PI / 18), 0.0F, 0.0F)
         );
         armRight2.addOrReplaceChild("armRightpad2", CustomModel.box(0, 26, -2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
         armRight2.addOrReplaceChild(
            "handRight", CustomModel.box(20, 26, -2.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, -0.05235988F)
         );
         PartDefinition armLeft = fredbody.addOrReplaceChild(
            "armLeft",
            CustomModel.box(62, 10, -1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
            PartPose.offsetAndRotation(6.5F, -8.0F, 0.0F, 0.0F, 0.0F, (float) (-Math.PI / 12))
         );
         armLeft.addOrReplaceChild("armLeftpad", CustomModel.box(38, 54, -2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
         PartDefinition armLeft2 = armLeft.addOrReplaceChild(
            "armLeft2", CustomModel.box(90, 48, -1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, (float) (-Math.PI / 18), 0.0F, 0.0F)
         );
         armLeft2.addOrReplaceChild("armLeftpad2", CustomModel.box(0, 58, -2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
         armLeft2.addOrReplaceChild(
            "handLeft", CustomModel.box(58, 56, -1.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.05235988F)
         );
         PartDefinition legRight = fredbody.addOrReplaceChild(
            "legRight", CustomModel.box(90, 8, -1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), PartPose.offset(-3.3F, 12.5F, 0.0F)
         );
         legRight.addOrReplaceChild("legRightpad", CustomModel.box(73, 33, -3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
         PartDefinition legRight2 = legRight.addOrReplaceChild(
            "legRight2", CustomModel.box(20, 35, -1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, 0.034906585F, 0.0F, 0.0F)
         );
         legRight2.addOrReplaceChild("legRightpad2", CustomModel.box(0, 39, -2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
         legRight2.addOrReplaceChild(
            "footRight", CustomModel.box(22, 39, -2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, -0.034906585F, 0.0F, 0.0F)
         );
         PartDefinition legLeft = fredbody.addOrReplaceChild(
            "legLeft", CustomModel.box(54, 10, -1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), PartPose.offset(3.3F, 12.5F, 0.0F)
         );
         legLeft.addOrReplaceChild("legLeftpad", CustomModel.box(48, 39, -3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
         PartDefinition legLeft2 = legLeft.addOrReplaceChild(
            "legLeft2", CustomModel.box(72, 48, -1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, 0.034906585F, 0.0F, 0.0F)
         );
         legLeft2.addOrReplaceChild("legLeftpad2", CustomModel.box(16, 50, -2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
         legLeft2.addOrReplaceChild(
            "footLeft", CustomModel.box(72, 50, -2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, -0.034906585F, 0.0F, 0.0F)
         );
         PartDefinition fredhead = fredbody.addOrReplaceChild(
            "fredhead", CustomModel.box(39, 22, -5.5F, -8.0F, -4.5F, 11.0F, 8.0F, 9.0F), PartPose.offset(0.0F, -13.0F, -0.5F)
         );
         fredhead.addOrReplaceChild("frednose", CustomModel.box(17, 67, -4.0F, -2.0F, -3.0F, 8.0F, 4.0F, 3.0F), PartPose.offset(0.0F, -2.0F, -4.5F));
         fredhead.addOrReplaceChild("jaw", CustomModel.box(49, 65, -5.0F, 0.0F, -4.5F, 10.0F, 3.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 0.08726646F, 0.0F, 0.0F));
         PartDefinition earRight = fredhead.addOrReplaceChild(
            "earRight",
            CustomModel.box(8, 0, -1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
            PartPose.offsetAndRotation(-4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, (float) (-Math.PI / 3))
         );
         earRight.addOrReplaceChild("earRightpad", CustomModel.box(85, 0, -2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
         PartDefinition earLeft = fredhead.addOrReplaceChild(
            "earLeft",
            CustomModel.box(40, 0, -1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
            PartPose.offsetAndRotation(4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, (float) (Math.PI / 3))
         );
         earLeft.addOrReplaceChild("earRightpad_1", CustomModel.box(40, 39, -2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
         PartDefinition hat = fredhead.addOrReplaceChild(
            "hat", CustomModel.box(70, 24, -3.0F, -0.5F, -3.0F, 6.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -8.4F, 0.0F, (float) (-Math.PI / 180.0), 0.0F, 0.0F)
         );
         hat.addOrReplaceChild(
            "hat2", CustomModel.box(78, 61, -2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.1F, 0.0F, (float) (-Math.PI / 180.0), 0.0F, 0.0F)
         );
         return LayerDefinition.create(modelData, 100, 80).bakeRoot().getChild("fredbody");
      }
   }

   private enum Mode {
      Rabbit("Rabbit"),
      FreddyBear("Freddy Bear");

      private final String name;

      Mode(String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   private static final class RabbitModel {
      private final ModelPart root = createRoot();
      private final ModelPart head = this.root.getChild("rabbitHead");
      private final ModelPart leftArm = this.root.getChild("rabbitLarm");
      private final ModelPart rightArm = this.root.getChild("rabbitRarm");
      private final ModelPart leftLeg = this.root.getChild("rabbitLleg");
      private final ModelPart rightLeg = this.root.getChild("rabbitRleg");

      private static ModelPart createRoot() {
         MeshDefinition modelData = new MeshDefinition();
         PartDefinition root = modelData.getRoot();
         PartDefinition rabbitBone = root.addOrReplaceChild(
            "rabbitBone", CustomModel.box(28, 45, -5.0F, -13.0F, -5.0F, 10.0F, 11.0F, 8.0F), PartPose.offset(0.0F, 24.0F, 0.0F)
         );
         rabbitBone.addOrReplaceChild("rabbitRleg", CustomModel.box(0, 0, -2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.offset(-3.0F, -2.0F, -1.0F));
         rabbitBone.addOrReplaceChild(
            "rabbitLarm", CustomModel.box(0, 0, 0.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F), PartPose.offsetAndRotation(5.0F, -13.0F, -1.0F, 0.0F, 0.0F, -0.0873F)
         );
         rabbitBone.addOrReplaceChild(
            "rabbitRarm", CustomModel.box(0, 0, -2.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F), PartPose.offsetAndRotation(-5.0F, -13.0F, -1.0F, 0.0F, 0.0F, 0.0873F)
         );
         rabbitBone.addOrReplaceChild("rabbitLleg", CustomModel.box(0, 0, -2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.offset(3.0F, -2.0F, -1.0F));
         rabbitBone.addOrReplaceChild(
            "rabbitHead",
            CubeListBuilder.create()
               .texOffs(0, 0)
               .addBox(-3.0F, 0.0F, -4.0F, 6.0F, 1.0F, 6.0F, CubeDeformation.NONE)
               .texOffs(56, 0)
               .addBox(-5.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F, CubeDeformation.NONE)
               .texOffs(56, 0)
               .addBox(3.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F, CubeDeformation.NONE)
               .texOffs(0, 45)
               .addBox(-4.0F, -11.0F, -4.0F, 8.0F, 11.0F, 8.0F, CubeDeformation.NONE)
               .texOffs(46, 0)
               .addBox(1.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F, CubeDeformation.NONE)
               .texOffs(46, 0)
               .addBox(-4.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, -14.0F, -1.0F)
         );
         return LayerDefinition.create(modelData, 64, 64).bakeRoot().getChild("rabbitBone");
      }
   }
}
