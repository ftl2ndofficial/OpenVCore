package vcore.features.modules.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlStateManager;
import vcore.utility.render.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Axis;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.VcoreUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.TextureStorage;

public class JumpCircle extends Module {
   private final Setting<JumpCircle.Mode> mode = new Setting<>("Mode", JumpCircle.Mode.Default);
   private final Setting<Boolean> easeOut = new Setting<>("EaseOut", true);
   private final Setting<Float> rotateSpeed = new Setting<>("RotateSpeed", 2.0F, 0.5F, 5.0F);
   private final Setting<Float> circleScale = new Setting<>("CircleScale", 1.0F, 0.5F, 5.0F);
   private final Setting<Boolean> onlySelf = new Setting<>("OnlySelf", true);
   private static final float GLOW_BOOST_ALPHA = 0.65F;
   private final List<UUID> cache = new CopyOnWriteArrayList<>();
   private final List<JumpCircle.Circle> circles = new CopyOnWriteArrayList<>();
   private Identifier custom;
   private int lastWorldId = -1;

   public JumpCircle() {
      super("JumpCircle", "Circle effect on jump.", Module.Category.RENDER);
   }

   @Override
   public void onEnable() {
      try {
         this.custom = VcoreUtility.getCustomImg("circle");
      } catch (Exception e) {
         this.sendMessage(e.getMessage());
      }

      this.lastWorldId = this.getWorldId();
      this.circles.clear();
      this.cache.clear();
   }

   @Override
   public void onUpdate() {
      if (mc.player != null && mc.level != null) {
         int worldId = this.getWorldId();
         if (worldId != this.lastWorldId) {
            this.circles.clear();
            this.cache.clear();
            this.lastWorldId = worldId;
         }

         if (this.mode.is(JumpCircle.Mode.Custom) && this.custom == null) {
            try {
               this.custom = VcoreUtility.getCustomImg("circle");
            } catch (Exception e) {
               this.sendMessage(".minecraft -> Vcore -> misc -> images -> circle.png");
            }
         }

         for (Player pl : mc.level.players()) {
            if (!this.cache.contains(pl.getUUID()) && pl.onGround() && (mc.player == pl || !this.onlySelf.getValue())) {
               this.cache.add(pl.getUUID());
            }
         }

         for (UUID uuid : new ArrayList<>(this.cache)) {
            Player pl = mc.level.getPlayerByUUID(uuid);
            if (pl != null && !pl.onGround()) {
               this.circles.add(new JumpCircle.Circle(new Vec3(pl.getX(), (int)Math.floor(pl.getY()) + 0.001F, pl.getZ()), new Timer()));
               this.cache.remove(uuid);
            }
         }

         this.circles.removeIf(c -> c.timer.passedMs(this.easeOut.getValue() ? 5000L : 6000L));
      } else {
         this.cache.clear();
         this.circles.clear();
         this.lastWorldId = -1;
      }
   }

   private int getWorldId() {
      return mc.level == null ? -1 : mc.level.dimension().identifier().hashCode();
   }

   @Override
   public void onRender3D(PoseStack stack) {
      if (fullNullCheck() || this.circles.isEmpty()) {
         return;
      }

      Vec3 camPos = Render3DEngine.getCameraPos();
      List<JumpCircle.Circle> renderCircles = new ArrayList<>(this.circles);
      Collections.reverse(renderCircles);
      try {
         RenderSystem.disableDepthTest();
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
         switch ((JumpCircle.Mode)this.mode.getValue()) {
            case Default:
               RenderSystem.setShaderTexture(0, TextureStorage.defaultCircle);
               break;
            case Portal:
               RenderSystem.setShaderTexture(0, TextureStorage.bubble);
               break;
            case Custom:
               RenderSystem.setShaderTexture(0, Objects.requireNonNullElse(this.custom, TextureStorage.defaultCircle));
         }

         BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

         for (JumpCircle.Circle c : renderCircles) {
            float colorAnim = (float)c.timer.getPassedTimeMs() / 6000.0F;
            float sizeAnim = this.circleScale.getValue()
               - (float)Math.pow(1.0F - (float)c.timer.getPassedTimeMs() * (this.easeOut.getValue() ? 2.0F : 1.0F) / 5000.0F, 4.0);
            stack.pushPose();
            stack.translate(
               c.pos().x - camPos.x,
               c.pos().y - camPos.y,
               c.pos().z - camPos.z
            );
            stack.mulPose(Axis.XP.rotationDegrees(90.0F));
            stack.mulPose(Axis.ZP.rotationDegrees(sizeAnim * this.rotateSpeed.getValue() * 1000.0F));
            Matrix4f matrix = stack.last().pose();
            float alpha = 1.0F - colorAnim;
            this.drawCircleLayer(buffer, matrix, sizeAnim, alpha * 0.65F);
            this.drawCircleLayer(buffer, matrix, sizeAnim, alpha);
            stack.popPose();
         }

         Render2DEngine.endBuilding(buffer);
      } finally {
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.enableDepthTest();
         com.mojang.blaze3d.vertex.BufferUploader.invalidateCommandEncoder();
         try {
            org.lwjgl.opengl.GL33.glBindSampler(0, 0);
         } catch (Throwable ignored) {}
         Collections.reverse(this.circles);
      }
   }

   private void drawCircleLayer(@NotNull BufferBuilder buffer, @NotNull Matrix4f matrix, float size, float alpha) {
      float scale = size * 2.0F;
      buffer.addVertex(matrix, -size, -size + scale, 0.0F).setUv(0.0F, 1.0F).setColor(Render2DEngine.applyOpacity(HudEditor.getColor(270), alpha).getRGB());
      buffer.addVertex(matrix, -size + scale, -size + scale, 0.0F).setUv(1.0F, 1.0F).setColor(Render2DEngine.applyOpacity(HudEditor.getColor(0), alpha).getRGB());
      buffer.addVertex(matrix, -size + scale, -size, 0.0F).setUv(1.0F, 0.0F).setColor(Render2DEngine.applyOpacity(HudEditor.getColor(180), alpha).getRGB());
      buffer.addVertex(matrix, -size, -size, 0.0F).setUv(0.0F, 0.0F).setColor(Render2DEngine.applyOpacity(HudEditor.getColor(90), alpha).getRGB());
   }

   public record Circle(Vec3 pos, Timer timer) {
   }

   public enum Mode {
      Default,
      Portal,
      Custom;
   }
}
