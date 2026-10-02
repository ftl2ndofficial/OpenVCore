package vcore.utility.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import vcore.Vcore;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.RenderSystem.DestFactor;
import vcore.utility.render.RenderSystem.SourceFactor;

public class Render3DEngine {
   public static List<Render3DEngine.FillAction> FILLED_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.OutlineAction> OUTLINE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.FadeAction> FADE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.GradientFadeAction> GRADIENT_FADE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.GradientOutlineAction> GRADIENT_OUTLINE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.FillSideAction> FILLED_SIDE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.OutlineSideAction> OUTLINE_SIDE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.DebugLineAction> DEBUG_LINE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.LineAction> LINE_QUEUE = new ArrayList<>();
   public static final Matrix4f lastProjMat = new Matrix4f();
   public static final Matrix4f lastModMat = new Matrix4f();
   public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();
   public static boolean rendering3D = false;

   public static Vec3 getCameraPos() {
      if (Module.mc.getEntityRenderDispatcher().camera != null) {
         return Module.mc.getEntityRenderDispatcher().camera.position();
      }
      return Module.mc.gameRenderer.getMainCamera().position();
   }

   public static void onRender3D(PoseStack stack) {
      if (!FILLED_QUEUE.isEmpty() || !FILLED_SIDE_QUEUE.isEmpty()) {
         Tesselator tessellator = Tesselator.getInstance();
         BufferBuilder bufferBuilder = tessellator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         RenderSystem.disableDepthTest();
         setupRender();
         FILLED_QUEUE.forEach(action -> setFilledBoxVertexes(bufferBuilder, stack.last().pose(), action.box(), action.color()));
         FILLED_SIDE_QUEUE.forEach(action -> setFilledSidePoints(bufferBuilder, stack.last().pose(), action.box, action.color(), action.side()));
         Render2DEngine.endBuilding(bufferBuilder);
         endRender();
         RenderSystem.enableDepthTest();
         FILLED_SIDE_QUEUE.clear();
         FILLED_QUEUE.clear();
      }

      if (!FADE_QUEUE.isEmpty() || !GRADIENT_FADE_QUEUE.isEmpty()) {
         Tesselator tessellator = Tesselator.getInstance();
         BufferBuilder bufferBuilder = tessellator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         RenderSystem.disableDepthTest();
         RenderSystem.disableCull();
         setupRender();
         FADE_QUEUE.forEach(action -> setFilledFadePoints(action.box(), bufferBuilder, stack.last().pose(), action.color(), action.color2()));
         GRADIENT_FADE_QUEUE.forEach(
            action -> setGradientFilledFadePoints(action.box(), bufferBuilder, stack.last().pose(), action.bottomColors(), action.topColors())
         );
         Render2DEngine.endBuilding(bufferBuilder);
         RenderSystem.enableCull();
         endRender();
         RenderSystem.enableDepthTest();
         FADE_QUEUE.clear();
         GRADIENT_FADE_QUEUE.clear();
      }

      if (!OUTLINE_QUEUE.isEmpty() || !OUTLINE_SIDE_QUEUE.isEmpty() || !GRADIENT_OUTLINE_QUEUE.isEmpty()) {
         setupRender();
         Tesselator tessellator = Tesselator.getInstance();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         Set<Float> lineWidths = new LinkedHashSet<>();
         OUTLINE_QUEUE.forEach(action -> lineWidths.add(action.lineWidth()));
         OUTLINE_SIDE_QUEUE.forEach(action -> lineWidths.add(action.lineWidth()));
         GRADIENT_OUTLINE_QUEUE.forEach(action -> lineWidths.add(action.lineWidth()));

         for (float lineWidth : lineWidths) {
            BufferBuilder buffer = tessellator.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            RenderSystem.lineWidth(lineWidth);
            OUTLINE_QUEUE.forEach(action -> {
               if (Float.compare(action.lineWidth(), lineWidth) == 0) {
                  setOutlinePoints(action.box(), matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color());
               }
            });
            OUTLINE_SIDE_QUEUE.forEach(action -> {
               if (Float.compare(action.lineWidth(), lineWidth) == 0) {
                  setSideOutlinePoints(action.box, matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color(), action.side());
               }
            });
            GRADIENT_OUTLINE_QUEUE.forEach(
               action -> {
                  if (Float.compare(action.lineWidth(), lineWidth) == 0) {
                     setGradientDashedOutlinePoints(
                        action.box(),
                        matrixFrom(action.box().minX, action.box().minY, action.box().minZ),
                        buffer,
                        action.colors(),
                        action.dashLength(),
                        action.gapLength()
                     );
                  }
               }
            );
            Render2DEngine.endBuilding(buffer);
         }

         RenderSystem.enableCull();
         RenderSystem.lineWidth(1.0F);
         RenderSystem.enableDepthTest();
         endRender();
         OUTLINE_QUEUE.clear();
         OUTLINE_SIDE_QUEUE.clear();
         GRADIENT_OUTLINE_QUEUE.clear();
      }

      if (!DEBUG_LINE_QUEUE.isEmpty()) {
         setupRender();
         RenderSystem.disableDepthTest();
         Tesselator tessellator = Tesselator.getInstance();
         BufferBuilder buffer = tessellator.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         RenderSystem.disableCull();
         DEBUG_LINE_QUEUE.forEach(
            action -> {
               PoseStack matrices = matrixFrom(action.start.x(), action.start.y(), action.start.z());
               vertexLine(
                  matrices,
                  buffer,
                  0.0F,
                  0.0F,
                  0.0F,
                  (float)(action.end.x() - action.start.x()),
                  (float)(action.end.y() - action.start.y()),
                  (float)(action.end.z() - action.start.z()),
                  action.color
               );
            }
         );
         Render2DEngine.endBuilding(buffer);
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         endRender();
         DEBUG_LINE_QUEUE.clear();
      }

      if (!LINE_QUEUE.isEmpty()) {
         setupRender();
         Tesselator tessellator = Tesselator.getInstance();
         RenderSystem.disableCull();
         RenderSystem.lineWidth(2.5F);
         RenderSystem.disableDepthTest();
         BufferBuilder buffer = tessellator.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         LINE_QUEUE.forEach(
            action -> {
               PoseStack matrices = matrixFrom(action.start.x(), action.start.y(), action.start.z());
               vertexLine(
                  matrices,
                  buffer,
                  0.0F,
                  0.0F,
                  0.0F,
                  (float)(action.end.x() - action.start.x()),
                  (float)(action.end.y() - action.start.y()),
                  (float)(action.end.z() - action.start.z()),
                  action.color
               );
            }
         );
         Render2DEngine.endBuilding(buffer);
         RenderSystem.enableCull();
         RenderSystem.lineWidth(1.0F);
         RenderSystem.enableDepthTest();
         endRender();
         LINE_QUEUE.clear();
      }
   }

