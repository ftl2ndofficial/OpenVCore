package vcore.injection;

import com.mojang.blaze3d.platform.InputConstants;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.input.MouseButtonEvent;
import vcore.utility.render.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.Core;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.render.Tooltips;
import vcore.gui.misc.PeekScreen;
import vcore.utility.Timer;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
   @Unique
   private static final float INVENTORY_ANIM_DURATION = 250.0F;
   @Unique
   private Timer delayTimer = new Timer();
   @Unique
   private Runnable postRender;
   @Unique
   private long openTime;
   @Shadow
   @Nullable
   protected Slot hoveredSlot;
   @Shadow
   protected int leftPos;
   @Shadow
   protected int topPos;
   private static final ItemStack[] ITEMS = new ItemStack[27];
   @Unique
   private Map<Render2DEngine.Rectangle, Integer> clickableRects = new HashMap<>();

   @Unique
   private Map<Render2DEngine.Rectangle, Integer> getClickableRects() {
      if (this.clickableRects == null) {
         this.clickableRects = new HashMap<>();
      }
      return this.clickableRects;
   }

   @Unique
   private Timer getDelayTimer() {
      if (this.delayTimer == null) {
         this.delayTimer = new Timer();
      }
      return this.delayTimer;
   }

   protected MixinHandledScreen(Component title) {
      super(title);
   }

   @Shadow
   protected abstract boolean isHovering(Slot var1, double var2, double var4);

   @Shadow
   protected abstract void slotClicked(Slot var1, int var2, int var3, ClickType var4);

   @Inject(method = "init", at = @At("RETURN"))
   private void onInit(CallbackInfo ci) {
      this.openTime = System.currentTimeMillis();
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void drawScreenHook(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         for (int i1 = 0; i1 < Module.mc.player.containerMenu.slots.size(); i1++) {
            Slot slot = (Slot)Module.mc.player.containerMenu.slots.get(i1);
            if (this.isHovering(slot, mouseX, mouseY)
               && slot.isActive()
               && ModuleManager.itemScroller.isEnabled()
               && this.shit()
               && this.attack()
               && this.getDelayTimer().passedMs(ModuleManager.itemScroller.delay.getValue().intValue())) {
               this.slotClicked(slot, slot.index, 0, ClickType.QUICK_MOVE);
               this.getDelayTimer().reset();
            }
         }
      }
   }

   private boolean shit() {
      return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 340)
         || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 344);
   }

   private boolean attack() {
      return Core.hold_mouse0;
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void onRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if (this.hoveredSlot != null && !this.hoveredSlot.getItem().isEmpty() && this.minecraft.player.inventoryMenu.getCarried().isEmpty()) {
            if (Tooltips.hasItems(this.hoveredSlot.getItem()) && Tooltips.storage.getValue()) {
               this.renderShulkerToolTip(context, mouseX, mouseY, 0, 0, this.hoveredSlot.getItem());
            } else if (this.hoveredSlot.getItem().getItem() == Items.FILLED_MAP && Tooltips.maps.getValue()) {
               this.drawMapPreview(context, this.hoveredSlot.getItem(), mouseX, mouseY);
            }
         }

         int xOffset = 0;
         int yOffset = 20;
         int stage = 0;
         if (ModuleManager.tooltips.isEnabled() && ModuleManager.tooltips.shulkerRegear.getValue()) {
            this.getClickableRects().clear();

            for (int i1 = 0; i1 < Module.mc.player.containerMenu.slots.size(); i1++) {
               Slot slot = (Slot)Module.mc.player.containerMenu.slots.get(i1);
               if (!slot.getItem().isEmpty()
                  && slot.getItem().getItem() instanceof BlockItem bi
                  && bi.getBlock() instanceof ShulkerBoxBlock
                  && this.renderShulkerToolTip(context, xOffset, yOffset + 67, mouseX, mouseY, slot.getItem())) {
                  this.getClickableRects().put(new Render2DEngine.Rectangle(xOffset, yOffset, xOffset + 176, yOffset + 67), slot.index);
                  yOffset += 67;
                  if (stage == 0) {
                     if (yOffset + 67 >= Module.mc.getWindow().getGuiScaledHeight()) {
                        yOffset = 20;
                        xOffset = Module.mc.getWindow().getGuiScaledWidth() - 176;
                        stage = 1;
                     }
                  } else if (stage == 1) {
                     if (yOffset + 67 >= Module.mc.getWindow().getGuiScaledHeight()) {
                        yOffset = 20;
                        xOffset = 170;
                        stage = 2;
                     }
                  } else if (yOffset + 67 >= Module.mc.getWindow().getGuiScaledHeight()) {
                     yOffset = 20;
                     xOffset = Module.mc.getWindow().getGuiScaledWidth() - 352;
                     stage = 0;
                  }
               }
            }

            if (this.postRender != null) {
               this.postRender.run();
               this.postRender = null;
            }
         }
      }
   }

   public boolean renderShulkerToolTip(GuiGraphics context, int offsetX, int offsetY, int mouseX, int mouseY, ItemStack stack) {
      try {
         ItemContainerContents compoundTag = (ItemContainerContents)stack.get(DataComponents.CONTAINER);
         if (compoundTag == null) {
            return false;
         }

         float[] colors = new float[]{1.0F, 1.0F, 1.0F};
         if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock sbb && sbb.getColor() != null) {
            Color c = new Color(sbb.getColor().getTextureDiffuseColor());
            colors = new float[]{c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F};
         }

         this.draw(context, compoundTag.stream().toList(), offsetX, offsetY, mouseX, mouseY, colors);
         return true;
      } catch (Exception ignore) {
         return false;
      }
   }

   @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
   private void onDrawMouseoverTooltip(GuiGraphics context, int x, int y, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if (this.hoveredSlot != null
            && !this.hoveredSlot.getItem().isEmpty()
            && this.minecraft.player.inventoryMenu.getCarried().isEmpty()
            && this.hoveredSlot.getItem().getItem() == Items.FILLED_MAP
            && Tooltips.maps.getValue()) {
            ci.cancel();
         }
      }
   }

   @Unique
   private void draw(GuiGraphics context, List<ItemStack> itemStacks, int offsetX, int offsetY, int mouseX, int mouseY, float[] colors) {
      RenderSystem.disableDepthTest();
      GL11.glClear(256);
      offsetX += 8;
      offsetY -= 82;
      boolean shulkerAnimPushed = false;
      if (this.isSurvivalMode()) {
         float progress = this.getOpenProgress();
         if (progress < 1.0F) {
            float anim = this.getBackOutScale(progress);
            float centerX = offsetX + 88.0F;
            float centerY = offsetY + 33.5F;
            context.pose().pushMatrix();
            context.pose().translate(centerX, centerY);
            context.pose().scale(anim, anim);
            context.pose().translate(-centerX, -centerY);
            shulkerAnimPushed = true;
         }
      }

      this.drawBackground(context, offsetX, offsetY, colors);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int row = 0;
      int i = 0;

      for (ItemStack itemStack : itemStacks) {
         context.renderItem(itemStack, offsetX + 8 + i * 18, offsetY + 7 + row * 18);
         context.renderItemDecorations(Module.mc.font, itemStack, offsetX + 8 + i * 18, offsetY + 7 + row * 18);
         if (mouseX > offsetX + 8 + i * 18 && mouseX < offsetX + 28 + i * 18 && mouseY > offsetY + 7 + row * 18 && mouseY < offsetY + 27 + row * 18) {
            this.postRender = () -> context.setTooltipForNextFrame(this.font, itemStack, mouseX, mouseY);
         }

         if (++i >= 9) {
            i = 0;
            row++;
         }
      }

      RenderSystem.enableDepthTest();
      if (shulkerAnimPushed) {
         context.pose().popMatrix();
      }
   }

   @Unique
   private boolean isSurvivalMode() {
      return this.minecraft != null && this.minecraft.gameMode != null && this.minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL;
   }

   @Unique
   private float getOpenProgress() {
      return Math.min(1.0F, (float)(System.currentTimeMillis() - this.openTime) / 250.0F);
   }

   @Unique
   private float getBackOutScale(float progress) {
      float x = progress - 1.0F;
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow(x, 3.0) + c1 * (float)Math.pow(x, 2.0);
   }

   private void drawBackground(GuiGraphics context, int x, int y, float[] colors) {
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(colors[0], colors[1], colors[2], 1.0F);
      context.blit(RenderPipelines.GUI_TEXTURED, TextureStorage.container, x, y, 0.0F, 0.0F, 176, 67, 176, 67);
      RenderSystem.enableBlend();
   }

   private void drawMapPreview(GuiGraphics context, ItemStack stack, int x, int y) {
      RenderSystem.enableBlend();
      context.pose().pushMatrix();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int y1 = y - 12;
      int x1 = x + 8;
      MapItemSavedData mapState = MapItem.getSavedData(stack, this.minecraft.level);
      if (mapState != null) {
         mapState.getHoldingPlayer(this.minecraft.player);
         x1 += 8;
         y1 += 8;
         double scale = 0.65625;
         context.pose().translate(x1, y1);
         context.pose().scale((float)scale, (float)scale);
         MapRenderState mapRenderState = new MapRenderState();
         this.minecraft.getMapRenderer().extractRenderState((MapId)stack.get(DataComponents.MAP_ID), mapState, mapRenderState);
         context.submitMapRenderState(mapRenderState);
      }

      context.pose().popMatrix();
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
      if (!Module.fullNullCheck()) {
         double mouseX = event.x();
         double mouseY = event.y();
         int button = event.button();
         if (button == 2
            && this.hoveredSlot != null
            && !this.hoveredSlot.getItem().isEmpty()
            && this.minecraft.player.inventoryMenu.getCarried().isEmpty()) {
            ItemStack itemStack = this.hoveredSlot.getItem();
            if (Tooltips.hasItems(itemStack) && Tooltips.middleClickOpen.getValue()) {
               Arrays.fill(ITEMS, ItemStack.EMPTY);
               ItemContainerContents nbt = (ItemContainerContents)itemStack.get(DataComponents.CONTAINER);
               if (nbt != null) {
                  List<ItemStack> list = nbt.stream().toList();

                  for (int i = 0; i < list.size(); i++) {
                     ITEMS[i] = list.get(i);
                  }
               }

               this.minecraft
                  .setScreen(
                     new PeekScreen(
                        new ShulkerBoxMenu(0, this.minecraft.player.getInventory(), new SimpleContainer(ITEMS)),
                        this.minecraft.player.getInventory(),
                        this.hoveredSlot.getItem().getHoverName(),
                        ((BlockItem)this.hoveredSlot.getItem().getItem()).getBlock()
                     )
                  );
               cir.setReturnValue(true);
            }
         }

         Map<Render2DEngine.Rectangle, Integer> rects = this.getClickableRects();
         if (!rects.isEmpty()) {
            for (Render2DEngine.Rectangle rect : rects.keySet()) {
               if (rect != null && rect.contains(mouseX, mouseY)) {
                  if (ModuleManager.tooltips.shulkerRegearShiftMode.getValue()) {
                     Module.mc
                        .gameMode
                        .handleInventoryMouseClick(Module.mc.player.containerMenu.containerId, rects.get(rect), 0, ClickType.QUICK_MOVE, Module.mc.player);
                  } else {
                     Module.mc
                        .gameMode
                        .handleInventoryMouseClick(Module.mc.player.containerMenu.containerId, rects.get(rect), 0, ClickType.PICKUP, Module.mc.player);
                  }
               }
            }
         }
      }
   }
}
