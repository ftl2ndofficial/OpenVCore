package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.AntiBot;
import vcore.features.modules.combat.HitBox;
import vcore.features.modules.misc.FixHP;
import vcore.features.modules.misc.NameProtect;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.SettingGroup;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public class ESP extends Module {
   private final Map<LivingEntity, Vector4f> projectedBounds = new HashMap<>();
   private static final Color ENEMY_COLOR = new Color(-1, true);
   private static final Color FRIEND_COLOR = new Color(-2147437568, true);
   private static final Color TAG_FILL_COLOR = new Color(Integer.MIN_VALUE, true);
   private static final Color TEXT_COLOR = new Color(-1, true);
   private static final float TWO_D_GRADIENT_RANGE = 180.0F;
   private static final float BOX_GRADIENT_DARK_FACTOR = 0.35F;
   private static final Color HEALTH_TOP_COLOR = new Color(0, 255, 0);
   private static final Color HEALTH_BOTTOM_COLOR = new Color(255, 0, 0);
   private static final Color HEALTH_BACKGROUND = new Color(0, 0, 0, 255);
   private static final Color HEALTH_MISSING_OVERLAY = new Color(0, 0, 0, 255);
   private final Setting<SettingGroup> nameTagGroup = new Setting<>("NameTag", new SettingGroup(false, 0));
   private final Setting<Boolean> drawTag = new Setting<>("NameTag", true).addToGroup(this.nameTagGroup);
   private final Setting<Boolean> drawArmor = new Setting<>("Armor", false).addToGroup(this.nameTagGroup);
   private final Setting<Boolean> drawHpTag = new Setting<>("HP", true, v -> this.drawTag.getValue()).addToGroup(this.nameTagGroup);
   private final Setting<Boolean> drawHealth = new Setting<>("HealthBar", false).addToGroup(this.nameTagGroup);
   private final Setting<SettingGroup> espGroup = new Setting<>("ESP", new SettingGroup(false, 0));
   private final Setting<ESP.Mode> mode = new Setting<>("Mode", ESP.Mode.TwoD).addToGroup(this.espGroup);
   private final Setting<Float> fadeDistance = new Setting<>("fadeDistance", 3.0F, 0.0F, 12.0F).addToGroup(this.espGroup).step(0.1F);
   public final Setting<Boolean> syncColor = new Setting<>("SyncColor", false).addToGroup(this.espGroup);
   public final Setting<ColorSetting> boxColor = new Setting<>(
         "Color", new ColorSetting(new Color(255, 255, 255, 255).getRGB()), v -> !this.syncColor.getValue()
      )
      .addToGroup(this.espGroup);
   private final Setting<SettingGroup> targetFilterGroup = new Setting<>("Target Filter", new SettingGroup(false, 0));
   private final Setting<Boolean> players = new Setting<>("Players", true).addToGroup(this.targetFilterGroup);
   private final Setting<Boolean> nakedOnly = new Setting<>("Naked", true, v -> this.players.getValue()).addToGroup(this.targetFilterGroup);
   private final Setting<Boolean> showSelf = new Setting<>("Self", false, v -> this.players.getValue()).addToGroup(this.targetFilterGroup);
   private final Setting<Boolean> animals = new Setting<>("Animals", false).addToGroup(this.targetFilterGroup);
   private final Setting<Boolean> mobs = new Setting<>("Mobs", false).addToGroup(this.targetFilterGroup);
   private final Setting<Boolean> villagers = new Setting<>("Villagers", false).addToGroup(this.targetFilterGroup);

   public ESP() {
      super("ESP", "Highlights specific entities.", Module.Category.RENDER);
   }

   @Override
   public void onDisable() {
      this.projectedBounds.clear();
   }

   @Override
   public void onRender2D(GuiGraphics context) {
      if (fullNullCheck() || mc.screen instanceof ClickGUI) {
         this.projectedBounds.clear();
         return;
      }

      this.projectedBounds.clear();
      this.collectTargets().forEach(entity -> {
         Vector4f rect = this.project(entity);
         if (rect != null) {
            this.projectedBounds.put(entity, rect);
         }
      });
      this.projectedBounds.forEach((entity, rect) -> this.renderEntity(context, entity, rect));
   }

   @Override
   public void onRender3D(PoseStack matrices) {
      if (!fullNullCheck() && this.mode.getValue() == ESP.Mode.Box) {
         for (LivingEntity entity : this.collectTargets()) {
            this.drawFilledHitbox(matrices, entity);
         }
      }
   }

   private List<LivingEntity> collectTargets() {
      if (mc.player != null && mc.level != null) {
         double size = 256.0;
         AABB search = AABB.ofSize(mc.player.position(), size * 2.0, size * 2.0, size * 2.0);
         return mc.level.getEntitiesOfClass(LivingEntity.class, search, this::isTargetValid);
      } else {
         return List.of();
      }
   }

   private boolean isTargetValid(LivingEntity entity) {
      if (entity == null || !entity.isAlive()) {
         return false;
      }

      if (entity == mc.player && !this.shouldRenderSelf()) {
         return false;
      }

      if (entity instanceof Player player) {
         if (ModuleManager.antiBot.isEnabled() && ModuleManager.antiBot.mode.getValue() == AntiBot.Mode.Matrix && AntiBot.isBot(player)) {
            return false;
         } else if (player.isSpectator()) {
            return false;
         } else if (!this.players.getValue()) {
            return false;
         } else if (entity == mc.player && this.showSelf.getValue()) {
            return true;
         } else {
            return this.nakedOnly.getValue() ? true : !this.isNaked(player);
         }
      } else if (entity instanceof Villager) {
         return this.villagers.getValue();
      } else if (entity instanceof Animal) {
         return this.animals.getValue();
      } else {
         return entity instanceof Monster ? this.mobs.getValue() : false;
      }
   }

   private boolean isNaked(Player player) {
      for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
         if (!player.getItemBySlot(slot).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private void renderEntity(GuiGraphics context, LivingEntity entity, Vector4f rect) {
      PoseStack matrices = Render2DEngine.toPoseStack(context.pose());
      if (this.mode.getValue() == ESP.Mode.TwoD) {
         this.draw2DBox(matrices, rect, entity);
      }

      if (this.drawHealth.getValue() && this.shouldDrawNameTagHealthBar()) {
         this.drawHealthBar(matrices, rect, entity);
      }

      if (this.drawArmor.getValue() && entity instanceof Player player) {
         this.drawArmorStrip(context, rect, player);
      }

      if (this.drawTag.getValue()) {
         this.drawNameTag(context, rect, entity);
      }
   }

   private void draw2DBox(PoseStack matrices, Vector4f rect, LivingEntity entity) {
      float left = rect.x;
      float top = rect.y;
      float right = rect.z;
      float bottom = rect.w;
      float width = right - left;
      float height = bottom - top;
      float thin = 0.5F;
      Color baseColor = this.getEspColor(entity);
      int alpha = Math.round(ENEMY_COLOR.getAlpha() * this.getFadeAlpha(entity));
      if (alpha > 0) {
         Color color = Render2DEngine.injectAlpha(baseColor, alpha);
         float horizontal = width * 0.3F;
         float vertical = height * 0.3F;
         float corner = thin + 0.2F;
         boolean gradient = this.syncColor.getValue() && !this.isFriend(entity);
         int gradientOffset = entity.tickCount * 2;
         this.draw2DBoxRect(matrices, left, top, horizontal, corner, color, gradient, left, top, width, height, alpha, gradientOffset);
         this.draw2DBoxRect(matrices, right - horizontal, top, horizontal, corner, color, gradient, left, top, width, height, alpha, gradientOffset);
         this.draw2DBoxRect(matrices, left, bottom - corner, horizontal, corner, color, gradient, left, top, width, height, alpha, gradientOffset);
         this.draw2DBoxRect(matrices, right - horizontal, bottom - corner, horizontal, corner, color, gradient, left, top, width, height, alpha, gradientOffset);
         this.draw2DBoxRect(matrices, left, top, corner, vertical, color, gradient, left, top, width, height, alpha, gradientOffset);
         this.draw2DBoxRect(matrices, right - corner, top, corner, vertical, color, gradient, left, top, width, height, alpha, gradientOffset);
         this.draw2DBoxRect(matrices, left, bottom - vertical, corner, vertical, color, gradient, left, top, width, height, alpha, gradientOffset);
         this.draw2DBoxRect(matrices, right - corner, bottom - vertical, corner, vertical, color, gradient, left, top, width, height, alpha, gradientOffset);
      }
   }

   private void draw2DBoxRect(
      PoseStack matrices,
      float x,
      float y,
      float width,
      float height,
      Color fallbackColor,
      boolean gradient,
      float boxLeft,
      float boxTop,
      float boxWidth,
      float boxHeight,
      int alpha,
      int offset
   ) {
      if (!gradient) {
         Render2DEngine.drawRect(matrices, x, y, width, height, fallbackColor);
      } else {
         Render2DEngine.drawGradientRect(
            matrices,
            x,
            y,
            width,
            height,
            this.get2DGradientColor(x, y + height, boxLeft, boxTop, boxWidth, boxHeight, alpha, offset),
            this.get2DGradientColor(x + width, y + height, boxLeft, boxTop, boxWidth, boxHeight, alpha, offset),
            this.get2DGradientColor(x + width, y, boxLeft, boxTop, boxWidth, boxHeight, alpha, offset),
            this.get2DGradientColor(x, y, boxLeft, boxTop, boxWidth, boxHeight, alpha, offset)
         );
      }
   }

   private Color get2DGradientColor(float x, float y, float boxLeft, float boxTop, float boxWidth, float boxHeight, int alpha, int offset) {
      float xProgress = boxWidth <= 0.0F ? 0.0F : (x - boxLeft) / boxWidth;
      float yProgress = boxHeight <= 0.0F ? 0.0F : (y - boxTop) / boxHeight;
      float progress = Mth.clamp((xProgress + yProgress) * 0.5F, 0.0F, 1.0F);
      return Render2DEngine.injectAlpha(HudEditor.getColor(offset + Math.round(progress * 180.0F)), alpha);
   }

   private void drawFilledHitbox(PoseStack matrices, LivingEntity entity) {
      AABB box = this.getInterpolatedBox(entity);
      float alpha = this.getFadeAlpha(entity);
      if (!(alpha <= 0.0F)) {
         int bottomAlpha = Math.round(120.0F * alpha);
         int topAlpha = Math.round(30.0F * alpha);
         if (this.syncColor.getValue() && !this.isFriend(entity)) {
            int offset = entity.tickCount * 2;
            Render3DEngine.drawGradientFilledFadeBox(matrices, box, this.getBoxGradientColors(bottomAlpha, offset), this.getBoxGradientColors(topAlpha, offset));
         } else {
            Color baseColor = this.getEspColor(entity);
            Color bottomColor = Render2DEngine.injectAlpha(baseColor, bottomAlpha);
            Color topColor = Render2DEngine.injectAlpha(baseColor, topAlpha);
            Render3DEngine.drawFilledFadeBox(matrices, box, bottomColor, topColor);
         }
      }
   }

   private Color getEspColor(LivingEntity entity) {
      if (this.isFriend(entity)) {
         return FRIEND_COLOR;
      } else {
         return this.syncColor.getValue() ? HudEditor.getColor(entity.tickCount) : this.boxColor.getValue().getColorObject();
      }
   }

   private boolean isFriend(LivingEntity entity) {
      return entity instanceof Player player && Managers.FRIEND.isFriend(player);
   }

   private Color[] getBoxGradientColors(int alpha, int offset) {
      return new Color[]{
         this.getBoxGradientCorner(offset, alpha, true),
         this.getBoxGradientCorner(offset + 90, alpha, false),
         this.getBoxGradientCorner(offset + 180, alpha, true),
         this.getBoxGradientCorner(offset + 270, alpha, false)
      };
   }

   private Color getBoxGradientCorner(int offset, int alpha, boolean darken) {
      Color color = HudEditor.getColor(offset);
      if (darken) {
         color = Render2DEngine.darker(color, 0.35F);
      }

      return Render2DEngine.injectAlpha(color, alpha);
   }

   private float getFadeAlpha(LivingEntity entity) {
      float distance = this.fadeDistance.getValue();
      if (!(distance <= 0.0F) && mc.gameRenderer != null) {
         double cameraDistance = mc.gameRenderer.getMainCamera().position().distanceTo(entity.getEyePosition());
         return Mth.clamp((float)(cameraDistance / distance), 0.0F, 1.0F);
      } else {
         return 1.0F;
      }
   }

   private void drawHealthBar(PoseStack matrices, Vector4f rect, LivingEntity entity) {
      float barWidth = 1.2F;
      float left = rect.x - (barWidth + 2.0F);
      float top = rect.y;
      float height = rect.w - rect.y;
      Render2DEngine.drawRect(matrices, left, top, barWidth, height, HEALTH_BACKGROUND);
      float maxHealth = Math.max(entity.getMaxHealth(), 1.0F);
      float currentHealth = entity instanceof Player player && ModuleManager.fixHP.isEnabled()
         ? FixHP.getHealth(player)
         : entity.getHealth() + entity.getAbsorptionAmount();
      float health = Mth.clamp(currentHealth, 0.0F, maxHealth);
      float ratio = Mth.clamp(health / maxHealth, 0.0F, 1.0F);
      float filled = height * ratio;
      Render2DEngine.verticalGradient(matrices, left, top, left + barWidth, top + height, HEALTH_TOP_COLOR, HEALTH_BOTTOM_COLOR);
      if (ratio < 1.0F) {
         float missingHeight = height - filled;
         Render2DEngine.drawRect(matrices, left, top, barWidth, missingHeight, HEALTH_MISSING_OVERLAY);
      }
   }

   private void drawNameTag(GuiGraphics context, Vector4f rect, LivingEntity entity) {
      if (mc.player != null) {
         String name;
         if (entity instanceof Player player) {
            name = NameProtect.getDisplayName(player);
         } else {
            Component display = entity.getDisplayName();
            name = display != null ? display.getString() : "";
         }

         boolean showHealthText = this.shouldDrawNameTagHealth();
         float hpVal = showHealthText ? this.getDisplayHealth(entity) : 0.0F;
         float nameWidth = FontRenderers.sf_medium.getStringWidth(name);
         String openBracket = " [";
         String healthValueStr = Math.round(hpVal) + " HP";
         String closeBracket = "]";
         float openWidth = showHealthText ? FontRenderers.sf_medium.getStringWidth(openBracket) : 0.0F;
         float healthValueWidth = showHealthText ? FontRenderers.sf_medium.getStringWidth(healthValueStr) : 0.0F;
         float closeWidth = showHealthText ? FontRenderers.sf_medium.getStringWidth(closeBracket) : 0.0F;
         float textWidth = nameWidth + openWidth + healthValueWidth + closeWidth;
         float textHeight = FontRenderers.sf_medium.getStringHeight(name + (showHealthText ? openBracket + healthValueStr + closeBracket : ""));
         float paddingX = 4.0F;
         float paddingY = 0.8F;
         float bgWidth = textWidth + paddingX * 2.0F;
         float bgHeight = textHeight + paddingY * 2.0F;
         float centerX = (rect.x + rect.z) / 2.0F;
         float bgX = centerX - bgWidth / 2.0F;
         float bgY = rect.y - bgHeight - 2.0F;
         float bgCenterY = bgY + bgHeight / 2.0F;
         float scale = 0.75F;
         PoseStack matrices = Render2DEngine.toPoseStack(context.pose());
         matrices.pushPose();
         matrices.translate(centerX, bgCenterY, 0.0F);
         matrices.scale(scale, scale, 1.0F);
         matrices.translate(-centerX, -bgCenterY, 0.0F);
         Color fillColor = TAG_FILL_COLOR;
         if (entity instanceof Player player && Managers.FRIEND.isFriend(player)) {
            fillColor = FRIEND_COLOR;
         }

         Render2DEngine.drawRect(matrices, bgX, bgY, bgWidth, bgHeight, fillColor);
         float textX = bgX + (bgWidth - textWidth) / 2.0F;
         float textY = bgY + (bgHeight - textHeight) / 2.0F;
         matrices.pushPose();
         matrices.translate(0.0F, 3.0F, 0.0F);
         FontRenderers.sf_medium.drawString(matrices, name, textX, textY, TEXT_COLOR.getRGB());
         if (showHealthText) {
            float hpStartX = textX + nameWidth;
            FontRenderers.sf_medium.drawString(matrices, openBracket, hpStartX, textY, TEXT_COLOR.getRGB());
            FontRenderers.sf_medium.drawString(matrices, healthValueStr, hpStartX + openWidth, textY, this.getHealthColor(entity));
            FontRenderers.sf_medium.drawString(matrices, closeBracket, hpStartX + openWidth + healthValueWidth, textY, TEXT_COLOR.getRGB());
         }

         matrices.popPose();
         matrices.popPose();
      }
   }

   private boolean shouldDrawNameTagHealthBar() {
      return !FixHP.isActionBarModeActive();
   }

   private boolean shouldDrawNameTagHealth() {
      return !this.drawHpTag.getValue() ? false : !FixHP.isActionBarModeActive();
   }

   private float getDisplayHealth(LivingEntity entity) {
      return entity instanceof Player player && ModuleManager.fixHP.isEnabled()
         ? FixHP.getHealth(player)
         : entity.getHealth() + entity.getAbsorptionAmount();
   }

   private int getHealthColor(LivingEntity entity) {
      float maxHealth = Math.max(entity.getMaxHealth(), 1.0F);
      float current = this.getDisplayHealth(entity);
      current = Mth.clamp(current, 0.0F, maxHealth);
      float ratio = current / maxHealth;
      Color color = Render2DEngine.interpolateColorC(new Color(240, 20, 0), new Color(0, 240, 20), ratio);
      return color.getRGB();
   }

   private void drawArmorStrip(GuiGraphics context, Vector4f rect, Player player) {
      List<ItemStack> stacks = new ArrayList<>();
      stacks.add(player.getOffhandItem());
      for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
         stacks.add(player.getItemBySlot(slot));
      }
      stacks.add(player.getMainHandItem());
      stacks.removeIf(ItemStack::isEmpty);
      if (!stacks.isEmpty()) {
         float totalWidth = stacks.size() * 18.0F;
         float startX = rect.x + (rect.z - rect.x - totalWidth) / 2.0F;
         float y = rect.w + 1.0F;
         float scale = 0.55F;
         float centerX = rect.x + (rect.z - rect.x) / 2.0F;
         float centerY = y + 7.0F;
         context.pose().pushMatrix();
         context.pose().translate(centerX, centerY);
         context.pose().scale(scale, scale);
         context.pose().translate(-centerX, -centerY);

         for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            float x = startX + i * 18.0F;
            context.renderItem(stack, (int)x, (int)y);
            context.renderItemDecorations(mc.font, stack, (int)x, (int)y);
         }

         context.pose().popMatrix();
      }
   }

   @Nullable
   private Vector4f project(LivingEntity entity) {
      if (mc.getEntityRenderDispatcher() == null) {
         return null;
      }

      AABB box = this.getInterpolatedBox(entity);
      Vector4f bounds = null;

      for (int i = 0; i < 8; i++) {
         double x = (i & 1) == 0 ? box.minX : box.maxX;
         double y = (i & 2) == 0 ? box.minY : box.maxY;
         double z = (i & 4) == 0 ? box.minZ : box.maxZ;
         Vec3 screen = Render3DEngine.worldSpaceToScreenSpace(new Vec3(x, y, z));
         if (!Double.isNaN(screen.x) && !Double.isNaN(screen.y) && !(screen.z <= 0.0) && !(screen.z >= 1.0)) {
            if (bounds == null) {
               bounds = new Vector4f((float)screen.x, (float)screen.y, (float)screen.x, (float)screen.y);
            } else {
               bounds.x = Math.min(bounds.x, (float)screen.x);
               bounds.y = Math.min(bounds.y, (float)screen.y);
               bounds.z = Math.max(bounds.z, (float)screen.x);
               bounds.w = Math.max(bounds.w, (float)screen.y);
            }
         }
      }

      return bounds;
   }

   private AABB getInterpolatedBox(LivingEntity entity) {
      double interpolatedX = entity.xo + (entity.getX() - entity.xo) * Render3DEngine.getTickDelta();
      double interpolatedY = entity.yo + (entity.getY() - entity.yo) * Render3DEngine.getTickDelta();
      double interpolatedZ = entity.zo + (entity.getZ() - entity.zo) * Render3DEngine.getTickDelta();
      return HitBox.getBaseBoundingBox(entity).move(interpolatedX - entity.getX(), interpolatedY - entity.getY(), interpolatedZ - entity.getZ());
   }

   public boolean shouldHideVanillaNameTag(LivingEntity entity) {
      return !this.drawTag.getValue() || entity == null || mc.player == null ? false : this.isTargetValid(entity);
   }

   private boolean shouldRenderSelf() {
      return !this.showSelf.getValue() ? false : !this.isInFirstPersonView() || this.isFreeCamActive();
   }

   private boolean isInFirstPersonView() {
      return mc.options != null && mc.options.getCameraType().isFirstPerson();
   }

   private boolean isFreeCamActive() {
      return ModuleManager.freeCam != null && ModuleManager.freeCam.isEnabled();
   }

   public enum Mode {
      TwoD,
      Box,
      Off;

      @Override
      public String toString() {
         return this == TwoD ? "2D" : super.toString();
      }
   }
}
