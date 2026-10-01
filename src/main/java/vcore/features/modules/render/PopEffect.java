package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.authlib.GameProfile;
import vcore.utility.render.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Axis;
import java.awt.Color;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import vcore.events.impl.TotemPopEvent;
import vcore.features.modules.Module;
import vcore.features.modules.render.popeffect.PopEffectParticles;
import vcore.injection.accesors.IEntity;
import vcore.injection.accesors.IModelPart;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public final class PopEffect extends Module {
   private static final int FULL_BRIGHT_LIGHT = 15728880;
   private static final double EFFECT_SCALER = 2.5;
   private static final int EFFECT_REVIVE_COUNT = 17;
   private static final int EFFECT_SPARK_COUNT = 99;
   private final Setting<PopEffect.Mode> mode = new Setting<>("Mode", PopEffect.Mode.ChamsOld);
   private final Setting<Integer> oldYSpeed = new Setting<>("YSpeed", 1, -10, 10, v -> this.isChamsOld());
   private final Setting<Integer> oldAlphaSpeed = new Setting<>("AlphaSpeed", 35, 1, 100, v -> this.isChamsOld());
   private final Setting<Float> oldRotationSpeed = new Setting<>("RotationSpeed", 0.0F, 0.0F, 6.0F, v -> this.isChamsOld());
   private final Setting<ColorSetting> oldColor = new Setting<>("Color", new ColorSetting(new Color(-1, true)), v -> this.isChamsOld());
   private final Setting<Integer> newLifeTime = new Setting<>("Fade Time", 25, 5, 100, v -> this.shouldShowNewFadeTime());
   private final Setting<Float> newDisperseSpeed = new Setting<>("Animation Speed", 1.5F, 0.0F, 10.0F, v -> this.shouldShowNewAnimationSetting()).step(0.5F);
   private final Setting<Float> newDisperseMaxDistance = new Setting<>("Animation Distance", 1.0F, 0.0F, 8.0F, v -> this.shouldShowNewAnimationSetting())
      .step(0.1F);
   private final Setting<Float> newWireframeThickness = new Setting<>("OutLine Thickness", 2.0F, 0.0F, 5.0F, v -> this.shouldShowNewOutlineSetting())
      .step(0.1F);
   private final Setting<Boolean> newFadeOut = new Setting<>("Fade", true, v -> this.isChamsNew());
   private final Setting<Boolean> newDisperse = new Setting<>("Animation", false, v -> this.isChamsNew());
   private final Setting<Boolean> newFilledModel = new Setting<>("Fill", true, v -> this.isChamsNew());
   private final Setting<Boolean> newWireframe = new Setting<>("OutLine", true, v -> this.isChamsNew());
   private final Setting<ColorSetting> newFilledColor = new Setting<>(
      "Fill Color", new ColorSetting(new Color(137, 0, 255, 150)), v -> this.isChamsNew() && this.newFilledModel.getValue()
   );
   private final Setting<ColorSetting> newWireframeColor = new Setting<>(
      "OutLine Color", new ColorSetting(new Color(100, 0, 255, 150)), v -> this.isChamsNew() && this.newWireframe.getValue()
   );
   private final Setting<Float> effectOpacity = new Setting<>("Opacity", 1.0F, 0.0F, 1.0F, v -> this.isEffect()).step(0.05F);
   private final CopyOnWriteArrayList<PopEffect.OldPerson> oldPopList = new CopyOnWriteArrayList<>();
   private final CopyOnWriteArrayList<PopEffect.NewPerson> newPopList = new CopyOnWriteArrayList<>();

   public PopEffect() {
      super("PopEffect", "Highlights totem pops.", Module.Category.RENDER);
   }

   @Override
   public void onUpdate() {
      this.oldPopList.forEach(person -> person.update(this.oldPopList));
      this.newPopList.forEach(person -> person.update(this.newPopList));
   }

   @Override
   public void onRender3D(PoseStack stack) {
      switch ((PopEffect.Mode)this.mode.getValue()) {
         case ChamsOld:
            this.renderChamsOld(stack);
            break;
         case ChamsNew:
            this.renderChamsNew(stack);
         case Effect:
      }
   }

   @Override
   public void onDisable() {
      this.clearEffects();
   }

   @Override
   public void onLogout() {
      this.clearEffects();
   }

   public boolean shouldHideVanillaTotemParticle() {
      return this.isEnabled() && this.isEffect();
   }

   public float getEffectOpacity() {
      return this.effectOpacity.getValue();
   }

   @EventHandler
   private void onTotemPop(@NotNull TotemPopEvent event) {
      if (mc.level != null && event.getEntity() != null) {
         switch ((PopEffect.Mode)this.mode.getValue()) {
            case ChamsOld:
               this.addChamsOld(event.getEntity());
            case ChamsNew:
            case Effect:
         }
      }
   }

   private void addChamsOld(@NotNull Player source) {
      if (!source.equals(mc.player)) {
         this.oldPopList.add(new PopEffect.OldPerson(this.createCapturedPlayer(source)));
      }
   }

   private void addChamsNew(@NotNull Player source) {
      if (!source.equals(mc.player)) {
         this.newPopList.add(new PopEffect.NewPerson(source));
      }
   }

   public void addChamsNewFromStatus(@NotNull Player source) {
      if (this.isEnabled() && this.isChamsNew()) {
         this.addChamsNew(source);
      }
   }

   public void addEffect(@NotNull Entity source) {
      if (this.isEnabled() && this.isEffect()) {
         double centerY = source.getY() + source.getDimensions(source.getPose()).height() / 2.0F;

         for (int i = 1; i < 18; i++) {
            source.level().addParticle(PopEffectParticles.REVIVE, source.getX(), centerY, source.getZ(), 2.5, source.getId(), 0.0);
         }

         for (int i = 1; i < 100; i++) {
            source.level().addParticle(PopEffectParticles.REVIVE_SPARK, source.getX(), centerY, source.getZ(), source.getId(), 0.0, 0.0);
         }
      }
   }

   private void renderChamsOld(@NotNull PoseStack matrices) {
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.defaultBlendFunc();
      this.oldPopList.forEach(person -> this.renderOldEntity(matrices, person.player, person.modelPlayer, person.getAlpha()));
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   }

   private void renderChamsNew(@NotNull PoseStack matrices) {
      if (!this.newPopList.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         float tickDelta = Render3DEngine.getTickDelta();
         this.newPopList.forEach(person -> this.renderChamsNewPerson(matrices, person, tickDelta));
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
      }
   }

   private void renderChamsNewPerson(@NotNull PoseStack matrices, @NotNull PopEffect.NewPerson person, float tickDelta) {
      float displacement = person.getDisplacement(tickDelta);
      float alpha = person.getAlpha(tickDelta, displacement);
      if (!(alpha <= 0.0F)) {
         if (this.newFilledModel.getValue()) {
            RenderSystem.enableCull();
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            this.renderNewFilledModel(matrices, buffer, person, alpha, displacement);
            Render2DEngine.endBuilding(buffer);
         }

         if (this.newWireframe.getValue()) {
            RenderSystem.disableCull();
            RenderSystem.lineWidth(this.newWireframeThickness.getValue());
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            this.renderNewWireframe(matrices, buffer, person, alpha, displacement);
            Render2DEngine.endBuilding(buffer);
            RenderSystem.lineWidth(1.0F);
         }
      }
   }

   private void renderOldEntity(@NotNull PoseStack matrices, @NotNull Player entity, @NotNull PlayerModel model, int alpha) {
      setOuterLayerVisible(model, false);
      double x = entity.getX() - mc.getEntityRenderDispatcher().camera.position().x;
      double y = entity.getY() - mc.getEntityRenderDispatcher().camera.position().y;
      double z = entity.getZ() - mc.getEntityRenderDispatcher().camera.position().z;
      ((IEntity)entity).setPos(entity.position().add(0.0, this.oldYSpeed.getValue().intValue() / 50.0, 0.0));
      matrices.pushPose();
      matrices.translate((float)x, (float)y, (float)z);
      float yRotYaw = alpha / 255.0F * 360.0F * this.oldRotationSpeed.getValue();
      yRotYaw = yRotYaw == 0.0F
         ? 0.0F
         : Render2DEngine.interpolateFloat(
            yRotYaw, yRotYaw - this.oldAlphaSpeed.getValue().intValue() / 255.0F * 360.0F * this.oldRotationSpeed.getValue(), Render3DEngine.getTickDelta()
         );
      matrices.mulPose(Axis.YP.rotation(MathUtility.rad(180.0F - entity.yBodyRot + yRotYaw)));
      preparePlayerScale(matrices);
      AvatarRenderState state = new AvatarRenderState();
      if (mc.getEntityRenderDispatcher().getRenderer(entity) instanceof AvatarRenderer renderer
         && entity instanceof AbstractClientPlayer clientPlayer) {
         renderer.extractRenderState(clientPlayer, state, Render3DEngine.getTickDelta());
      }
      model.setupAnim(state);
      BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
      RenderSystem.setShaderColor(
         this.oldColor.getValue().getGlRed(), this.oldColor.getValue().getGlGreen(), this.oldColor.getValue().getGlBlue(), alpha / 255.0F
      );
      model.renderToBuffer(matrices, buffer, 15728880, OverlayTexture.NO_OVERLAY, -1);
      Render2DEngine.endBuilding(buffer);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      matrices.popPose();
   }

   private void renderNewFilledModel(
      @NotNull PoseStack matrices, @NotNull BufferBuilder buffer, @NotNull PopEffect.NewPerson person, float alpha, float displacement
   ) {
      int color = this.withAlpha(this.newFilledColor.getValue(), alpha);
      this.setupChamsNewPlayerTransform(matrices, person);
      setOuterLayerVisible(person.modelPlayer, false);
      this.applyChamsNewFlyingTransforms(matrices, person);

      for (PopEffect.BodyPart part : PopEffect.BodyPart.values()) {
         Vec3 partDisplacement = this.getPartDisplacement(person.uuid, part, displacement);
         matrices.pushPose();
         matrices.translate(partDisplacement.x(), partDisplacement.y(), partDisplacement.z());
         this.renderModelPart(person.modelPlayer, part, matrices, buffer, color);
         matrices.popPose();
      }

      matrices.popPose();
   }

   private void renderNewWireframe(
      @NotNull PoseStack matrices, @NotNull BufferBuilder buffer, @NotNull PopEffect.NewPerson person, float alpha, float displacement
   ) {
      int color = this.withAlpha(this.newWireframeColor.getValue(), alpha);
      this.setupChamsNewPlayerTransform(matrices, person);
      setOuterLayerVisible(person.modelPlayer, false);
      this.applyChamsNewFlyingTransforms(matrices, person);

      for (PopEffect.BodyPart part : PopEffect.BodyPart.values()) {
         Vec3 partDisplacement = this.getPartDisplacement(person.uuid, part, displacement);
         matrices.pushPose();
         matrices.translate(partDisplacement.x(), partDisplacement.y(), partDisplacement.z());
         this.renderWireframePart(this.getModelPart(person.modelPlayer, part), matrices, buffer, color);
         matrices.popPose();
      }

      matrices.popPose();
   }

   @NotNull
   private Player createCapturedPlayer(@NotNull Player source) {
      Player captured = new RemotePlayer(mc.level, new GameProfile(source.getUUID(), source.getName().getString())) {
         public boolean isSpectator() {
            return false;
         }

         public boolean isCreative() {
            return false;
         }
      };
      captured.copyPosition(source);
      captured.yBodyRot = source.yBodyRot;
      captured.yBodyRotO = source.yBodyRotO;
      captured.yHeadRot = source.yHeadRot;
      captured.yHeadRotO = source.yHeadRotO;
      captured.attackAnim = source.attackAnim;
      captured.swingTime = source.swingTime;
      captured.setShiftKeyDown(source.isShiftKeyDown());
      captured.setPose(source.getPose());
      captured.walkAnimation.setSpeed(source.walkAnimation.speed());
      return captured;
   }

   @NotNull
   private Player createChamsNewCapturedPlayer(@NotNull Player source) {
      Player captured = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "Captured Player")) {
         public boolean isSpectator() {
            return false;
         }

         public boolean isCreative() {
            return false;
         }

         public boolean isPushable() {
            return false;
         }

         public boolean canBeCollidedWith() {
            return false;
         }

         public boolean canCollideWith(Entity other) {
            return false;
         }
      };
      captured.copyPosition(source);
      captured.xo = source.xo;
      captured.yo = source.yo;
      captured.zo = source.zo;
      captured.xOld = source.xOld;
      captured.yOld = source.yOld;
      captured.zOld = source.zOld;
      captured.yRotO = source.yRotO;
      captured.xRotO = source.xRotO;
      captured.yBodyRot = source.yBodyRot;
      captured.yBodyRotO = source.yBodyRotO;
      captured.yHeadRot = source.yHeadRot;
      captured.yHeadRotO = source.yHeadRotO;
      captured.attackAnim = source.attackAnim;
      captured.oAttackAnim = source.oAttackAnim;
      captured.swingTime = source.swingTime;
      captured.swinging = source.swinging;
      captured.setShiftKeyDown(source.isShiftKeyDown());
      captured.setPose(source.getPose());
      captured.walkAnimation.setSpeed(source.walkAnimation.speed());
      return captured;
   }

   private void setupCapturedPlayerTransform(@NotNull PoseStack matrices, @NotNull Player entity) {
      double x = entity.getX() - mc.getEntityRenderDispatcher().camera.position().x;
      double y = entity.getY() - mc.getEntityRenderDispatcher().camera.position().y;
      double z = entity.getZ() - mc.getEntityRenderDispatcher().camera.position().z;
      matrices.pushPose();
      matrices.translate((float)x, (float)y, (float)z);
      matrices.mulPose(Axis.YP.rotation(MathUtility.rad(180.0F - entity.yBodyRot)));
      preparePlayerScale(matrices);
   }

   private void setupChamsNewPlayerTransform(@NotNull PoseStack matrices, @NotNull PopEffect.NewPerson person) {
      double x = person.player.getX() - mc.getEntityRenderDispatcher().camera.position().x;
      double y = person.player.getY() - mc.getEntityRenderDispatcher().camera.position().y;
      double z = person.player.getZ() - mc.getEntityRenderDispatcher().camera.position().z;
      matrices.pushPose();
      matrices.translate((float)x, (float)y, (float)z);
      matrices.mulPose(Axis.YP.rotationDegrees(180.0F - person.player.yBodyRot));
      prepareChamsNewPlayerScale(matrices);
   }

   private void applyChamsNewFlyingTransforms(@NotNull PoseStack matrices, @NotNull PopEffect.NewPerson person) {
      if (!person.usingRiptide) {
         matrices.mulPose(Axis.XP.rotationDegrees(person.leaningPitch * (90.0F - person.statePitch)));
      }
   }

   private static void preparePlayerScale(@NotNull PoseStack matrices) {
      matrices.scale(-1.0F, -1.0F, 1.0F);
      matrices.scale(1.6F, 1.8F, 1.6F);
      matrices.translate(0.0F, -1.501F, 0.0F);
   }

   private static void prepareChamsNewPlayerScale(@NotNull PoseStack matrices) {
      matrices.scale(-1.0F, -1.0F, 1.0F);
      matrices.scale(0.8F, 0.9F, 0.8F);
      matrices.translate(0.0F, -1.5F, 0.0F);
   }

   private static void setOuterLayerVisible(@NotNull PlayerModel model, boolean visible) {
      model.leftPants.visible = visible;
      model.rightPants.visible = visible;
      model.leftSleeve.visible = visible;
      model.rightSleeve.visible = visible;
      model.jacket.visible = visible;
      model.hat.visible = visible;
   }

   private static void copyModelPartTransform(@NotNull ModelPart target, @NotNull ModelPart source) {
      target.xRot = source.xRot;
      target.yRot = source.yRot;
      target.zRot = source.zRot;
      target.x = source.x;
      target.y = source.y;
      target.z = source.z;
      target.xScale = source.xScale;
      target.yScale = source.yScale;
      target.zScale = source.zScale;
   }

   private void renderModelPart(
      @NotNull PlayerModel model, @NotNull PopEffect.BodyPart part, @NotNull PoseStack matrices, @NotNull BufferBuilder buffer, int color
   ) {
      switch (part) {
         case HEAD:
            model.head.render(matrices, buffer, 15728880, OverlayTexture.NO_OVERLAY, color);
            break;
         case BODY:
            model.body.render(matrices, buffer, 15728880, OverlayTexture.NO_OVERLAY, color);
            break;
         case LEFT_ARM:
            model.leftArm.render(matrices, buffer, 15728880, OverlayTexture.NO_OVERLAY, color);
            break;
         case RIGHT_ARM:
            model.rightArm.render(matrices, buffer, 15728880, OverlayTexture.NO_OVERLAY, color);
            break;
         case LEFT_LEG:
            model.leftLeg.render(matrices, buffer, 15728880, OverlayTexture.NO_OVERLAY, color);
            break;
         case RIGHT_LEG:
            model.rightLeg.render(matrices, buffer, 15728880, OverlayTexture.NO_OVERLAY, color);
      }
   }

   @NotNull
   private ModelPart getModelPart(@NotNull PlayerModel model, @NotNull PopEffect.BodyPart part) {
      return switch (part) {
         case HEAD -> model.head;
         case BODY -> model.body;
         case LEFT_ARM -> model.leftArm;
         case RIGHT_ARM -> model.rightArm;
         case LEFT_LEG -> model.leftLeg;
         case RIGHT_LEG -> model.rightLeg;
      };
   }

   private void renderWireframePart(@NotNull ModelPart part, @NotNull PoseStack matrices, @NotNull BufferBuilder buffer, int color) {
      matrices.pushPose();
      part.translateAndRotate(matrices);
      Matrix4f matrix = matrices.last().pose();

      for (Cube cuboid : ((IModelPart)(Object)part).vcore$getCuboids()) {
         this.renderCuboidWireframe(matrix, buffer, cuboid, color);
      }

      matrices.popPose();
   }

   private void renderCuboidWireframe(@NotNull Matrix4f matrix, @NotNull BufferBuilder buffer, @NotNull Cube cuboid, int color) {
      float x1 = cuboid.minX / 16.0F;
      float y1 = cuboid.minY / 16.0F;
      float z1 = cuboid.minZ / 16.0F;
      float x2 = cuboid.maxX / 16.0F;
      float y2 = cuboid.maxY / 16.0F;
      float z2 = cuboid.maxZ / 16.0F;
      this.vertexLine(matrix, buffer, x1, y1, z1, x2, y1, z1, color);
      this.vertexLine(matrix, buffer, x2, y1, z1, x2, y2, z1, color);
      this.vertexLine(matrix, buffer, x2, y2, z1, x1, y2, z1, color);
      this.vertexLine(matrix, buffer, x1, y2, z1, x1, y1, z1, color);
      this.vertexLine(matrix, buffer, x1, y1, z2, x2, y1, z2, color);
      this.vertexLine(matrix, buffer, x2, y1, z2, x2, y2, z2, color);
      this.vertexLine(matrix, buffer, x2, y2, z2, x1, y2, z2, color);
      this.vertexLine(matrix, buffer, x1, y2, z2, x1, y1, z2, color);
      this.vertexLine(matrix, buffer, x1, y1, z1, x1, y1, z2, color);
      this.vertexLine(matrix, buffer, x2, y1, z1, x2, y1, z2, color);
      this.vertexLine(matrix, buffer, x2, y2, z1, x2, y2, z2, color);
      this.vertexLine(matrix, buffer, x1, y2, z1, x1, y2, z2, color);
   }

   private void vertexLine(@NotNull Matrix4f matrix, @NotNull BufferBuilder buffer, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
      Vector3f from = new Vector3f(x1, y1, z1);
      Vector3f to = new Vector3f(x2, y2, z2);
      Vector3f normal = new Vector3f(to).sub(from).normalize();
      int alpha = color >> 24 & 0xFF;
      int red = color >> 16 & 0xFF;
      int green = color >> 8 & 0xFF;
      int blue = color & 0xFF;
      buffer.addVertex(matrix, from.x, from.y, from.z).setColor(red, green, blue, alpha).setNormal(normal.x, normal.y, normal.z);
      buffer.addVertex(matrix, to.x, to.y, to.z).setColor(red, green, blue, alpha).setNormal(normal.x, normal.y, normal.z);
   }

   @NotNull
   private Vec3 getPartDisplacement(@NotNull UUID uuid, @NotNull PopEffect.BodyPart part, float amount) {
      if (this.newDisperse.getValue() && !(amount <= 0.0F)) {
         long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits() ^ part.ordinal();
         RandomSource random = RandomSource.create(seed);

         Vec3 base = switch (part) {
            case HEAD -> new Vec3(0.0, -1.0, 0.0);
            case BODY -> Vec3.ZERO;
            case LEFT_ARM -> new Vec3(1.0, 0.0, 0.0);
            case RIGHT_ARM -> new Vec3(-1.0, 0.0, 0.0);
            case LEFT_LEG -> new Vec3(0.3, 1.0, 0.0);
            case RIGHT_LEG -> new Vec3(-0.3, 1.0, 0.0);
         };
         Vec3 noise = new Vec3((random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5);
         return base.add(noise).normalize().scale(amount);
      } else {
         return Vec3.ZERO;
      }
   }

   @NotNull
   private PlayerModel createPlayerModel() {
      PlayerModel model = new PlayerModel(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      setOuterLayerVisible(model, false);
      return model;
   }

   private int withAlpha(@NotNull ColorSetting setting, float alphaMultiplier) {
      int color = setting.getRawColor();
      int alpha = Mth.clamp((int)((color >> 24 & 0xFF) * alphaMultiplier), 0, 255);
      return alpha << 24 | color & 16777215;
   }

   private void clearEffects() {
      this.oldPopList.clear();
      this.newPopList.clear();
   }

   private boolean shouldShowNewFadeTime() {
      return this.isChamsNew() && this.newFadeOut != null && this.newFadeOut.getValue();
   }

   private boolean shouldShowNewAnimationSetting() {
      return this.isChamsNew() && this.newDisperse != null && this.newDisperse.getValue();
   }

   private boolean shouldShowNewOutlineSetting() {
      return this.isChamsNew() && this.newWireframe != null && this.newWireframe.getValue();
   }

   private boolean isChamsOld() {
      return this.mode.is(PopEffect.Mode.ChamsOld);
   }

   private boolean isChamsNew() {
      return this.mode.is(PopEffect.Mode.ChamsNew);
   }

   private boolean isEffect() {
      return this.mode.is(PopEffect.Mode.Effect);
   }

   private enum BodyPart {
      HEAD,
      BODY,
      LEFT_ARM,
      RIGHT_ARM,
      LEFT_LEG,
      RIGHT_LEG;
   }

   private enum Mode {
      ChamsOld("Chams Old"),
      ChamsNew("Chams New"),
      Effect("Effect");

      private final String displayName;

      Mode(String displayName) {
         this.displayName = displayName;
      }

      @Override
      public String toString() {
         return this.displayName;
      }
   }

   private final class NewPerson {
      private final Player player;
      private final PlayerModel modelPlayer;
      private final UUID uuid;
      private final float statePitch;
      private final float leaningPitch;
      private final boolean usingRiptide;
      private int age;

      private NewPerson(@NotNull Player source) {
         this.player = PopEffect.this.createChamsNewCapturedPlayer(source);
         this.modelPlayer = PopEffect.this.createPlayerModel();
         this.uuid = this.player.getUUID();
         this.statePitch = source.getXRot();
         this.leaningPitch = source.getSwimAmount(0.0F);
         this.usingRiptide = source.isAutoSpinAttack();
         this.age = 0;
         this.captureModelPose(source);
      }

      private void captureModelPose(@NotNull Player source) {
         if (Module.mc.getEntityRenderDispatcher().getRenderer(source) instanceof LivingEntityRenderer<?, ?, ?> renderer
            && renderer.getModel() instanceof PlayerModel originalModel) {
            PopEffect.copyModelPartTransform(this.modelPlayer.head, originalModel.head);
            PopEffect.copyModelPartTransform(this.modelPlayer.hat, originalModel.hat);
            PopEffect.copyModelPartTransform(this.modelPlayer.body, originalModel.body);
            PopEffect.copyModelPartTransform(this.modelPlayer.leftArm, originalModel.leftArm);
            PopEffect.copyModelPartTransform(this.modelPlayer.rightArm, originalModel.rightArm);
            PopEffect.copyModelPartTransform(this.modelPlayer.leftLeg, originalModel.leftLeg);
            PopEffect.copyModelPartTransform(this.modelPlayer.rightLeg, originalModel.rightLeg);
            PopEffect.setOuterLayerVisible(this.modelPlayer, false);
         } else {
            AvatarRenderState state = new AvatarRenderState();
            if (Module.mc.getEntityRenderDispatcher().getRenderer(this.player) instanceof AvatarRenderer renderer
               && this.player instanceof AbstractClientPlayer clientPlayer) {
               renderer.extractRenderState(clientPlayer, state, Render3DEngine.getTickDelta());
            }
            this.modelPlayer.setupAnim(state);
            PopEffect.setOuterLayerVisible(this.modelPlayer, false);
         }
      }

      private void update(@NotNull CopyOnWriteArrayList<PopEffect.NewPerson> arrayList) {
         this.age++;
         if (this.age >= PopEffect.this.newLifeTime.getValue()) {
            arrayList.remove(this);
            this.player.remove(RemovalReason.DISCARDED);
         }
      }

      private float getAlpha(float tickDelta, float displacement) {
         if (!PopEffect.this.newFadeOut.getValue()) {
            return 1.0F;
         }

         float elapsed = Math.max(0.0F, this.age - 1 + tickDelta);
         float fadeStart = PopEffect.this.newLifeTime.getValue().intValue() * 0.8F;
         float lifeAlpha = elapsed >= fadeStart ? 1.0F - (elapsed - fadeStart) / (PopEffect.this.newLifeTime.getValue().intValue() - fadeStart) : 1.0F;
         float maxDistance = PopEffect.this.newDisperseMaxDistance.getValue();
         float disperseAlpha = PopEffect.this.newDisperse.getValue() && maxDistance > 0.0F ? 1.0F - displacement / maxDistance : 1.0F;
         return Mth.clamp(lifeAlpha * disperseAlpha, 0.0F, 1.0F);
      }

      private float getDisplacement(float tickDelta) {
         if (!PopEffect.this.newDisperse.getValue()) {
            return 0.0F;
         }

         float elapsed = Math.max(0.0F, this.age - 1 + tickDelta);
         float speed = Mth.clamp(PopEffect.this.newDisperseSpeed.getValue(), 1.0F, 10.0F);
         float multiplier = (float)Math.pow(speed / 10.0F, 2.0) * 2.0F;
         return Math.min(elapsed * multiplier, PopEffect.this.newDisperseMaxDistance.getValue());
      }
   }

   private final class OldPerson {
      private final Player player;
      private final PlayerModel modelPlayer;
      private int alpha;

      private OldPerson(@NotNull Player player) {
         this.player = player;
         this.modelPlayer = PopEffect.this.createPlayerModel();
         this.modelPlayer.getHead().offsetScale(new Vector3f(-0.3F, -0.3F, -0.3F));
         this.alpha = PopEffect.this.oldColor.getValue().getAlpha();
      }

      private void update(@NotNull CopyOnWriteArrayList<PopEffect.OldPerson> arrayList) {
         if (this.alpha <= 0) {
            arrayList.remove(this);
            this.player.remove(RemovalReason.KILLED);
            this.player.onClientRemoval();
         } else {
            this.alpha = this.alpha - PopEffect.this.oldAlphaSpeed.getValue();
         }
      }

      private int getAlpha() {
         return MathUtility.clamp(this.alpha, 0, 255);
      }
   }
}
