package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import vcore.events.impl.UseTridentEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class TridentBoost extends Module {
   private final Setting<TridentBoost.Mode> mode = new Setting<>("Mode", TridentBoost.Mode.Motion);
   private final Setting<Float> factor = new Setting<>("Factor", 1.0F, 0.1F, 20.0F);
   public final Setting<Integer> cooldown = new Setting<>("Cooldown", 10, 0, 20);
   public final Setting<Boolean> anyWeather = new Setting<>("AnyWeather", true);

   public TridentBoost() {
      super("TridentBoost", "Boost with trident.", Module.Category.MOVEMENT);
   }

   @EventHandler
   public void onUseTrident(UseTridentEvent e) {
      if (mc.player.getTicksUsingItem() >= this.cooldown.getValue()) {
         float j = EnchantmentHelper.getTridentSpinAttackStrength(mc.player.getUseItem(), mc.player);
         if ((this.anyWeather.getValue() || mc.player.isInWaterOrRain()) && j > 0.0F) {
            float f = mc.player.getYRot();
            float g = mc.player.getXRot();
            float speedX = -Mth.sin(f * (float) (Math.PI / 180.0)) * Mth.cos(g * (float) (Math.PI / 180.0));
            float speedY = -Mth.sin(g * (float) (Math.PI / 180.0));
            float speedZ = Mth.cos(f * (float) (Math.PI / 180.0)) * Mth.cos(g * (float) (Math.PI / 180.0));
            float plannedSpeed = Mth.sqrt(speedX * speedX + speedY * speedY + speedZ * speedZ);
            float n = this.mode.is(TridentBoost.Mode.Factor) ? this.factor.getValue() * 3.0F * ((1.0F + j) / 4.0F) : this.factor.getValue();
            speedX *= n / plannedSpeed;
            speedY *= n / plannedSpeed;
            speedZ *= n / plannedSpeed;
            mc.player.push(speedX, speedY, speedZ);
            mc.player.startAutoSpinAttack(20, 8.0F, mc.player.getUseItem());
            if (mc.player.onGround()) {
               mc.player.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
            }

            Holder<SoundEvent> registryEntry = EnchantmentHelper.pickHighestLevel(mc.player.getUseItem(), EnchantmentEffectComponents.TRIDENT_SOUND)
               .orElse(SoundEvents.TRIDENT_THROW);
            mc.level.playSound(null, mc.player, (SoundEvent)registryEntry.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
         }
      }

      e.cancel();
   }

   private enum Mode {
      Motion,
      Factor;
   }
}