   @Deprecated
   public static void drawFilledBox(PoseStack stack, AABB box, Color c) {
      FILLED_QUEUE.add(new Render3DEngine.FillAction(box, c));
   }

   public static void setFilledBoxVertexes(@NotNull BufferBuilder bufferBuilder, Matrix4f m, @NotNull AABB box, @NotNull Color c) {
      Vec3 camPos = getCameraPos();
      float minX = (float)(box.minX - camPos.x);
      float minY = (float)(box.minY - camPos.y);
      float minZ = (float)(box.minZ - camPos.z);
      float maxX = (float)(box.maxX - camPos.x);
      float maxY = (float)(box.maxY - camPos.y);
      float maxZ = (float)(box.maxZ - camPos.z);
      bufferBuilder.addVertex(m, minX, minY, minZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, minY, minZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, minY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, minX, minY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, minX, minY, minZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, minX, maxY, minZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, maxY, minZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, minY, minZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, minY, minZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, maxY, minZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, maxY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, minY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, minX, minY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, minY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, maxY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, minX, maxY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, minX, minY, minZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, minX, minY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, minX, maxY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, minX, maxY, minZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, minX, maxY, minZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, minX, maxY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, maxY, maxZ).setColor(c.getRGB());
      bufferBuilder.addVertex(m, maxX, maxY, minZ).setColor(c.getRGB());
   }

   @NotNull
   public static AABB interpolateBox(@NotNull AABB from, @NotNull AABB to, float delta) {
      double X = Render2DEngine.interpolate(from.maxX, to.maxX, delta);
      double Y = Render2DEngine.interpolate(from.maxY, to.maxY, delta);
      double Z = Render2DEngine.interpolate(from.maxZ, to.maxZ, delta);
      double X1 = Render2DEngine.interpolate(from.minX, to.minX, delta);
      double Y1 = Render2DEngine.interpolate(from.minY, to.minY, delta);
      double Z1 = Render2DEngine.interpolate(from.minZ, to.minZ, delta);
      return new AABB(X1, Y1, Z1, X, Y, Z);
   }

