package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public class Prediction extends Module {
   private final Setting<Boolean> renderBow = new Setting<>("Bow", true);
   private final Setting<Boolean> renderCrossbow = new Setting<>("Crossbow", true);
   private final Setting<Boolean> renderPearls = new Setting<>("Pearl", true);
   private final Setting<Boolean> renderItems = new Setting<>("Item", false);
   private final Setting<Boolean> renderLine = new Setting<>("Line", true);
   private final Setting<Boolean> renderLanding = new Setting<>("Landing", true);
   private final Setting<Boolean> showTags = new Setting<>("Tag", true);

   private List<Prediction.PredictionResult> cachedResults = Collections.emptyList();
   private long lastComputeNano = 0L;

   public Prediction() {
      super("Prediction", "Draws impact previews for projectiles and items.", Module.Category.RENDER);
   }

   @Override
   public void onDisable() {
      this.cachedResults = Collections.emptyList();
      this.lastComputeNano = 0L;
   }

   @Override
   public void onRender3D(PoseStack stack) {
      if (!fullNullCheck()) {
         for (Prediction.PredictionResult result : this.getPredictions()) {
            this.renderPath(stack, result);
         }
      }
   }

   @Override
   public void onRender2D(GuiGraphics context) {
      if (fullNullCheck() || mc.screen instanceof ClickGUI || mc.options.hideGui) {
         return;
      }
      if (this.showTags.getValue()) {
         for (Prediction.PredictionResult result : this.getPredictions()) {
            // ONLY show the floating HUD tag when the projectile has actually been shot / thrown in-flight!
            // When holding the weapon / pearl in hand (preview mode), no tag is shown to prevent HUD clutter.
            // Once the projectile impacts/lands, the entity is destroyed and this HUD tag automatically disappears.
            if (result.isThrown()) {
               this.drawTag(context, result);
            }
         }
      }
   }

   private List<Prediction.PredictionResult> getPredictions() {
      if (fullNullCheck() || mc.level == null) {
         return Collections.emptyList();
      }

      long now = System.nanoTime();
      // Cache results within the same frame (~2ms window) so onRender3D and onRender2D share identical results
      if (now - this.lastComputeNano < 2_000_000L && !this.cachedResults.isEmpty()) {
         return this.cachedResults;
      }

      List<Prediction.PredictionResult> list = new ArrayList<>();

      // 1. Predict player held weapon / item (Bow, Crossbow, Ender Pearl)
      Prediction.PredictionResult playerPrediction = this.getPlayerPrediction();
      if (playerPrediction != null) {
         list.add(playerPrediction);
      }

      // 2. Predict in-flight world entities (Thrown pearls, flying arrows, falling items)
      list.addAll(this.computeWorldEntityPredictions());

      this.cachedResults = list;
      this.lastComputeNano = now;
      return this.cachedResults;
   }

   private Prediction.PredictionResult getPlayerPrediction() {
      if (mc.player == null || mc.level == null) {
         return null;
      }

      ItemStack mainHand = mc.player.getMainHandItem();
      ItemStack offHand = mc.player.getOffhandItem();

      // Bow
      if (this.renderBow.getValue()) {
         if (mainHand.getItem() instanceof BowItem) {
            return this.simulateBow(mainHand);
         } else if (offHand.getItem() instanceof BowItem) {
            return this.simulateBow(offHand);
         }
      }

      // Crossbow
      if (this.renderCrossbow.getValue()) {
         if (mainHand.getItem() instanceof CrossbowItem) {
            return this.simulateCrossbow(mainHand);
         } else if (offHand.getItem() instanceof CrossbowItem) {
            return this.simulateCrossbow(offHand);
         }
      }

      // Ender Pearl
      if (this.renderPearls.getValue()) {
         if (mainHand.is(Items.ENDER_PEARL)) {
            return this.simulatePlayerPearl();
         } else if (offHand.is(Items.ENDER_PEARL)) {
            return this.simulatePlayerPearl();
         }
      }

      return null;
   }

   private Prediction.PredictionResult simulateBow(ItemStack stack) {
      boolean isUsing = mc.player.isUsingItem() && mc.player.getUseItem() == stack;
      float power;
      if (isUsing) {
         power = BowItem.getPowerForTime(mc.player.getTicksUsingItem());
      } else {
         power = 1.0F; // Full charge preview while holding bow
      }

      if (power < 0.1F) {
         return null;
      }
      double speed = power * 3.0F;
      return this.simulatePlayerLaunch(new ItemStack(Items.BOW), speed, 0.05, 0.99, 0.60);
   }

   private Prediction.PredictionResult simulateCrossbow(ItemStack stack) {
      double speed = 3.15F;
      return this.simulatePlayerLaunch(new ItemStack(Items.CROSSBOW), speed, 0.05, 0.99, 0.60);
   }

   private Prediction.PredictionResult simulatePlayerPearl() {
      double speed = 1.5F;
      return this.simulatePlayerLaunch(new ItemStack(Items.ENDER_PEARL), speed, 0.03, 0.99, 0.80);
   }

   private Prediction.PredictionResult simulatePlayerLaunch(
      ItemStack icon, double speed, double gravity, double drag, double waterDrag
   ) {
      if (mc.player == null || mc.level == null) {
         return null;
      }

      float tickDelta = Render3DEngine.getTickDelta();
      Vec3 eyePos = mc.player.getEyePosition(tickDelta);

      float yaw = mc.player.getYRot();
      float pitch = mc.player.getXRot();
      if (ModuleManager.aura != null && ModuleManager.aura.isEnabled() && Aura.target != null) {
         yaw = ModuleManager.aura.rotationYaw;
         pitch = ModuleManager.aura.rotationPitch;
      }

      float f = -Mth.sin(yaw * (float) (Math.PI / 180.0)) * Mth.cos(pitch * (float) (Math.PI / 180.0));
      float g = -Mth.sin(pitch * (float) (Math.PI / 180.0));
      float h = Mth.cos(yaw * (float) (Math.PI / 180.0)) * Mth.cos(pitch * (float) (Math.PI / 180.0));
      Vec3 dir = new Vec3(f, g, h).normalize();
      Vec3 motion = dir.scale(speed);

      // Add player momentum if moving (matching vanilla Minecraft projectile throw physics)
      Vec3 playerMotion = mc.player.getDeltaMovement();
      motion = motion.add(playerMotion.x, mc.player.onGround() ? 0.0 : playerMotion.y, playerMotion.z);

      Vec3 startPos = eyePos.subtract(0.0, 0.1, 0.0);
      Prediction.LandingData landingData = this.simulateLanding(startPos, motion, gravity, drag, waterDrag, mc.player);
      if (landingData == null) {
         return null;
      }
      return new Prediction.PredictionResult(startPos, landingData.landingPos(), false, true, landingData.timeSeconds(), icon);
   }

   private List<Prediction.PredictionResult> computeWorldEntityPredictions() {
      List<Prediction.PredictionResult> results = new ArrayList<>();
      if (mc.level == null || mc.player == null) {
         return results;
      }

      boolean pearls = this.renderPearls.getValue();
      boolean arrows = this.renderBow.getValue() || this.renderCrossbow.getValue();
      boolean items = this.renderItems.getValue();

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof ThrownEnderpearl pearl && pearls && !pearl.onGround()) {
            boolean fromPlayer = pearl.getOwner() == mc.player || (pearl.getOwner() == null && this.isNearestPlayer(pearl));
            Prediction.PredictionResult result = this.simulateEntity(pearl, new ItemStack(Items.ENDER_PEARL), 0.03, 0.99, 0.80, fromPlayer);
            if (result != null) {
               results.add(result);
            }
         } else if (entity instanceof AbstractArrow arrow && arrows && !arrow.onGround() && arrow.getDeltaMovement().lengthSqr() > 1.0E-4) {
            boolean fromPlayer = arrow.getOwner() == mc.player;
            ItemStack weaponItem = arrow.getWeaponItem();
            ItemStack icon = (weaponItem != null && !weaponItem.isEmpty()) ? weaponItem : new ItemStack(Items.ARROW);
            Prediction.PredictionResult result = this.simulateEntity(arrow, icon, 0.05, 0.99, 0.60, fromPlayer);
            if (result != null) {
               results.add(result);
            }
         } else if (entity instanceof ItemEntity item && items && this.shouldSimulateItem(item)) {
            ItemStack itemStack = item.getItem();
            ItemStack icon = (itemStack != null && !itemStack.isEmpty()) ? itemStack.copy() : ItemStack.EMPTY;
            Prediction.PredictionResult result = this.simulateEntity(item, icon, 0.04, 0.98, 0.80, false);
            if (result != null) {
               results.add(result);
            }
         }
      }

      return results;
   }

   private boolean isNearestPlayer(Entity entity) {
      if (mc.player == null || mc.level == null) {
         return false;
      }
      double myDist = mc.player.distanceToSqr(entity);
      for (Player player : mc.level.players()) {
         if (player != mc.player && player.distanceToSqr(entity) < myDist) {
            return false;
         }
      }
      return true;
   }

   private boolean shouldSimulateItem(ItemEntity item) {
      return !item.onGround() && item.getDeltaMovement().lengthSqr() > 1.0E-4;
   }

   private Prediction.PredictionResult simulateEntity(
      Entity entity, ItemStack icon, double gravity, double drag, double waterDrag, boolean fromPlayer
   ) {
      if (mc.level == null) {
         return null;
      }
      Vec3 position = entity.position();
      Vec3 motion = entity.getDeltaMovement();
      Prediction.LandingData landingData = this.simulateLanding(position, motion, gravity, drag, waterDrag, entity);
      if (landingData == null) {
         return null;
      }
      return new Prediction.PredictionResult(position, landingData.landingPos(), true, fromPlayer, landingData.timeSeconds(), icon);
   }

   private Prediction.LandingData simulateLanding(
      Vec3 startPos, Vec3 initialMotion, double gravity, double drag, double waterDrag, Entity ignoreEntity
   ) {
      if (mc.level == null) {
         return null;
      }

      Vec3 position = startPos;
      Vec3 motion = initialMotion;
      double time = 0.0;

      for (int i = 0; i < 600; i++) {
         Vec3 next = position.add(motion);

         HitResult hit = this.findCollision(position, next, ignoreEntity);

         boolean inWater = false;
         try {
            BlockPos bpos = BlockPos.containing(position);
            inWater = mc.level.getFluidState(bpos).is(FluidTags.WATER);
         } catch (Throwable ignored) {
         }

         if (inWater) {
            motion = motion.scale(waterDrag);
         } else {
            motion = motion.scale(drag);
         }
         motion = motion.subtract(0.0, gravity, 0.0);
         time += 0.05;

         if (hit != null && hit.getType() != Type.MISS) {
            return new Prediction.LandingData(hit.getLocation(), time);
         }

         if (next.y <= mc.level.getMinY()) {
            return new Prediction.LandingData(next, time);
         }

         position = next;
      }

      return new Prediction.LandingData(position, time);
   }

   private HitResult findCollision(Vec3 start, Vec3 end, Entity ignoreEntity) {
      if (mc.level == null) {
         return null;
      }
      ClipContext ctx = new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, ignoreEntity != null ? ignoreEntity : mc.player);
      HitResult blockHit = mc.level.clip(ctx);
      Vec3 limit = (blockHit != null && blockHit.getType() != Type.MISS) ? blockHit.getLocation() : end;

      AABB searchBox = new AABB(
         Math.min(start.x, limit.x), Math.min(start.y, limit.y), Math.min(start.z, limit.z),
         Math.max(start.x, limit.x), Math.max(start.y, limit.y), Math.max(start.z, limit.z)
      ).inflate(0.5);

      EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
         mc.level,
         ignoreEntity != null ? ignoreEntity : mc.player,
         start,
         limit,
         searchBox,
         e -> !e.isSpectator() && e.isPickable() && e != ignoreEntity && e != mc.player,
         0.3F
      );

      if (entityHit != null) {
         return entityHit;
      }
      return (blockHit != null && blockHit.getType() == Type.BLOCK) ? blockHit : null;
   }

   private void renderPath(PoseStack stack, Prediction.PredictionResult result) {
      if (mc.player == null) {
         return;
      }

      // 1. Draw single 3D trajectory line
      if (this.renderLine.getValue()) {
         Color lineColor = Render2DEngine.injectAlpha(HudEditor.getColor(0), 220);

         if (result.isThrown()) {
            // Projectile is in-flight: draw 1 direct line from the projectile in the air down to its landing spot!
            if (result.startPos() != null) {
               Render3DEngine.drawLine(result.startPos(), result.landingPos(), lineColor);
            }
         } else {
            // Aiming preview: draw 1 line from player's POV to predicted landing spot
            Vec3 pov = mc.player.getEyePosition(Render3DEngine.getTickDelta());
            Vec3 toLanding = result.landingPos().subtract(pov);
            double dist = toLanding.length();
            Vec3 playerStart = dist > 0.5 ? pov.add(toLanding.scale(0.3 / dist)) : pov;
            Render3DEngine.drawLine(playerStart, result.landingPos(), lineColor);
         }
      }

      // 2. Render colored landing spot indicator on floor (clean box only, no crosshair rays)
      if (this.renderLanding.getValue()) {
         this.renderLandingSpot(stack, result);
      }
   }

   private void renderLandingSpot(PoseStack stack, Prediction.PredictionResult result) {
      Vec3 landing = result.landingPos();
      Color baseColor = HudEditor.getColor(0);
      Color fillColor = Render2DEngine.injectAlpha(baseColor, 60);
      Color outlineColor = Render2DEngine.injectAlpha(baseColor, 220);

      double size = 0.25;
      AABB landingBox = new AABB(
         landing.x - size, landing.y - 0.05, landing.z - size,
         landing.x + size, landing.y + 0.15, landing.z + size
      );

      // Draw 3D filled bounding box & outline at impact point
      Render3DEngine.drawFilledBox(stack, landingBox, fillColor);
      Render3DEngine.drawBoxOutline(landingBox, outlineColor, 1.5F);
   }

   private void drawTag(GuiGraphics context, Prediction.PredictionResult result) {
      // First try landing spot on the ground (floating 0.4 blocks above landing box)
      Vec3 targetPos = result.landingPos().add(0.0, 0.4, 0.0);
      Vec3 screen = Render3DEngine.worldSpaceToScreenSpace(targetPos);

      // If landing spot is off-screen (e.g. player looking up into the sky), display above flying projectile!
      if (!this.isOnScreen(screen) && result.startPos() != null) {
         targetPos = result.startPos().add(0.0, 0.5, 0.0);
         screen = Render3DEngine.worldSpaceToScreenSpace(targetPos);
      }

      if (this.isOnScreen(screen)) {
         Prediction.TagLayout layout = this.createTagLayout(result, screen);
         PoseStack matrices = Render2DEngine.toPoseStack(context.pose());
         matrices.pushPose();
         this.applyTagScale(matrices, layout);
         Render2DEngine.drawRound(matrices, layout.x(), layout.y(), layout.width(), layout.height(), 3.0F, new Color(0, 0, 0, 160));
         if (!result.icon().isEmpty()) {
            context.pose().pushMatrix();
            float centerX = layout.x() + layout.width() / 2.0F;
            float centerY = layout.y() + layout.height() / 2.0F;
            context.pose().translate(centerX, centerY);
            context.pose().scale(0.85F, 0.85F);
            context.pose().translate(-centerX, -centerY);
            context.pose().translate(layout.iconX(), layout.iconY());
            context.pose().scale(layout.iconScale(), layout.iconScale());
            context.renderItem(result.icon(), 0, 0);
            context.renderItemDecorations(mc.font, result.icon(), 0, 0);
            context.pose().popMatrix();
         }

         matrices.pushPose();
         matrices.translate(0.0F, 3.0F, 0.0F);
         FontRenderers.sf_medium.drawString(matrices, layout.text(), layout.textX(), layout.textY(), Color.WHITE.getRGB());
         matrices.popPose();
         matrices.popPose();
      }
   }

   private void applyTagScale(PoseStack matrices, Prediction.TagLayout layout) {
      float centerX = layout.x() + layout.width() / 2.0F;
      float centerY = layout.y() + layout.height() / 2.0F;
      matrices.translate(centerX, centerY, 0.0F);
      matrices.scale(0.85F, 0.85F, 1.0F);
      matrices.translate(-centerX, -centerY, 0.0F);
   }

   private boolean isOnScreen(Vec3 screenPos) {
      return Double.isFinite(screenPos.x) && Double.isFinite(screenPos.y) && screenPos.z > 0.0 && screenPos.z < 1.0;
   }

   private String formatTime(double seconds) {
      return String.format(Locale.ROOT, "%.1f s", seconds);
   }

   private Prediction.TagLayout createTagLayout(Prediction.PredictionResult result, Vec3 screen) {
      String text = this.formatTime(result.timeSeconds());
      float textWidth = FontRenderers.sf_medium.getStringWidth(text);
      float width = Math.max(35.0F, textWidth + 20.0F);
      float height = 14.0F;
      float x = (float)screen.x - width / 2.0F;
      float y = (float)screen.y - height - 6.0F;
      float iconScale = 0.8F;
      float iconSize = 16.0F * iconScale;
      float iconX = x + 2.0F;
      float iconY = y + (height - iconSize) / 2.0F;
      float centeredTextX = x + width / 2.0F - textWidth / 2.0F;
      float minTextX = iconX + iconSize + 2.0F;
      float textX = Math.max(minTextX, centeredTextX);
      float textHeight = FontRenderers.sf_medium.getStringHeight(text);
      float textY = y + (height - textHeight) / 2.0F;
      return new Prediction.TagLayout(text, x, y, width, height, iconX, iconY, iconScale, iconSize, textX, textY);
   }

   private record PredictionResult(
      Vec3 startPos,
      Vec3 landingPos,
      boolean isThrown,
      boolean fromPlayer,
      double timeSeconds,
      ItemStack icon
   ) {
   }

   private record LandingData(Vec3 landingPos, double timeSeconds) {
   }

   private record TagLayout(
      String text, float x, float y, float width, float height, float iconX, float iconY, float iconScale, float iconSize, float textX, float textY
   ) {
   }
}
