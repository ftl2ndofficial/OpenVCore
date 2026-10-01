package vcore.features.modules.render;

import org.lwjgl.opengl.GL11;
import vcore.utility.render.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import vcore.Vcore;
import vcore.events.impl.EventAttack;
import vcore.events.impl.EventDeath;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.render.frageffects.FragCharmSfx;
import vcore.features.modules.render.frageffects.FragDeathMemory;
import vcore.features.modules.render.frageffects.FragEffectMath;
import vcore.features.modules.render.frageffects.FragParticleRenderer;
import vcore.utility.Timer;
import vcore.utility.render.Render2DEngine;

public final class FragEffects extends Module {
   private static final Identifier VIGNETTE_TEXTURE = Identifier.fromNamespaceAndPath("vcore", "textures/effects/lightarroundscreen.png");
   private static final long EFFECT_MS_MAX = 2800L;
   private static final int COLOR_5 = -65536;
   private static final int COLOR_6 = -1;
   private final FragCharmSfx charmSfx;
   private final FragDeathMemory deathMemory;
   private final FragParticleRenderer particles;
   private final Timer linearTimer = new Timer();
   private float[] timerValues = new float[0];
   private boolean animate;
   private int ticksForEditTimer;

   public FragEffects() {
      super("FragEffects", "Relake frag kill feedback effects.", Module.Category.RENDER);
      this.charmSfx = FragCharmSfx.create();
      this.deathMemory = FragDeathMemory.create(950L, this::initWorldEffectOnUpdate, this::shouldRotateToKilled);
      this.particles = FragParticleRenderer.create();
      this.refillTimerValues(0.125F, 0.175F, 0.3F, 0.65F, 0.8F);
   }

   @Override
   public void onDisable() {
      this.clearRuntimeState();
   }

   @Override
   public void onLogout() {
      this.clearRuntimeState();
   }

   @Override
   public void onUpdate() {
      if (mc.player != null && mc.level != null) {
         this.updateTickTimerValues();
         this.updateTargetMemories();
         this.deathMemory.updateAutoMemories();
         this.deathMemory.removeAutoMemories();
         this.particles.updateParticlesList();
      } else {
         this.clearRuntimeState();
      }
   }

   @Override
   public void onRender3D(PoseStack matrices) {
      if (mc.player != null && mc.level != null) {
         this.particles.renderParticles(matrices);
      }
   }

   @Override
   public void onRender2D(GuiGraphics context) {
      this.drawVignetteTextureIfEffectMoment(context);
      this.charmSfx.inRenderThreadUpdate();
   }

   @EventHandler
   private void onAttackEntity(EventAttack event) {
      if (event != null && event.getEntity() != null) {
         if (event.getEntity() instanceof LivingEntity living && living.isAlive()) {
            this.deathMemory.controllingAddingMemoryToEntity(living, this.mobDetect());
         }
      }
   }

   @EventHandler
   private void onDeathEntity(EventDeath event) {
      if (event != null && event.getPlayer() != null) {
         this.deathMemory.onContains(event.getPlayer());
      }
   }

   public float getMulCameraZoomValue() {
      if (this.isEnabled() && mc.options.getCameraType().isFirstPerson()) {
         float effectPC = Math.min(this.getEffectPC() * 3.0F, 1.0F);
         if (effectPC > 0.0F) {
            float input = (effectPC > 0.5F ? 1.0F - effectPC : effectPC) * 2.0F;
            float value = (float)(effectPC > 0.5F ? FragEffectMath.easeExpoIn(input) : FragEffectMath.easeBackInOut(input));
            return 1.0F + FragEffectMath.lerp(value, value * value, 0.3F) / 1.25F;
         }
      }

      return 1.0F;
   }

   private boolean isWasTargeted(LivingEntity living) {
      return living != null && living == Aura.target;
   }

   private boolean mobDetect() {
      return true;
   }

   private boolean shouldRotateToKilled() {
      return true;
   }

   private void updateTargetMemories() {
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof LivingEntity living && this.isWasTargeted(living)) {
            this.deathMemory.controllingAddingMemoryToEntity(living, this.mobDetect());
         }
      }
   }

   private void initWorldEffectOnUpdate() {
      this.charmSfx.sfxPulseDistantRand(0L);
      this.charmSfx.sfxKnockMain(75L);
      this.charmSfx.sfxSparksBlockHit(150L);
      this.charmSfx.sfxEchoMain(225L);
      Vcore.FRAG_EFFECT_TIMER = 0.07F;
      this.restartEffect();
   }

   private void restartEffect() {
      this.animate = true;
      this.linearTimer.reset();
      this.ticksForEditTimer = 0;
   }

   private float getEffectPC() {
      if (!this.animate) {
         return 0.0F;
      }

      float value = Math.min((float)this.linearTimer.getPassedTimeMs() / 2800.0F, 1.0F);
      if (value >= 1.0F) {
         this.animate = false;
         Vcore.FRAG_EFFECT_TIMER = 1.0F;
      }

      return value;
   }

   private void refillTimerValues(float... values) {
      this.timerValues = values;
   }

   private void updateTickTimerValues() {
      if (!this.animate) {
         Vcore.FRAG_EFFECT_TIMER = 1.0F;
      } else {
         this.ticksForEditTimer++;
         if (this.ticksForEditTimer < this.timerValues.length) {
            Vcore.FRAG_EFFECT_TIMER = this.timerValues[this.ticksForEditTimer];
         } else if (this.linearTimer.getPassedTimeMs() > 300L) {
            Vcore.FRAG_EFFECT_TIMER = 1.0F;
         }
      }
   }

   private void drawVignetteTextureIfEffectMoment(GuiGraphics context) {
      float effectPC = this.getEffectPC();
      if (effectPC != 0.0F) {
         float alphaPC = (float)FragEffectMath.easeQuintOut(effectPC);
         float scalePC = Math.min((alphaPC > 0.5F ? 1.0F - alphaPC : alphaPC) * 4.25F, 1.0F);
         float width = mc.getWindow().getGuiScaledWidth();
         float height = mc.getWindow().getGuiScaledHeight();
         float extPCX = width / 2.0F * (1.0F - scalePC);
         float extPCY = height / 2.0F * (1.0F - scalePC);
         float x = -extPCX;
         float y = -extPCY;
         float x2 = width + extPCX;
         float y2 = height + extPCY;
         int color = FragEffectMath.multAlpha(FragEffectMath.getOverallColorFrom(-65536, -1, effectPC), alphaPC);
         Color renderColor = new Color(color, true);
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
         RenderSystem.setShaderTexture(0, VIGNETTE_TEXTURE);
         BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         Matrix4f matrix = Render2DEngine.toPoseStack(context.pose()).last().pose();

         for (int i = 0; i < 4; i++) {
            buffer.addVertex(matrix, x, y2, 0.0F).setUv(0.0F, 1.0F).setColor(renderColor.getRGB());
            buffer.addVertex(matrix, x2, y2, 0.0F).setUv(1.0F, 1.0F).setColor(renderColor.getRGB());
            buffer.addVertex(matrix, x2, y, 0.0F).setUv(1.0F, 0.0F).setColor(renderColor.getRGB());
            buffer.addVertex(matrix, x, y, 0.0F).setUv(0.0F, 0.0F).setColor(renderColor.getRGB());
         }

         Render2DEngine.endBuilding(buffer);
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void clearRuntimeState() {
      this.animate = false;
      this.ticksForEditTimer = 0;
      Vcore.FRAG_EFFECT_TIMER = 1.0F;
      this.deathMemory.clear();
      this.particles.clear();
      this.charmSfx.clear();
   }
}
