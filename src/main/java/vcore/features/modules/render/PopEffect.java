package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.awt.Color;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import vcore.events.impl.TotemPopEvent;
import vcore.features.modules.Module;
import vcore.features.modules.render.popeffect.PopEffectParticles;
import vcore.injection.accesors.IModelPart;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.RenderSystem;

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
            break;
         case Effect:
            break;
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
               break;
            case ChamsNew:
               this.addChamsNew(event.getEntity());
               break;
            case Effect:
               this.addEffect(event.getEntity());
               break;
         }
      }
   }

   private void addChamsOld(@NotNull Player source) {
      if (!source.equals(mc.player)) {
         for (PopEffect.OldPerson existing : this.oldPopList) {
            if (existing.uuid.equals(source.getUUID()) && existing.age == 0) {
               return;
            }
         }
         this.oldPopList.add(new PopEffect.OldPerson(source));
      }
   }

   private void addChamsNew(@NotNull Player source) {
      if (!source.equals(mc.player)) {
         for (PopEffect.NewPerson existing : this.newPopList) {
            if (existing.uuid.equals(source.getUUID()) && existing.age == 0) {
               return;
            }
         }
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
      if (this.oldPopList.isEmpty()) {
         return;
      }
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.defaultBlendFunc();
      this.oldPopList.forEach(person -> this.renderOldEntity(matrices, person));
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
      com.mojang.blaze3d.vertex.BufferUploader.invalidateCommandEncoder();
      try {
         org.lwjgl.opengl.GL33.glBindSampler(0, 0);
      } catch (Throwable ignored) {}
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
         com.mojang.blaze3d.vertex.BufferUploader.invalidateCommandEncoder();
         try {
            org.lwjgl.opengl.GL33.glBindSampler(0, 0);
         } catch (Throwable ignored) {}
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

   private void renderOldEntity(@NotNull PoseStack matrices, @NotNull PopEffect.OldPerson person) {
      setOuterLayerVisible(person.modelPlayer, false);
      Vec3 camPos = Render3DEngine.getCameraPos();
      double x = person.pos.x - camPos.x;
      double y = person.pos.y - camPos.y;
      double z = person.pos.z - camPos.z;
      matrices.pushPose();
      matrices.translate((float)x, (float)y, (float)z);
      float yRotYaw = person.getAlpha() / 255.0F * 360.0F * this.oldRotationSpeed.getValue();
      yRotYaw = yRotYaw == 0.0F
         ? 0.0F
         : Render2DEngine.interpolateFloat(
            yRotYaw, yRotYaw - this.oldAlphaSpeed.getValue().intValue() / 255.0F * 360.0F * this.oldRotationSpeed.getValue(), Render3DEngine.getTickDelta()
         );
      matrices.mulPose(Axis.YP.rotationDegrees(180.0F - person.yBodyRot + yRotYaw));
      preparePlayerScale(matrices);
      int color = this.withAlpha(this.oldColor.getValue(), person.getAlpha() / 255.0F);
      BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      person.modelPlayer.renderToBuffer(matrices, buffer, 15728880, OverlayTexture.NO_OVERLAY, color);
      Render2DEngine.endBuilding(buffer);
      matrices.popPose();
   }

   private void renderNewFilledModel(
      @NotNull PoseStack matrices, @NotNull BufferBuilder buffer, @NotNull PopEffect.NewPerson person, float alpha, float displacement
   ) {
      int color = this.withAlpha(this.newFilledColor.getValue(), alpha);
      this.setupChamsNewPlayerTransform(matrices, person);
      setOuterLayerVisible(person.modelPlayer, false);

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

      for (PopEffect.BodyPart part : PopEffect.BodyPart.values()) {
         Vec3 partDisplacement = this.getPartDisplacement(person.uuid, part, displacement);
         matrices.pushPose();
         matrices.translate(partDisplacement.x(), partDisplacement.y(), partDisplacement.z());
         this.renderWireframePart(this.getModelPart(person.modelPlayer, part), matrices, buffer, color);
         matrices.popPose();
      }

      matrices.popPose();
   }

   private void setupChamsNewPlayerTransform(@NotNull PoseStack matrices, @NotNull PopEffect.NewPerson person) {
      Vec3 camPos = Render3DEngine.getCameraPos();
      double x = person.pos.x - camPos.x;
      double y = person.pos.y - camPos.y;
      double z = person.pos.z - camPos.z;
      matrices.pushPose();
      matrices.translate((float)x, (float)y, (float)z);
      matrices.mulPose(Axis.YP.rotationDegrees(180.0F - person.yBodyRot));
      prepareChamsNewPlayerScale(matrices);
   }

   private static void preparePlayerScale(@NotNull PoseStack matrices) {
      matrices.scale(-1.0F, -1.0F, 1.0F);
      matrices.scale(0.9375F, 0.9375F, 0.9375F);
      matrices.translate(0.0F, -1.501F, 0.0F);
   }

   private static void prepareChamsNewPlayerScale(@NotNull PoseStack matrices) {
      matrices.scale(-1.0F, -1.0F, 1.0F);
      matrices.scale(0.9375F, 0.9375F, 0.9375F);
      matrices.translate(0.0F, -1.501F, 0.0F);
   }

   private static void setOuterLayerVisible(@NotNull PlayerModel model, boolean visible) {
      model.leftPants.visible = visible;
      model.rightPants.visible = visible;
      model.leftSleeve.visible = visible;
      model.rightSleeve.visible = visible;
      model.jacket.visible = visible;
      model.hat.visible = visible;
   }

   private void captureModelPose(@NotNull Player source, @NotNull PlayerModel model) {
      if (mc.getEntityRenderDispatcher().getRenderer(source) instanceof AvatarRenderer renderer
         && source instanceof AbstractClientPlayer clientPlayer) {
         AvatarRenderState state = new AvatarRenderState();
         renderer.extractRenderState(clientPlayer, state, Render3DEngine.getTickDelta());
         model.setupAnim(state);
      }
      setOuterLayerVisible(model, false);
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
            break;
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
      Vector3f normal = new Vector3f(to).sub(from);
      if (normal.lengthSquared() > 1.0E-6F) {
         normal.normalize();
      } else {
         normal.set(0.0F, 1.0F, 0.0F);
      }
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
            case HEAD -> new Vec3(0.0, -0.6, 0.0);
            case BODY -> Vec3.ZERO;
            case LEFT_ARM -> new Vec3(0.6, 0.0, 0.0);
            case RIGHT_ARM -> new Vec3(-0.6, 0.0, 0.0);
            case LEFT_LEG -> new Vec3(0.3, 0.6, 0.0);
            case RIGHT_LEG -> new Vec3(-0.3, 0.6, 0.0);
         };
         Vec3 noise = new Vec3((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2);
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
      private final Vec3 pos;
      private final float yBodyRot;
      private final PlayerModel modelPlayer;
      private final UUID uuid;
      private int age;

      private NewPerson(@NotNull Player source) {
         this.pos = source.position();
         this.yBodyRot = source.yBodyRot;
         this.modelPlayer = PopEffect.this.createPlayerModel();
         this.uuid = source.getUUID();
         this.age = 0;
         PopEffect.this.captureModelPose(source, this.modelPlayer);
      }

      private void update(@NotNull CopyOnWriteArrayList<PopEffect.NewPerson> arrayList) {
         this.age++;
         if (this.age >= PopEffect.this.newLifeTime.getValue()) {
            arrayList.remove(this);
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
      private Vec3 pos;
      private final float yBodyRot;
      private final PlayerModel modelPlayer;
      private final UUID uuid;
      private int alpha;
      private int age;

      private OldPerson(@NotNull Player source) {
         this.pos = source.position();
         this.yBodyRot = source.yBodyRot;
         this.modelPlayer = PopEffect.this.createPlayerModel();
         this.uuid = source.getUUID();
         this.alpha = PopEffect.this.oldColor.getValue().getAlpha();
         this.age = 0;
         PopEffect.this.captureModelPose(source, this.modelPlayer);
      }

      private void update(@NotNull CopyOnWriteArrayList<PopEffect.OldPerson> arrayList) {
         this.age++;
         if (this.alpha <= 0) {
            arrayList.remove(this);
         } else {
            this.alpha = this.alpha - PopEffect.this.oldAlphaSpeed.getValue();
            this.pos = this.pos.add(0.0, PopEffect.this.oldYSpeed.getValue().intValue() / 50.0, 0.0);
         }
      }

      private int getAlpha() {
         return MathUtility.clamp(this.alpha, 0, 255);
      }
   }
}
