package vcore.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Items;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.PostPlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.injection.accesors.IMinecraftClient;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.player.InventoryUtility;

public final class AutoGApple extends Module {
   public final Setting<Integer> Delay = new Setting<>("UseDelay", 0, 0, 2000);
   private final Setting<Float> health = new Setting<>("health", 15.0F, 1.0F, 36.0F);
   public Setting<Boolean> absorption = new Setting<>("Absorption", false);
   public Setting<Boolean> autoTotemIntegration = new Setting<>("AutoTotemIntegration", false);
   private boolean isActive;
   private final Timer useDelay = new Timer();

   public AutoGApple() {
      super("AutoGApple", "Auto eats golden apples.", Module.Category.COMBAT);
   }

   @EventHandler
   public void onUpdate(PostPlayerUpdateEvent e) {
      if (!fullNullCheck()) {
         if (this.GapInOffHand()) {
            if (mc.player.getHealth() + (this.absorption.getValue() ? mc.player.getAbsorptionAmount() : 0.0F) <= this.health.getValue()
               && this.useDelay.passedMs(this.Delay.getValue().intValue())) {
               this.isActive = true;
               if (mc.screen != null && !mc.player.isUsingItem()) {
                  ((IMinecraftClient)mc).idoItemUse();
               } else {
                  mc.options.keyUse.setDown(true);
               }
            } else if (this.isActive) {
               this.isActive = false;
               mc.options.keyUse.setDown(false);
            }
         } else if (this.isActive) {
            this.isActive = false;
            mc.options.keyUse.setDown(false);
         }
      }
   }

   private boolean GapInOffHand() {
      return this.autoTotemIntegration.getValue()
            && ModuleManager.autoTotem.isEnabled()
            && InventoryUtility.findItemInHotBar(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE).found()
         ? true
         : !mc.player.getOffhandItem().isEmpty()
            && (mc.player.getOffhandItem().getItem() == Items.GOLDEN_APPLE || mc.player.getOffhandItem().getItem() == Items.ENCHANTED_GOLDEN_APPLE);
   }
}