   @Deprecated
   public static void drawFilledSide(PoseStack stack, @NotNull AABB box, Color c, Direction dir) {
      FILLED_SIDE_QUEUE.add(new Render3DEngine.FillSideAction(box, c, dir));
   }

   public static void setFilledSidePoints(BufferBuilder buffer, Matrix4f matrix, AABB box, Color c, Direction dir) {
      Vec3 camPos = getCameraPos();
      float minX = (float)(box.minX - camPos.x);
      float minY = (float)(box.minY - camPos.y);
      float minZ = (float)(box.minZ - camPos.z);
      float maxX = (float)(box.maxX - camPos.x);
      float maxY = (float)(box.maxY - camPos.y);
      float maxZ = (float)(box.maxZ - camPos.z);
      if (dir == Direction.DOWN) {
         buffer.addVertex(matrix, minX, minY, minZ).setColor(c.getRGB());
         buffer.addVertex(matrix, maxX, minY, minZ).setColor(c.getRGB());
         buffer.addVertex(matrix, maxX, minY, maxZ).setColor(c.getRGB());
         buffer.addVertex(matrix, minX, minY, maxZ).setColor(c.getRGB());
      }

      if (dir == Direction.NORTH) {
         buffer.addVertex(matrix, minX, minY, minZ).setColor(c.getRGB());
         buffer.addVertex(matrix, minX, maxY, minZ).setColor(c.getRGB());
         buffer.addVertex(matrix, maxX, maxY, minZ).setColor(c.getRGB());
         buffer.addVertex(matrix, maxX, minY, minZ).setColor(c.getRGB());
      }

      if (dir == Direction.EAST) {
         buffer.addVertex(matrix, maxX, minY, minZ).setColor(c.getRGB());
         buffer.addVertex(matrix, maxX, maxY, minZ).setColor(c.getRGB());
         buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(c.getRGB());
         buffer.addVertex(matrix, maxX, minY, maxZ).setColor(c.getRGB());
      }

      if (dir == Direction.SOUTH) {
         buffer.addVertex(matrix, minX, minY, maxZ).setColor(c.getRGB());
         buffer.addVertex(matrix, maxX, minY, maxZ).setColor(c.getRGB());
         buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(c.getRGB());
         buffer.addVertex(matrix, minX, maxY, maxZ).setColor(c.getRGB());
      }

      if (dir == Direction.WEST) {
         buffer.addVertex(matrix, minX, minY, minZ).setColor(c.getRGB());
         buffer.addVertex(matrix, minX, minY, maxZ).setColor(c.getRGB());
         buffer.addVertex(matrix, minX, maxY, maxZ).setColor(c.getRGB());
         buffer.addVertex(matrix, minX, maxY, minZ).setColor(c.getRGB());
      }

      if (dir == Direction.UP) {
         buffer.addVertex(matrix, minX, maxY, minZ).setColor(c.getRGB());
         buffer.addVertex(matrix, minX, maxY, maxZ).setColor(c.getRGB());
         buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(c.getRGB());
         buffer.addVertex(matrix, maxX, maxY, minZ).setColor(c.getRGB());
      }
   }

   public static void drawTextIn3D(String text, @NotNull Vec3 pos, double offX, double offY, double textOffset, @NotNull Color color) {
      PoseStack matrices = new PoseStack();
      Camera camera = Module.mc.gameRenderer.getMainCamera();
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
      matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
      matrices.translate(pos.x() - camera.position().x, pos.y() - camera.position().y, pos.z() - camera.position().z);
      matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
      matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
      setupRender();
      matrices.translate(offX, offY - 0.1, -0.01);
      matrices.scale(-0.025F, -0.025F, 0.0F);
      FontRenderers.sf_medium.drawCenteredString(matrices, text, textOffset, 0.0, color.getRGB());
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      endRender();
   }

   @NotNull
   public static Vec3 worldSpaceToScreenSpace(@NotNull Vec3 pos) {
      Vec3 camPos = getCameraPos();
      int displayHeight = Module.mc.getWindow().getScreenHeight();
      int[] viewport = new int[4];
      GL11.glGetIntegerv(2978, viewport);
      Vector3f target = new Vector3f();
      double deltaX = pos.x - camPos.x;
      double deltaY = pos.y - camPos.y;
      double deltaZ = pos.z - camPos.z;
      Vector4f transformedCoordinates = new Vector4f((float)deltaX, (float)deltaY, (float)deltaZ, 1.0F).mul(lastWorldSpaceMatrix);
      Matrix4f matrixProj = new Matrix4f(lastProjMat);
      Matrix4f matrixModel = new Matrix4f(lastModMat);
      matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
      return new Vec3(target.x / getScaleFactor(), (displayHeight - target.y) / getScaleFactor(), target.z);
   }

