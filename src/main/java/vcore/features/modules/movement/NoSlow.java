package vcore.features.modules.movement;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import vcore.events.impl.EventKeyboardInput;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.SettingGroup;

public class NoSlow extends Module {
   public final Setting<NoSlow.Mode> mode = new Setting<>("Mode", NoSlow.Mode.Normal);
   private final Setting<Boolean> mainHand = new Setting<>("MainHand", true);
   private final Setting<SettingGroup> selection = new Setting<>("Selection", new SettingGroup(false, 0));
   private final Setting<Boolean> food = new Setting<>("Food", true).addToGroup(this.selection);
   private final Setting<Boolean> projectiles = new Setting<>("Projectiles", false).addToGroup(this.selection);
   private final Setting<Boolean> shield = new Setting<>("Shield", true).addToGroup(this.selection);
   public final Setting<Boolean> soulSand = new Setting<>("SoulSand", false).addToGroup(this.selection);
   public final Setting<Boolean> honey = new Setting<>("Honey", false).addToGroup(this.selection);
   public final Setting<Boolean> slime = new Setting<>("Slime", false).addToGroup(this.selection);
   public final Setting<Boolean> ice = new Setting<>("Ice", false).addToGroup(this.selection);
   public final Setting<Boolean> sweetBerryBush = new Setting<>("SweetBerryBush", false).addToGroup(this.selection);
   public final Setting<Boolean> sneak = new Setting<>("Sneak", false).addToGroup(this.selection);
   public final Setting<Boolean> crawl = new Setting<>("Crawl", false).addToGroup(this.selection);
   private int grimTickCounter;
   private boolean grimTickReady;

   public NoSlow() {
      super("NoSlow", "No slowdown when using items.", Module.Category.MOVEMENT);
   }

   @Override
   public List<Setting<?>> getSettings() {
      List<Setting<?>> settings = new ArrayList<>(super.getSettings());
      settings.removeIf(setting -> setting != this.mode && !"Enabled".equals(setting.getName()) && !"Keybind".equals(setting.getName()));
      return settings;
   }

   @Override
   public void onUpdate() {
      if (mc.player != null) {
         this.updateGrimTickState();
         if (this.isUsingSlowItem()) {
            switch ((NoSlow.Mode)this.mode.getValue()) {
               case Grim:
                   if (mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                      int sel = mc.player.getInventory().getSelectedSlot();
                      this.sendPacket(new ServerboundSetCarriedItemPacket(sel % 8 + 1));
                      this.sendPacket(new ServerboundSetCarriedItemPacket(sel % 7 + 2));
                      this.sendPacket(new ServerboundSetCarriedItemPacket(sel));
                   } else if (this.mainHand.getValue() && (mc.player.getTicksUsingItem() <= 3 || mc.player.tickCount % 2 == 0)) {
                     this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                  }
               case GrimTick:
            }
         }
      }
   }

   @EventHandler
   public void onKeyboardInput(EventKeyboardInput e) {
      if (e != null && mc.player != null) {
         if (this.mode.getValue() == NoSlow.Mode.Matrix && mc.player.isUsingItem() && !mc.player.isFallFlying()) {
            float forward = vcore.utility.player.InputUtility.getForward(mc.player.input) * 5.0F;
            float left = vcore.utility.player.InputUtility.getLeft(mc.player.input) * 5.0F;
            float multiplier = 1.0F;
            if (mc.player.onGround()) {
               if (forward != 0.0F && left != 0.0F) {
                  forward *= 0.35F;
                  left *= 0.35F;
               } else {
                  forward *= 0.5F;
                  left *= 0.5F;
               }
            } else if (forward != 0.0F && left != 0.0F) {
               multiplier = 0.47F;
            } else {
               multiplier = 0.67F;
            }

            forward *= multiplier;
            left *= multiplier;
            vcore.utility.player.InputUtility.setMoveVector(mc.player.input, left, forward);
         }
      }
   }

   public boolean canNoSlow() {
      return mc.player == null
         ? true
         : this.mode.getValue() != NoSlow.Mode.Matrix
            && this.consumeGrimTickWindow()
            && this.allowsActiveItem()
            && this.allowsActiveHand()
            && !this.isGrimNewBlocked();
   }

   private boolean isGrimNewBlocked() {
      return mc.player != null
         && this.mode.getValue() == NoSlow.Mode.Grim
         && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
         && (mc.player.getOffhandItem().getComponents().has(DataComponents.FOOD) || mc.player.getOffhandItem().getItem() == Items.SHIELD);
   }

   private void updateGrimTickState() {
      if (this.mode.getValue() == NoSlow.Mode.GrimTick && this.isUsingSlowItem()) {
         if (this.grimTickCounter < 20) {
            this.grimTickCounter++;
         }

         this.grimTickReady = this.grimTickCounter > 1;
      } else {
         this.grimTickCounter = 0;
         this.grimTickReady = false;
      }
   }

   private boolean consumeGrimTickWindow() {
      if (this.mode.getValue() != NoSlow.Mode.GrimTick) {
         return true;
      }

      if (!this.grimTickReady) {
         return false;
      }

      this.grimTickReady = false;
      this.grimTickCounter = 0;
      return true;
   }

   private boolean allowsActiveItem() {
      if (!this.food.getValue() && mc.player.getUseItem().getComponents().has(DataComponents.FOOD)) {
         return false;
      } else {
         return !this.shield.getValue() && mc.player.getUseItem().getItem() == Items.SHIELD
            ? false
            : this.projectiles.getValue()
               || mc.player.getUseItem().getItem() != Items.CROSSBOW
                  && mc.player.getUseItem().getItem() != Items.BOW
                  && mc.player.getUseItem().getItem() != Items.TRIDENT;
      }
   }

   private boolean allowsActiveHand() {
      return this.mainHand.getValue() || mc.player.getUsedItemHand() != InteractionHand.MAIN_HAND;
   }

   private boolean isUsingSlowItem() {
      return mc.player != null && mc.player.isUsingItem() && !mc.player.isHandsBusy() && !mc.player.isFallFlying();
   }

   public enum Mode {
      Grim,
      GrimTick,
      Matrix,
      Normal;
   }
}
