package vcore.features.modules.render;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.AntiBot;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.animation.AnimationUtility;

public class Arrows extends Module {
   private static final boolean GLOW = false;
   private static final float HEIGHT = 1.2F;
   private static final boolean DOWN = true;
   private static final float DOWN_HEIGHT = 3.63F;
   private static final float TRACER_WIDTH = 0.0F;
   private static final int TRACER_RADIUS = 80;
   private static final int PITCH_LOCK = 42;
   private static final Color FRIEND_COLOR = new Color(59392);
   private static final Color TRACER_COLOR = new Color(16711680);
   private float smoothYaw = 0.0F;

   public Arrows() {
      super("Arrows", "Directional arrows to other players.", Module.Category.RENDER);
   }

   @Override
   public void onRender2D(GuiGraphics context) {
      if (!fullNullCheck()) {
         float middleW = ModuleManager.crosshair.getAnimatedPosX();
         float middleH = ModuleManager.crosshair.getAnimatedPosY();
         int color = 0;
         PoseStack poseStack = Render2DEngine.toPoseStack(context.pose());
         poseStack.pushPose();
         poseStack.translate(middleW, middleH, 0.0F);
         poseStack
            .mulPose(Axis.XP.rotationDegrees(90.0F / Math.abs(90.0F / MathUtility.clamp(mc.player.getXRot(), 42.0F, 90.0F)) - 102.0F));
         poseStack.translate(-middleW, -middleH, 0.0F);
         this.smoothYaw = AnimationUtility.fast(this.smoothYaw, mc.player.getYRot(), 13.0F);

         for (Player e : Lists.newArrayList(mc.level.players())) {
            if (e != mc.player && (!ModuleManager.antiBot.isEnabled() || ModuleManager.antiBot.mode.getValue() != AntiBot.Mode.Matrix || !AntiBot.isBot(e))) {
               poseStack.pushPose();
               float yaw = this.getRotations(e) - this.smoothYaw;
               poseStack.translate(middleW, middleH, 0.0F);
               poseStack.mulPose(Axis.ZP.rotationDegrees(yaw));
               poseStack.translate(-middleW, -middleH, 0.0F);
               if (Managers.FRIEND.isFriend(e)) {
                  color = FRIEND_COLOR.getRGB();
               } else {
                  color = TRACER_COLOR.getRGB();
               }

               Render2DEngine.drawTracerPointer(poseStack, middleW, middleH - 80.0F, 6.0F, 0.0F, 3.63F, true, false, color);
               poseStack.translate(middleW, middleH, 0.0F);
               poseStack.mulPose(Axis.ZP.rotationDegrees(-yaw));
               poseStack.translate(-middleW, -middleH, 0.0F);
               poseStack.popPose();
            }
         }

         poseStack.popPose();
      }
   }

   private float getRotations(Player entity) {
      if (mc.player == null) {
         return 0.0F;
      }

      double x = this.interp(entity.position().x, entity.xo) - this.interp(mc.player.position().x, mc.player.xo);
      double z = this.interp(entity.position().z, entity.zo) - this.interp(mc.player.position().z, mc.player.zo);
      return (float)(-(Math.atan2(x, z) * (180.0 / Math.PI)));
   }

   private double interp(double current, double previous) {
      return previous + (current - previous) * Render3DEngine.getTickDelta();
   }
}