   public static double getScaleFactor() {
      return Module.mc.getWindow().getGuiScale();
   }

   @Deprecated
   public static void drawFilledFadeBox(@NotNull PoseStack stack, @NotNull AABB box, @NotNull Color c, @NotNull Color c1) {
      FADE_QUEUE.add(new Render3DEngine.FadeAction(box, c, c1));
   }

   public static void drawGradientFilledFadeBox(@NotNull PoseStack stack, @NotNull AABB box, @NotNull Color[] bottomColors, @NotNull Color[] topColors) {
      if (bottomColors.length >= 4 && topColors.length >= 4) {
         GRADIENT_FADE_QUEUE.add(new Render3DEngine.GradientFadeAction(box, bottomColors, topColors));
      }
   }

   public static void setFilledFadePoints(AABB box, BufferBuilder buffer, Matrix4f posMatrix, Color c, Color c1) {
      Vec3 camPos = getCameraPos();
      float minX = (float)(box.minX - camPos.x);
      float minY = (float)(box.minY - camPos.y);
      float minZ = (float)(box.minZ - camPos.z);
      float maxX = (float)(box.maxX - camPos.x);
      float maxY = (float)(box.maxY - camPos.y);
      float maxZ = (float)(box.maxZ - camPos.z);
      buffer.addVertex(posMatrix, minX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, minX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(c1.getRGB());
   }

   public static void setGradientFilledFadePoints(AABB box, BufferBuilder buffer, Matrix4f posMatrix, Color[] bottomColors, Color[] topColors) {
      Vec3 camPos = getCameraPos();
      float minX = (float)(box.minX - camPos.x);
      float minY = (float)(box.minY - camPos.y);
      float minZ = (float)(box.minZ - camPos.z);
      float maxX = (float)(box.maxX - camPos.x);
      float maxY = (float)(box.maxY - camPos.y);
      float maxZ = (float)(box.maxZ - camPos.z);
      int bottomMinMin = bottomColors[0].getRGB();
      int bottomMaxMin = bottomColors[1].getRGB();
      int bottomMaxMax = bottomColors[2].getRGB();
      int bottomMinMax = bottomColors[3].getRGB();
      int topMinMin = topColors[0].getRGB();
      int topMaxMin = topColors[1].getRGB();
      int topMaxMax = topColors[2].getRGB();
      int topMinMax = topColors[3].getRGB();
      buffer.addVertex(posMatrix, minX, minY, minZ).setColor(bottomMinMin);
      buffer.addVertex(posMatrix, maxX, minY, minZ).setColor(bottomMaxMin);
      buffer.addVertex(posMatrix, maxX, minY, maxZ).setColor(bottomMaxMax);
      buffer.addVertex(posMatrix, minX, minY, maxZ).setColor(bottomMinMax);
      buffer.addVertex(posMatrix, minX, minY, minZ).setColor(bottomMinMin);
      buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(topMinMin);
      buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(topMaxMin);
      buffer.addVertex(posMatrix, maxX, minY, minZ).setColor(bottomMaxMin);
      buffer.addVertex(posMatrix, maxX, minY, minZ).setColor(bottomMaxMin);
      buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(topMaxMin);
      buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(topMaxMax);
      buffer.addVertex(posMatrix, maxX, minY, maxZ).setColor(bottomMaxMax);
      buffer.addVertex(posMatrix, minX, minY, maxZ).setColor(bottomMinMax);
      buffer.addVertex(posMatrix, maxX, minY, maxZ).setColor(bottomMaxMax);
      buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(topMaxMax);
      buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(topMinMax);
      buffer.addVertex(posMatrix, minX, minY, minZ).setColor(bottomMinMin);
      buffer.addVertex(posMatrix, minX, minY, maxZ).setColor(bottomMinMax);
      buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(topMinMax);
      buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(topMinMin);
      buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(topMinMin);
      buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(topMinMax);
      buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(topMaxMax);
      buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(topMaxMin);
   }

   public static void drawLine(@NotNull Vec3 start, @NotNull Vec3 end, @NotNull Color color) {
      LINE_QUEUE.add(new Render3DEngine.LineAction(start, end, color));
   }

   @Deprecated
   public static void drawBoxOutline(@NotNull AABB box, Color color, float lineWidth) {
      OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(box, color, lineWidth));
   }

