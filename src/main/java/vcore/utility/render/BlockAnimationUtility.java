package vcore.utility.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class BlockAnimationUtility {
   private static final Map<BlockAnimationUtility.BlockRenderData, Long> blocks = new ConcurrentHashMap<>();

   public static void onRender(PoseStack matrixStack) {
      blocks.forEach((animation, time) -> {
         if ((float)(System.currentTimeMillis() - time) > 300.0F) {
            blocks.remove(animation);
         } else {
            animation.renderWithTime(System.currentTimeMillis() - time, matrixStack);
         }
      });
   }

   public static void renderBlock(
      BlockPos pos,
      Color lineColor,
      int lineWidth,
      Color fillColor,
      BlockAnimationUtility.BlockAnimationMode animationMode,
      BlockAnimationUtility.BlockRenderMode renderMode
   ) {
      if (renderMode != BlockAnimationUtility.BlockRenderMode.None) {
         blocks.put(new BlockAnimationUtility.BlockRenderData(pos, lineColor, lineWidth, fillColor, animationMode, renderMode), System.currentTimeMillis());
      }
   }

   public static boolean isRendering(BlockPos pos) {
      return blocks.keySet().stream().anyMatch(blockRenderData -> blockRenderData.pos().equals(pos));
   }

   public enum BlockAnimationMode {
      Fade,
      Hover,
      Decrease,
      Static,
      Flash,
      Grow,
      Fill,
      TNT,
      Pull;
   }

   private record BlockRenderData(
      BlockPos pos,
      Color lineColor,
      int lineWidth,
      Color fillColor,
      BlockAnimationUtility.BlockAnimationMode animationMode,
      BlockAnimationUtility.BlockRenderMode renderMode
   ) {
      void renderWithTime(Long time, PoseStack stack) {
         switch (this.animationMode) {
            case Fade: {
               AABB box = new AABB(this.pos);
               renderBox(time, stack, box, this.renderMode, this.lineColor, this.lineWidth, this.fillColor);
               break;
            }
            case Hover: {
               float scale = 1.0F + (float)time.longValue() / 1500.0F;
               AABB box = new AABB(this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.pos.getX(), this.pos.getY(), this.pos.getZ());
               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Line) {
                  Render3DEngine.drawBoxOutline(
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5F, 0.5 + scale * 0.5, 0.5 + scale * 0.5),
                     Render2DEngine.injectAlpha(this.lineColor, (int)(this.lineColor.getAlpha() * (1.0F - (float)time.longValue() / 300.0F))),
                     this.lineWidth
                  );
               }

               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Fill) {
                  Render3DEngine.drawFilledBox(
                     stack,
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5),
                     Render2DEngine.injectAlpha(this.fillColor, (int)(this.fillColor.getAlpha() * (1.0F - (float)time.longValue() / 300.0F)))
                  );
               }
               break;
            }
            case Decrease: {
               float scale = 1.0F - (float)time.longValue() / 300.0F;
               AABB box = new AABB(this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.pos.getX(), this.pos.getY(), this.pos.getZ());
               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Line) {
                  Render3DEngine.drawBoxOutline(
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5), this.lineColor, this.lineWidth
                  );
               }

               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Fill) {
                  Render3DEngine.drawFilledBox(
                     stack,
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5),
                     Render2DEngine.injectAlpha(this.fillColor, (int)(this.fillColor.getAlpha() * (1.0F - (float)time.longValue() / 300.0F)))
                  );
               }
               break;
            }
            case Static:
               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Line) {
                  Render3DEngine.drawBoxOutline(new AABB(this.pos), this.lineColor, this.lineWidth);
               }

               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Fill) {
                  Render3DEngine.drawFilledBox(stack, new AABB(this.pos), this.fillColor);
               }
               break;
            case Flash: {
               float scale;
               if (time > 100L) {
                  scale = 1.0F - (float)(time - 100L) / 400.0F;
               } else {
                  scale = (float)time.longValue() / 100.0F;
               }

               AABB box = new AABB(this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.pos.getX(), this.pos.getY(), this.pos.getZ());
               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Line) {
                  Render3DEngine.drawBoxOutline(
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5), this.lineColor, this.lineWidth
                  );
               }

               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Fill) {
                  Render3DEngine.drawFilledBox(
                     stack,
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5),
                     Render2DEngine.injectAlpha(this.fillColor, (int)(this.fillColor.getAlpha() * scale))
                  );
               }
               break;
            }
            case Grow: {
               float scale = (float)time.longValue() / 300.0F;
               AABB box = new AABB(this.pos.getX(), this.pos.getY() + scale, this.pos.getZ(), this.pos.getX() + 1, this.pos.getY(), this.pos.getZ() + 1);
               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Line) {
                  Render3DEngine.drawBoxOutline(box, this.lineColor, this.lineWidth);
               }

               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Fill) {
                  Render3DEngine.drawFilledBox(
                     stack, box, Render2DEngine.injectAlpha(this.fillColor, (int)(this.fillColor.getAlpha() * ((float)time.longValue() / 300.0F)))
                  );
               }
               break;
            }
            case Fill: {
               float scale = (float)time.longValue() / 300.0F;
               AABB box = new AABB(this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.pos.getX(), this.pos.getY(), this.pos.getZ());
               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Line) {
                  Render3DEngine.drawBoxOutline(
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5), this.lineColor, this.lineWidth
                  );
               }

               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Fill) {
                  Render3DEngine.drawFilledBox(
                     stack,
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5),
                     Render2DEngine.injectAlpha(this.fillColor, (int)(this.fillColor.getAlpha() * ((float)time.longValue() / 300.0F)))
                  );
               }
               break;
            }
            case TNT: {
               float scale;
               if (time < 200L) {
                  scale = 1.0F;
               } else {
                  scale = 1.0F + ((float)time.longValue() - 200.0F) / 400.0F;
               }

               AABB box = new AABB(this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.pos.getX(), this.pos.getY(), this.pos.getZ());
               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Line) {
                  Render3DEngine.drawBoxOutline(
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5F, 0.5 + scale * 0.5, 0.5 + scale * 0.5), this.lineColor, this.lineWidth
                  );
               }

               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Fill) {
                  Render3DEngine.drawFilledBox(
                     stack,
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5),
                     Render2DEngine.injectAlpha(this.fillColor, (int)(this.fillColor.getAlpha() * scale))
                  );
               }
               break;
            }
            case Pull: {
               float scale;
               if (time < 200L) {
                  scale = 1.5F - (float)time.longValue() / 200.0F * 0.5F;
               } else {
                  scale = 1.0F;
               }

               AABB box = new AABB(this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.pos.getX(), this.pos.getY(), this.pos.getZ());
               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Line) {
                  Render3DEngine.drawBoxOutline(
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5F, 0.5 + scale * 0.5, 0.5 + scale * 0.5), this.lineColor, this.lineWidth
                  );
               }

               if (this.renderMode == BlockAnimationUtility.BlockRenderMode.All || this.renderMode == BlockAnimationUtility.BlockRenderMode.Fill) {
                  Render3DEngine.drawFilledBox(
                     stack,
                     box.contract(scale, scale, scale).move(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5),
                     Render2DEngine.injectAlpha(this.fillColor, (int)(this.fillColor.getAlpha() * scale))
                  );
               }
            }
         }
      }

      private static void renderBox(
         Long time, PoseStack stack, AABB box, BlockAnimationUtility.BlockRenderMode renderMode, Color lineColor, int lineWidth, Color fillColor
      ) {
         if (renderMode == BlockAnimationUtility.BlockRenderMode.All || renderMode == BlockAnimationUtility.BlockRenderMode.Line) {
            Render3DEngine.drawBoxOutline(
               box, Render2DEngine.injectAlpha(lineColor, (int)(fillColor.getAlpha() * (1.0F - (float)time.longValue() / 300.0F))), lineWidth
            );
         }

         if (renderMode == BlockAnimationUtility.BlockRenderMode.All || renderMode == BlockAnimationUtility.BlockRenderMode.Fill) {
            Render3DEngine.drawFilledBox(
               stack, box, Render2DEngine.injectAlpha(fillColor, (int)(fillColor.getAlpha() * (1.0F - (float)time.longValue() / 300.0F)))
            );
         }
      }
   }

   public enum BlockRenderMode {
      Fill,
      Line,
      All,
      None;
   }
}
