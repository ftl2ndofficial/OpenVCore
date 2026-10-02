package vcore.features.hud.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import vcore.utility.render.RenderSystem;
import vcore.core.manager.client.ModuleManager;
import vcore.features.hud.HudElement;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.misc.FixHP;
import vcore.features.modules.misc.NameProtect;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.setting.impl.PositionSetting;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.animation.EaseOutBack;
import vcore.utility.render.animation.EaseOutCirc;

public class TargetHud extends HudElement {
   private static final float HUD_WIDTH = 137.0F;
   private static final float HUD_HEIGHT = 47.5F;
   private static final float CONTENT_X = 48.0F;
   private static final float HEALTH_BAR_Y = 32.0F;
   private static final float HEALTH_BAR_WIDTH = 85.0F;
   private static final float HEALTH_BAR_HEIGHT = 11.0F;
   private static final float ITEM_ROW_Y = -14.5F;
   private static final float ITEM_SCALE = 0.75F;
   private static final float ITEM_SPACING = 12.0F;
   private static final float NAME_Y = 9.5F;
   private static final float HP_TEXT_Y = 22.5F;
   private static final float NAME_RIGHT_PADDING = 8.0F;
   private static final float NAME_FADE_WIDTH = 4.0F;
   private static final float NAME_FADE_END_MARGIN = 1.75F;
   public EaseOutBack animation = new EaseOutBack();
   public static EaseOutCirc healthAnimation = new EaseOutCirc();
   private boolean direction = false;
   private LivingEntity target;

   public TargetHud(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("TargetHud", 150, 50, position, activeElementsSupplier);
   }

   @Override
   public void tick() {
      this.animation.update(this.direction);
      healthAnimation.update();
   }

   @Override
   public void render(GuiGraphics context) {
      super.render(context);
      this.getTarget();
      if (this.target != null) {
         float totalHealth = this.getHealth();
         float health = Math.min(this.target.getMaxHealth(), totalHealth);
         if (this.animation.getAnimationd() > 0.0) {
            float animationFactor = (float)MathUtility.clamp(this.animation.getAnimationd(), 0.0, 1.0);
            this.renderNurik(context, health, totalHealth, animationFactor);
         }
      }
   }

   private void getTarget() {
      if (Aura.target != null) {
         if (Aura.target instanceof LivingEntity) {
            this.target = (LivingEntity)Aura.target;
            this.direction = true;
         } else {
            this.target = null;
            this.direction = false;
         }
      } else if (mc.screen instanceof ChatScreen) {
         this.target = mc.player;
         this.direction = true;
      } else {
         this.direction = false;
         if (this.animation.getAnimationd() < 0.02) {
            this.target = null;
         }
      }
   }

