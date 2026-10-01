package vcore.features.modules.misc;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Random;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.features.modules.render.StorageEsp;
import vcore.setting.Setting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.utility.Timer;
import vcore.utility.math.MathUtility;

public class ChestStealer extends Module {
   public final Setting<ItemSelectSetting> items = new Setting<>("Items", new ItemSelectSetting(new ArrayList<>()));
   private final Setting<Integer> delay = new Setting<>("Delay", 50, 0, 1000);
   private final Setting<Boolean> random = new Setting<>("Random", false);
   private final Setting<Boolean> close = new Setting<>("Close", false);
   private final Setting<Boolean> autoMyst = new Setting<>("AutoMyst", false);
   private final Setting<ChestStealer.Sort> sort = new Setting<>("Sort", ChestStealer.Sort.None);
   private final Timer autoMystDelay = new Timer();
   private final Timer timer = new Timer();
   private final Random rnd = new Random();

   public ChestStealer() {
      super("ChestStealer", "Steals items from chests.", Module.Category.MISC);
   }

   @Override
   public void onRender3D(PoseStack stack) {
      if (mc.player.containerMenu instanceof ChestMenu chest) {
         for (int i = 0; i < chest.getContainer().getContainerSize(); i++) {
            Slot slot = chest.getSlot(i);
            if (slot.hasItem()
               && this.isAllowed(slot.getItem())
               && this.timer
                  .every(this.delay.getValue() + (this.random.getValue() && this.delay.getValue() != 0 ? this.rnd.nextInt(this.delay.getValue()) : 0))
               && mc.screen != null
               && !mc.screen.getTitle().getString().toLowerCase().contains("auction")
               && !mc.screen.getTitle().getString().toLowerCase().contains("purchase")
               && !mc.screen.getTitle().getString().contains("Аукцион")
               && !mc.screen.getTitle().getString().contains("покупки")) {
               mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
               this.autoMystDelay.reset();
            }
         }

         if (this.isContainerEmpty(chest) && this.close.getValue()) {
            mc.player.closeContainer();
         }
      }
   }

   @EventHandler
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (this.autoMyst.getValue() && mc.screen == null && this.autoMystDelay.passedMs(3000L)) {
         for (BlockEntity be : StorageEsp.getBlockEntities()) {
            if (be instanceof EnderChestBlockEntity && !(mc.player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) > 39.0)) {
               mc.gameMode
                  .useItemOn(
                     mc.player,
                     InteractionHand.MAIN_HAND,
                     new BlockHitResult(
                        Vec3.atCenterOf(be.getBlockPos()).add(MathUtility.random(-0.4, 0.4), 0.375, MathUtility.random(-0.4, 0.4)), Direction.UP, be.getBlockPos(), false
                     )
                  );
               mc.player.swing(InteractionHand.MAIN_HAND);
               break;
            }
         }
      }
   }

   private boolean isAllowed(ItemStack stack) {
      boolean allowed = this.items.getValue().contains(stack.getItem().getDescriptionId().replace("block.minecraft.", "").replace("item.minecraft.", ""));

      return switch ((ChestStealer.Sort)this.sort.getValue()) {
         case None -> true;
         case WhiteList -> allowed;
         default -> !allowed;
      };
   }

   private boolean isContainerEmpty(ChestMenu container) {
      for (int i = 0; i < (container.getContainer().getContainerSize() == 90 ? 54 : 27); i++) {
         if (container.getSlot(i).hasItem()) {
            return false;
         }
      }

      return true;
   }

   private enum Sort {
      None,
      WhiteList,
      BlackList;
   }
}