   public static void drawGradientDashedBoxOutline(@NotNull AABB box, @NotNull Color[] colors, float lineWidth, double dashLength, double gapLength) {
      if (colors.length >= 4) {
         GRADIENT_OUTLINE_QUEUE.add(new Render3DEngine.GradientOutlineAction(box, colors, lineWidth, dashLength, gapLength));
      }
   }

   public static void setOutlinePoints(AABB box, PoseStack matrices, BufferBuilder buffer, Color color) {
      box = box.move(new Vec3(box.minX, box.minY, box.minZ).reverse());
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float y2 = (float)box.maxY;
      float z2 = (float)box.maxZ;
      vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
      vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
      vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
      vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
      vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
      vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
      vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
      vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
      vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
      vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
      vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color);
      vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
   }

   public static void setGradientDashedOutlinePoints(AABB box, PoseStack matrices, BufferBuilder buffer, Color[] colors, double dashLength, double gapLength) {
      if (colors.length >= 4) {
         box = box.move(new Vec3(box.minX, box.minY, box.minZ).reverse());
         float x1 = (float)box.minX;
         float y1 = (float)box.minY;
         float z1 = (float)box.minZ;
         float x2 = (float)box.maxX;
         float y2 = (float)box.maxY;
         float z2 = (float)box.maxZ;
         drawDashedLineSegment(matrices, buffer, x1, y1, z1, x2, y1, z1, colors[0], colors[1], dashLength, gapLength);
         drawDashedLineSegment(matrices, buffer, x2, y1, z1, x2, y1, z2, colors[1], colors[2], dashLength, gapLength);
         drawDashedLineSegment(matrices, buffer, x2, y1, z2, x1, y1, z2, colors[2], colors[3], dashLength, gapLength);
         drawDashedLineSegment(matrices, buffer, x1, y1, z2, x1, y1, z1, colors[3], colors[0], dashLength, gapLength);
         drawDashedLineSegment(matrices, buffer, x1, y2, z1, x2, y2, z1, colors[0], colors[1], dashLength, gapLength);
         drawDashedLineSegment(matrices, buffer, x2, y2, z1, x2, y2, z2, colors[1], colors[2], dashLength, gapLength);
         drawDashedLineSegment(matrices, buffer, x2, y2, z2, x1, y2, z2, colors[2], colors[3], dashLength, gapLength);
         drawDashedLineSegment(matrices, buffer, x1, y2, z2, x1, y2, z1, colors[3], colors[0], dashLength, gapLength);
         drawDashedLineSegment(matrices, buffer, x1, y1, z1, x1, y2, z1, colors[0], colors[0], dashLength, gapLength);
         drawDashedLineSegment(matrices, buffer, x2, y1, z1, x2, y2, z1, colors[1], colors[1], dashLength, gapLength);
         drawDashedLineSegment(matrices, buffer, x2, y1, z2, x2, y2, z2, colors[2], colors[2], dashLength, gapLength);
         drawDashedLineSegment(matrices, buffer, x1, y1, z2, x1, y2, z2, colors[3], colors[3], dashLength, gapLength);
      }
   }

   private static void drawDashedLineSegment(
      PoseStack matrices,
      BufferBuilder buffer,
      double x1,
      double y1,
      double z1,
      double x2,
      double y2,
      double z2,
      Color color1,
      Color color2,
      double dashLength,
      double gapLength
   ) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      double dz = z2 - z1;
      double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (!(length < 0.001)) {
         double safeDashLength = Math.max(0.001, dashLength);
         double safeGapLength = Math.max(0.0, gapLength);
         double segmentLength = safeDashLength + safeGapLength;
         double unitX = dx / length;
         double unitY = dy / length;
         double unitZ = dz / length;

         for (double currentPos = 0.0; currentPos < length; currentPos += segmentLength) {
            double dashEnd = Math.min(currentPos + safeDashLength, length);
            if (!(dashEnd <= currentPos)) {
               double startT = currentPos / length;
               double endT = dashEnd / length;
               float startX = (float)(x1 + unitX * currentPos);
               float startY = (float)(y1 + unitY * currentPos);
               float startZ = (float)(z1 + unitZ * currentPos);
               float endX = (float)(x1 + unitX * dashEnd);
               float endY = (float)(y1 + unitY * dashEnd);
               float endZ = (float)(z1 + unitZ * dashEnd);
               vertexLine(
                  matrices, buffer, startX, startY, startZ, endX, endY, endZ, interpolateColor(color1, color2, startT), interpolateColor(color1, color2, endT)
               );
            }
         }
      }
   }

   private static Color interpolateColor(Color color1, Color color2, double amount) {
      return Render2DEngine.interpolateColorC(color1, color2, (float)Mth.clamp(amount, 0.0, 1.0));
   }

   @Deprecated
   public static void drawSideOutline(@NotNull AABB box, Color color, float lineWidth, Direction dir) {
      OUTLINE_SIDE_QUEUE.add(new Render3DEngine.OutlineSideAction(box, color, lineWidth, dir));
   }

   public static void setSideOutlinePoints(AABB box, PoseStack matrices, BufferBuilder buffer, Color color, Direction dir) {
      box = box.move(new Vec3(box.minX, box.minY, box.minZ).reverse());
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float y2 = (float)box.maxY;
      float z2 = (float)box.maxZ;
      switch (dir) {
         case UP:
            vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
            vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
            vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color);
            vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
            break;
         case DOWN:
            vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
            vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
            vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
            vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
            break;
         case EAST:
            vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
            vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
            vertexLine(matrices, buffer, x2, y2, z2, x2, y2, z1, color);
            vertexLine(matrices, buffer, x2, y1, z2, x2, y1, z1, color);
            break;
         case WEST:
            vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
            vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
            vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
            vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
            break;
         case NORTH:
            vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
            vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
            vertexLine(matrices, buffer, x2, y1, z1, x1, y1, z1, color);
            vertexLine(matrices, buffer, x2, y2, z1, x1, y2, z1, color);
            break;
         case SOUTH:
            vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
            vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
            vertexLine(matrices, buffer, x1, y1, z2, x2, y1, z2, color);
            vertexLine(matrices, buffer, x1, y2, z2, x2, y2, z2, color);
      }
   }

   public static void vertexLine(
      @NotNull PoseStack matrices, @NotNull VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, @NotNull Color lineColor
   ) {
      vertexLine(matrices, buffer, x1, y1, z1, x2, y2, z2, lineColor, lineColor);
   }

   public static void vertexLine(
      @NotNull PoseStack matrices,
      @NotNull VertexConsumer buffer,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      @NotNull Color startColor,
      @NotNull Color endColor
   ) {
      Matrix4f model = matrices.last().pose();
      Pose entry = matrices.last();
      Vector3f normalVec = getNormal(x1, y1, z1, x2, y2, z2);
      buffer.addVertex(model, x1, y1, z1)
         .setColor(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), startColor.getAlpha())
         .setNormal(entry, normalVec.x(), normalVec.y(), normalVec.z());
      buffer.addVertex(model, x2, y2, z2)
         .setColor(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), endColor.getAlpha())
         .setNormal(entry, normalVec.x(), normalVec.y(), normalVec.z());
   }

   @NotNull
   public static Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
      float xNormal = x2 - x1;
      float yNormal = y2 - y1;
      float zNormal = z2 - z1;
      float normalSqrt = Mth.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);
      return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
   }

   @NotNull
   public static PoseStack matrixFrom(double x, double y, double z) {
      PoseStack matrices = new PoseStack();
      Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
      matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
      matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
      matrices.translate(x - camera.position().x, y - camera.position().y, z - camera.position().z);
      return matrices;
   }

   public static void setupRender() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   }

   public static void endRender() {
      RenderSystem.disableBlend();
      BufferUploader.invalidateCommandEncoder();
      try {
         org.lwjgl.opengl.GL33.glBindSampler(0, 0);
      } catch (Throwable ignored) {}
   }

   public static void drawTargetEsp(PoseStack stack, @NotNull Entity target) {
      ArrayList<Vec3> vecs = new ArrayList<>();
      ArrayList<Vec3> vecs1 = new ArrayList<>();
      ArrayList<Vec3> vecs2 = new ArrayList<>();
      Vec3 camPos = getCameraPos();
      double x = target.xo + (target.getX() - target.xo) * getTickDelta() - camPos.x;
      double y = target.yo + (target.getY() - target.yo) * getTickDelta() - camPos.y;
      double z = target.zo + (target.getZ() - target.zo) * getTickDelta() - camPos.z;
      double height = target.getBbHeight();

      for (int i = 0; i <= 361; i++) {
         double v = Math.sin(Math.toRadians(i));
         double u = Math.cos(Math.toRadians(i));
         Vec3 vec = new Vec3((float)(u * 0.5), height, (float)(v * 0.5));
         vecs.add(vec);
         double v1 = Math.sin(Math.toRadians((i + 120) % 360));
         double u1 = Math.cos(Math.toRadians(i + 120) % 360.0);
         Vec3 vec1 = new Vec3((float)(u1 * 0.5), height, (float)(v1 * 0.5));
         vecs1.add(vec1);
         double v2 = Math.sin(Math.toRadians((i + 240) % 360));
         double u2 = Math.cos(Math.toRadians((i + 240) % 360));
         Vec3 vec2 = new Vec3((float)(u2 * 0.5), height, (float)(v2 * 0.5));
         vecs2.add(vec2);
         height -= 0.004F;
      }

      stack.pushPose();
      stack.translate(x, y, z);
      setupRender();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f matrix = stack.last().pose();

      for (int j = 0; j < vecs.size() - 1; j++) {
         float alpha = 1.0F - (j + (float)(System.currentTimeMillis() - Vcore.initTime) / 5.0F) % 360.0F / 60.0F;
         bufferBuilder.addVertex(matrix, (float)vecs.get(j).x, (float)vecs.get(j).y, (float)vecs.get(j).z)
            .setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
         bufferBuilder.addVertex(matrix, (float)vecs.get(j + 1).x, (float)vecs.get(j + 1).y + 0.1F, (float)vecs.get(j + 1).z)
            .setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
      }

      Render2DEngine.endBuilding(bufferBuilder);
      bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (int j = 0; j < vecs1.size() - 1; j++) {
         float alpha = 1.0F - (j + (float)(System.currentTimeMillis() - Vcore.initTime) / 5.0F) % 360.0F / 60.0F;
         bufferBuilder.addVertex(matrix, (float)vecs1.get(j).x, (float)vecs1.get(j).y, (float)vecs1.get(j).z)
            .setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
         bufferBuilder.addVertex(matrix, (float)vecs1.get(j + 1).x, (float)vecs1.get(j + 1).y + 0.1F, (float)vecs1.get(j + 1).z)
            .setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
      }

      Render2DEngine.endBuilding(bufferBuilder);
      bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (int j = 0; j < vecs2.size() - 1; j++) {
         float alpha = 1.0F - (j + (float)(System.currentTimeMillis() - Vcore.initTime) / 5.0F) % 360.0F / 60.0F;
         bufferBuilder.addVertex(matrix, (float)vecs2.get(j).x, (float)vecs2.get(j).y, (float)vecs2.get(j).z)
            .setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
         bufferBuilder.addVertex(matrix, (float)vecs2.get(j + 1).x, (float)vecs2.get(j + 1).y + 0.1F, (float)vecs2.get(j + 1).z)
            .setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
      }

      Render2DEngine.endBuilding(bufferBuilder);
      RenderSystem.enableCull();
      stack.translate(-x, -y, -z);
      endRender();
      RenderSystem.enableDepthTest();
      stack.popPose();
   }

   public static void drawCircle3D(PoseStack stack, Entity ent, float radius, int color, int points, boolean hudColor, int colorOffset) {
      setupRender();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      Vec3 camPos = getCameraPos();
      double x = ent.xo + (ent.getX() - ent.xo) * getTickDelta() - camPos.x;
      double y = ent.yo + (ent.getY() - ent.yo) * getTickDelta() - camPos.y;
      double z = ent.zo + (ent.getZ() - ent.zo) * getTickDelta() - camPos.z;
      stack.pushPose();
      stack.translate(x, y, z);
      Matrix4f matrix = stack.last().pose();

      for (int i = 0; i <= points; i++) {
         if (hudColor) {
            color = HudEditor.getColor(i * colorOffset).getRGB();
         }

         bufferBuilder.addVertex(matrix, (float)(radius * Math.cos(i * 6.28 / points)), 0.0F, (float)(radius * Math.sin(i * 6.28 / points))).setColor(color);
      }

      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
      endRender();
      stack.translate(-x, -y, -z);
      stack.popPose();
   }

   public static void renderGhosts(LivingEntity target, float anim, float red, float speed) {
      if (target != null && Module.mc.player != null) {
         Camera camera = Module.mc.gameRenderer.getMainCamera();
         Vec3 targetPos = new Vec3(
            Render2DEngine.interpolate(target.xo, target.getX(), getTickDelta()) - camera.position().x,
            Render2DEngine.interpolate(target.yo, target.getY(), getTickDelta()) - camera.position().y,
            Render2DEngine.interpolate(target.zo, target.getZ(), getTickDelta()) - camera.position().z
         );
         double iAge = Render2DEngine.interpolate(Module.mc.player.tickCount - 1, Module.mc.player.tickCount, getTickDelta());
         float halfHeight = target.getBbHeight() / 2.0F + 0.2F;
         float baseWidth = target.getBbWidth() + 0.2F;
         float minY = 0.2F;
         float maxY = target.getBbHeight() - 0.2F;
         float hitEffect = Math.min(red * 2.0F, 2.0F);
         float acceleration = (float)Math.sin(hitEffect * Math.PI) * 0.18F;
         float bany = (float)Math.sin(hitEffect * Math.PI) * -0.04F;
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         RenderSystem.setShaderTexture(0, TextureStorage.firefly);
         RenderSystem.disableDepthTest();
         BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         int length = 10;

         for (int j = 0; j < 4; j++) {
            for (int i = 0; i <= length; i++) {
               double baseAngle = ((i / 2.0F + iAge * speed * 2.0) * length + j * 90.0F) % (length * 180.0F);
               double radians = Math.toRadians(baseAngle);
               float distanceMultiplier = 1.0F + acceleration;
               double sinQuad = Math.sin(Math.toRadians(iAge * 0.7 + i * (j + halfHeight)) * 1.1) / 2.0;
               double adjustedSin = j % 2 == 0 ? sinQuad : -sinQuad;
               double yOffset = minY + (adjustedSin + 0.5) * (maxY - minY);
               float offset = (float)(i + length) / (length + length);
               double finalWidth = baseWidth * distanceMultiplier * 1.04F;
               PoseStack matrices = new PoseStack();
               matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
               matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
               matrices.translate(targetPos.x + Math.cos(radians) * finalWidth, targetPos.y + yOffset, targetPos.z + Math.sin(radians) * finalWidth);
               matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
               matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
               float scale = 0.6F * offset * (0.6F + speed * 0.1F) + bany;
               if (!(scale <= 0.0F)) {
                  int baseColor = HudEditor.getColor(i).getRGB();
                  int color = applyRedAndAlpha(baseColor, 1.0F + red * 10.0F, offset * anim);
                  float halfScale = scale / 2.0F;
                  Matrix4f matrix = matrices.last().pose();
                  buffer.addVertex(matrix, -halfScale, halfScale, 0.0F).setUv(0.0F, 1.0F).setColor(color);
                  buffer.addVertex(matrix, halfScale, halfScale, 0.0F).setUv(1.0F, 1.0F).setColor(color);
                  buffer.addVertex(matrix, halfScale, -halfScale, 0.0F).setUv(1.0F, 0.0F).setColor(color);
                  buffer.addVertex(matrix, -halfScale, -halfScale, 0.0F).setUv(0.0F, 0.0F).setColor(color);
               }
            }
         }

         BufferUploader.drawWithShader(buffer.buildOrThrow());
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   private static int applyRedAndAlpha(int color, float redMultiplier, float alpha) {
      Color base = new Color(color, true);
      int r = Mth.clamp((int)(base.getRed() * redMultiplier), 0, 255);
      int a = Mth.clamp((int)(base.getAlpha() * alpha), 0, 255);
      return new Color(r, base.getGreen(), base.getBlue(), a).getRGB();
   }

   public static void drawLineDebug(Vec3 start, Vec3 end, Color color) {
      DEBUG_LINE_QUEUE.add(new Render3DEngine.DebugLineAction(start, end, color));
   }

   public static float getTickDelta() {
      return Module.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
   }

   public record DebugLineAction(Vec3 start, Vec3 end, Color color) {
   }

   public record FadeAction(AABB box, Color color, Color color2) {
   }

   public record FillAction(AABB box, Color color) {
   }

   public record FillSideAction(AABB box, Color color, Direction side) {
   }

   public record GradientFadeAction(AABB box, Color[] bottomColors, Color[] topColors) {
   }

   public record GradientOutlineAction(AABB box, Color[] colors, float lineWidth, double dashLength, double gapLength) {
   }

   public record LineAction(Vec3 start, Vec3 end, Color color) {
   }

   public record OutlineAction(AABB box, Color color, float lineWidth) {
   }

   public record OutlineSideAction(AABB box, Color color, float lineWidth, Direction side) {
   }
}