   private void renderNurik(GuiGraphics context, float health, float totalHealth, float animationFactor) {
      float hurtPercent = Render2DEngine.interpolateFloat(
            MathUtility.clamp(this.target.hurtTime == 0 ? 0 : this.target.hurtTime + 1, 0, 10), this.target.hurtTime, Render3DEngine.getTickDelta()
         )
         / 8.0F;
      healthAnimation.setValue(health);
      health = (float)healthAnimation.getAnimationD();
      PoseStack matrices = Render2DEngine.toPoseStack(context.pose());
      Render2DEngine.drawHudBase2(matrices, this.getPosX(), this.getPosY(), 137.0F, 47.5F, 9.0F, 5.0F, HudEditor.getBlurOpacity(), animationFactor);
      this.setBounds(this.getPosX(), this.getPosY(), 137.0F, 47.5F);
      float avatarSize = 39.0F;
      float avatarX = this.getPosX() + 4.25F;
      float avatarY = this.getPosY() + (47.5F - avatarSize) / 2.0F;
      float avatarRadius = 6.5F;

      matrices.pushPose();
      matrices.translate(avatarX + avatarSize / 2.0F, avatarY + avatarSize / 2.0F, 0.0F);
      matrices.scale(1.0F - hurtPercent / 15.0F, 1.0F - hurtPercent / 15.0F, 1.0F);
      matrices.translate(-(avatarX + avatarSize / 2.0F), -(avatarY + avatarSize / 2.0F), 0.0F);

      // Rounded avatar background box
      Render2DEngine.drawRound(
         matrices,
         avatarX,
         avatarY,
         avatarSize,
         avatarSize,
         avatarRadius,
         new Color(20, 20, 20, (int)(220 * animationFactor))
      );

      // Determine skin texture
      Identifier skin;
      boolean isPlayerSkin = true;
      if (this.target instanceof AbstractClientPlayer player) {
         skin = player.getSkin().body().texturePath();
      } else if (this.target instanceof Player player) {
         skin = DefaultPlayerSkin.get(player.getUUID()).body().texturePath();
      } else {
         skin = DefaultPlayerSkin.getDefaultTexture();
      }

      if (isPlayerSkin) {
         // Head base layer
         Render2DEngine.renderRoundedTexture(
            matrices,
            skin,
            avatarX,
            avatarY,
            avatarSize,
            avatarSize,
            avatarRadius,
            8.0F / 64.0F,
            8.0F / 64.0F,
            16.0F / 64.0F,
            16.0F / 64.0F,
            animationFactor
         );
         // Hat/outer layer
         Render2DEngine.renderRoundedTexture(
            matrices,
            skin,
            avatarX,
            avatarY,
            avatarSize,
            avatarSize,
            avatarRadius,
            40.0F / 64.0F,
            8.0F / 64.0F,
            48.0F / 64.0F,
            16.0F / 64.0F,
            animationFactor
         );
      } else {
         Render2DEngine.renderRoundedTexture(
            matrices,
            skin,
            avatarX,
            avatarY,
            avatarSize,
            avatarSize,
            avatarRadius,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            animationFactor
         );
      }

      // Hurt overlay
      if (hurtPercent > 0.05F) {
         int hurtAlpha = (int)(MathUtility.clamp(hurtPercent * 160.0F, 0.0F, 180.0F) * animationFactor);
         Render2DEngine.drawRound(matrices, avatarX, avatarY, avatarSize, avatarSize, avatarRadius, new Color(255, 50, 50, hurtAlpha));
      }

      // Rounded border outline around avatar
      Render2DEngine.drawRoundOutline(
         matrices,
         avatarX,
         avatarY,
         avatarSize,
         avatarSize,
         avatarRadius,
         new Color(255, 255, 255, (int)(45 * animationFactor))
      );

      matrices.popPose();
      Render2DEngine.drawRect(matrices, this.getPosX() + 48.0F, this.getPosY() + 32.0F, 85.0F, 11.0F, 4.0F, 0.15F * animationFactor);
      Render2DEngine.drawRect(
         matrices,
         this.getPosX() + 48.0F,
         this.getPosY() + 32.0F,
         MathUtility.clamp(85.0F * (health / this.target.getMaxHealth()), 8.0F, 85.0F),
         11.0F,
         4.0F,
         animationFactor
      );
      this.renderTargetName(context, animationFactor);
      FontRenderers.inter_target_hp
         .drawString(
            context.pose(),
            this.getHealthDisplayText(totalHealth, this.getAbsorptionHealth()),
            this.getPosX() + 48.0F,
            this.getPosY() + 22.5F,
            Render2DEngine.applyOpacity(Color.WHITE.getRGB(), animationFactor)
         );
      if (this.target instanceof Player) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float)MathUtility.clamp(this.animation.getAnimationd(), 0.0, 1.0));
         ItemStack[] items = this.getTargetItems((Player)this.target);
         float rowWidth = (items.length - 1) * 12.0F + 12.0F;
         float xItemOffset = this.getPosX() + 48.0F + (85.0F - rowWidth) / 2.0F + 2.0F;

         for (ItemStack itemStack : items) {
            context.pose().pushMatrix();
            context.pose().translate(xItemOffset, this.getPosY() + -14.5F);
            context.pose().scale(0.75F, 0.75F);
            context.renderItem(itemStack, 0, 0);
            context.renderItemDecorations(mc.font, itemStack, 0, 0);
            context.pose().popMatrix();
            xItemOffset += 12.0F;
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public float getHealth() {
      return this.getBaseHealth() + this.getAbsorptionHealth();
   }

   private float getBaseHealth() {
      return this.target instanceof Player ent && ModuleManager.fixHP.isEnabled() ? FixHP.getHealth(ent) : this.target.getHealth();
   }

   private float getAbsorptionHealth() {
      return this.target == null ? 0.0F : this.target.getAbsorptionAmount();
   }

   private String getTargetName() {
      return ModuleManager.nameProtect.isEnabled() && this.target == mc.player ? NameProtect.getCustomName() : this.target.getName().getString();
   }

   private void renderTargetName(GuiGraphics context, float animationFactor) {
      String targetName = this.getTargetName();
      float textX = this.getPosX() + 48.0F;
      float textY = this.getPosY() + 9.5F;
      float maxNameWidth = 81.0F;
      float clipTop = textY - 1.0F;
      float clipBottom = clipTop + FontRenderers.inter_target_name.getStringHeight(targetName) + 4.0F;
      float clipRight = textX + maxNameWidth;
      PoseStack matrices = Render2DEngine.toPoseStack(context.pose());
      Render2DEngine.addWindow(matrices, new Render2DEngine.Rectangle(textX, clipTop, clipRight, clipBottom));
      FontRenderers.inter_target_name
         .drawStringWithHorizontalFade(
            context.pose(),
            targetName,
            textX,
            textY,
            Render2DEngine.applyOpacity(Color.WHITE.getRGB(), animationFactor),
            Math.max(0.0F, maxNameWidth - 1.75F),
            4.0F
         );
      Render2DEngine.popWindow();
   }

   private ItemStack[] getTargetItems(Player player) {
      ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
      ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
      ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
      ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
      ItemStack[] items = new ItemStack[]{player.getMainHandItem(), head, chest, legs, feet, player.getOffhandItem()};
      ItemStack[] packedItems = new ItemStack[items.length];

      for (int i = 0; i < packedItems.length; i++) {
         packedItems[i] = ItemStack.EMPTY;
      }

      int writeIndex = packedItems.length - 1;

      for (int i = items.length - 1; i >= 0; i--) {
         ItemStack itemStack = items[i];
         if (!itemStack.isEmpty()) {
            packedItems[writeIndex--] = itemStack;
         }
      }

      return packedItems;
   }

   private String getHealthDisplayText(float totalHealth, float absorptionHealth) {
      String healthText = "HP: " + this.formatHealthValue(totalHealth);
      if (absorptionHealth > 0.0F) {
         healthText = healthText + " - " + this.formatHealthValue(absorptionHealth) + " AB";
      }

      return healthText;
   }

   private String formatHealthValue(float value) {
      return String.format(Locale.US, "%.1f", value);
   }

   public static void sizeAnimation(PoseStack matrixStack, double width, double height, double animation) {
      matrixStack.translate(width, height, 0.0);
      matrixStack.scale((float)animation, (float)animation, 1.0F);
      matrixStack.translate(-width, -height, 0.0);
   }
}
