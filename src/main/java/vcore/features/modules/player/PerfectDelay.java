package vcore.features.modules.player;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.injection.accesors.IClientPlayerEntity;
import vcore.setting.Setting;

public class PerfectDelay extends Module {
   private final Setting<PerfectDelay.HorseJump> horse = new Setting<>("Horse", PerfectDelay.HorseJump.Legit);
   private final Setting<Boolean> bow = new Setting<>("Bow", true);
   private final Setting<Boolean> crossbow = new Setting<>("Crossbow", true);
   private final Setting<Boolean> trident = new Setting<>("Trident", true);

   public PerfectDelay() {
      super("PerfectDelay", "Perfect timing delays.", Module.Category.PLAYER);
   }

   private float getEnchantLevel(ItemStack stack) {
      return EnchantmentHelper.getItemEnchantmentLevel(
         (Holder)mc.level.registryAccess().registryOrThrow(Enchantments.PROTECTION.registryKey()).getHolder(Enchantments.QUICK_CHARGE).get(), stack
      );
   }

   @Override
   public void onUpdate() {
      if (mc.player.getUseItem().getItem() instanceof TridentItem
         && this.trident.getValue()
         && mc.player.getTicksUsingItem() > (ModuleManager.tridentBoost.isEnabled() ? ModuleManager.tridentBoost.cooldown.getValue() : 9)) {
         mc.gameMode.releaseUsingItem(mc.player);
      }

      if (mc.player.getUseItem().getItem() instanceof CrossbowItem
         && this.crossbow.getValue()
         && mc.player.getTicksUsingItem() >= 25.0 - 0.25 * this.getEnchantLevel(mc.player.getUseItem()) * 20.0) {
         mc.gameMode.releaseUsingItem(mc.player);
      }

      if (mc.player.getUseItem().getItem() instanceof BowItem && this.bow.getValue() && mc.player.getTicksUsingItem() > 19) {
         mc.gameMode.releaseUsingItem(mc.player);
      }

      if (mc.player.getControlledVehicle() != null && mc.player.getControlledVehicle() instanceof Horse && this.horse.is(PerfectDelay.HorseJump.Rage)) {
         ((IClientPlayerEntity)mc.player).setMountJumpStrength(1.0F);
      }

      if (mc.player.getControlledVehicle() != null
         && mc.player.getControlledVehicle() instanceof Horse
         && this.horse.is(PerfectDelay.HorseJump.Legit)
         && mc.player.getJumpRidingScale() >= 1.0F) {
         mc.options.keyJump.setDown(false);
      }
   }

   private enum HorseJump {
      Legit,
      Rage,
      Off;
   }
}
