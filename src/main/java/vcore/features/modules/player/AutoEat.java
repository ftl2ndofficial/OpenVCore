package vcore.features.modules.player;

import baritone.api.BaritoneAPI;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vcore.Vcore;
import vcore.features.modules.Module;
import vcore.injection.accesors.IMinecraftClient;
import vcore.setting.Setting;

public class AutoEat extends Module {
   public final Setting<Integer> hunger = new Setting<>("Hunger", 8, 0, 20);
   public final Setting<Boolean> gapple = new Setting<>("Gapple", false);
   public final Setting<Boolean> chorus = new Setting<>("Chorus", false);
   public final Setting<Boolean> rottenFlesh = new Setting<>("RottenFlesh", false);
   public final Setting<Boolean> spiderEye = new Setting<>("SpiderEye", false);
   public final Setting<Boolean> pufferfish = new Setting<>("Pufferfish", false);
   public final Setting<Boolean> swapBack = new Setting<>("SwapBack", true);
   public final Setting<Boolean> pauseBaritone = new Setting<>("PauseBaritone", true, v -> Vcore.baritone);
   private boolean eating;
   private int prevSlot;

   public AutoEat() {
      super("AutoEat", "Auto eats food when hungry.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (mc.player.getFoodData().getFoodLevel() <= this.hunger.getValue()) {
         boolean found;
         if (!this.isHandGood(InteractionHand.MAIN_HAND) && !this.isHandGood(InteractionHand.OFF_HAND)) {
            found = this.switchToFood();
         } else {
            found = true;
         }

         if (!found) {
            if (this.eating) {
               this.stopEating();
            }

            return;
         }

         this.startEating();
      } else if (this.eating) {
         this.stopEating();
      }
   }

   public void startEating() {
      this.eating = true;
      if (mc.screen != null && !mc.player.isUsingItem()) {
         ((IMinecraftClient)mc).idoItemUse();
      } else {
         if (this.pauseBaritone.getValue() && Vcore.baritone) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
         }

         mc.options.keyUse.setDown(true);
      }
   }

   public void stopEating() {
      this.eating = false;
      mc.options.keyUse.setDown(false);
      if (this.swapBack.getValue()) {
         mc.player.getInventory().setSelectedSlot(this.prevSlot);
      }

      if (this.pauseBaritone.getValue() && Vcore.baritone) {
         BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
      }
   }

   public boolean switchToFood() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getComponents().has(DataComponents.FOOD)
            && (this.gapple.getValue() || stack.getItem() != Items.GOLDEN_APPLE && stack.getItem() != Items.ENCHANTED_GOLDEN_APPLE)
            && (this.chorus.getValue() || stack.getItem() != Items.CHORUS_FRUIT)
            && (this.rottenFlesh.getValue() || stack.getItem() != Items.ROTTEN_FLESH)
            && (this.spiderEye.getValue() || stack.getItem() != Items.SPIDER_EYE)
            && (this.pufferfish.getValue() || stack.getItem() != Items.PUFFERFISH)) {
            this.prevSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(i);
            this.sendPacket(new ServerboundSetCarriedItemPacket(i));
            return true;
         }
      }

      return false;
   }

   private boolean isHandGood(InteractionHand hand) {
      ItemStack stack = hand == InteractionHand.MAIN_HAND ? mc.player.getMainHandItem() : mc.player.getOffhandItem();
      Item item = stack.getItem();
      return stack.getComponents().has(DataComponents.FOOD)
         && (this.gapple.getValue() || item != Items.GOLDEN_APPLE && item != Items.ENCHANTED_GOLDEN_APPLE)
         && (this.chorus.getValue() || item != Items.CHORUS_FRUIT)
         && (this.rottenFlesh.getValue() || item != Items.ROTTEN_FLESH)
         && (this.spiderEye.getValue() || item != Items.SPIDER_EYE)
         && (this.pufferfish.getValue() || item != Items.PUFFERFISH);
   }
}
