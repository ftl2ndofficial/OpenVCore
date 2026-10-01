package vcore.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAfterRotate;
import vcore.events.impl.EventPostSync;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.utility.Timer;

public final class AutoBuff extends Module {
   private final Setting<Boolean> strength = new Setting<>("Strength", true);
   private final Setting<Boolean> speed = new Setting<>("Speed", true);
   private final Setting<Boolean> fire = new Setting<>("FireResistance", true);
   private final Setting<BooleanSettingGroup> heal = new Setting<>("InstantHealing", new BooleanSettingGroup(false));
   private final Setting<Integer> healthH = new Setting<>("Health", 8, 0, 20).addToGroup(this.heal);
   private final Setting<BooleanSettingGroup> regen = new Setting<>("Regeneration", new BooleanSettingGroup(false));
   private final Setting<AutoBuff.TriggerOn> triggerOn = new Setting<>("Trigger", AutoBuff.TriggerOn.LackOfRegen).addToGroup(this.regen);
   private final Setting<Integer> healthR = new Setting<>("HP", 8, 0, 20, v -> this.triggerOn.is(AutoBuff.TriggerOn.Health)).addToGroup(this.regen);
   private final Setting<Boolean> onDaGround = new Setting<>("OnlyOnGround", true);
   private final Setting<Boolean> pauseAura = new Setting<>("PauseAura", false);
   public Timer timer = new Timer();
   private boolean spoofed = false;

   public AutoBuff() {
      super("AutoBuff", "Auto throws potions.", Module.Category.COMBAT);
   }

   public static int getPotionSlot(AutoBuff.Potions potion) {
      for (int i = 0; i < 9; i++) {
         if (isStackPotion(mc.player.getInventory().getItem(i), potion)) {
            return i;
         }
      }

      return -1;
   }

   public static boolean isPotionOnHotBar(AutoBuff.Potions potions) {
      return getPotionSlot(potions) != -1;
   }

   public static boolean isStackPotion(ItemStack stack, AutoBuff.Potions potion) {
      if (stack == null) {
         return false;
      }

      if (stack.getItem() instanceof SplashPotionItem) {
         PotionContents potionContentsComponent = (PotionContents)stack.getOrDefault(
            DataComponents.POTION_CONTENTS, PotionContents.EMPTY
         );
         Holder<MobEffect> id = null;
         switch (potion) {
            case STRENGTH:
               id = MobEffects.STRENGTH;
               break;
            case SPEED:
               id = MobEffects.SPEED;
               break;
            case FIRERES:
               id = MobEffects.FIRE_RESISTANCE;
               break;
            case HEAL:
               id = MobEffects.INSTANT_HEALTH;
               break;
            case REGEN:
               id = MobEffects.REGENERATION;
         }

         for (MobEffectInstance effect : potionContentsComponent.getAllEffects()) {
            if (effect.getEffect() == id) {
               return true;
            }
         }
      }

      return false;
   }

   @EventHandler
   public void onPostRotationSet(EventAfterRotate event) {
      if (Aura.target == null || !(mc.player.getAttackStrengthScale(1.0F) > 0.5F)) {
         if (mc.player.tickCount > 80 && this.shouldThrow()) {
            mc.player.setXRot(90.0F);
            this.spoofed = true;
         }
      }
   }

   private boolean shouldThrow() {
      return !mc.player.hasEffect(MobEffects.SPEED) && isPotionOnHotBar(AutoBuff.Potions.SPEED) && this.speed.getValue()
         || !mc.player.hasEffect(MobEffects.STRENGTH) && isPotionOnHotBar(AutoBuff.Potions.STRENGTH) && this.strength.getValue()
         || !mc.player.hasEffect(MobEffects.FIRE_RESISTANCE) && isPotionOnHotBar(AutoBuff.Potions.FIRERES) && this.fire.getValue()
         || mc.player.getHealth() + mc.player.getAbsorptionAmount() < this.healthH.getValue().intValue()
            && isPotionOnHotBar(AutoBuff.Potions.HEAL)
            && this.heal.getValue().isEnabled()
         || !mc.player.hasEffect(MobEffects.REGENERATION)
            && this.triggerOn.is(AutoBuff.TriggerOn.LackOfRegen)
            && isPotionOnHotBar(AutoBuff.Potions.REGEN)
            && this.regen.getValue().isEnabled()
         || mc.player.getHealth() + mc.player.getAbsorptionAmount() < this.healthR.getValue().intValue()
            && this.triggerOn.is(AutoBuff.TriggerOn.Health)
            && isPotionOnHotBar(AutoBuff.Potions.REGEN)
            && this.regen.getValue().isEnabled();
   }

   @EventHandler
   public void onPostSync(EventPostSync e) {
      if (Aura.target == null || !(mc.player.getAttackStrengthScale(1.0F) > 0.5F)) {
         if (!this.onDaGround.getValue() || mc.player.onGround()) {
            if (mc.player.tickCount > 80 && this.shouldThrow() && this.timer.passedMs(1000L) && this.spoofed) {
               if (!mc.player.hasEffect(MobEffects.SPEED) && isPotionOnHotBar(AutoBuff.Potions.SPEED) && this.speed.getValue()) {
                  this.throwPotion(AutoBuff.Potions.SPEED);
               }

               if (!mc.player.hasEffect(MobEffects.STRENGTH) && isPotionOnHotBar(AutoBuff.Potions.STRENGTH) && this.strength.getValue()) {
                  this.throwPotion(AutoBuff.Potions.STRENGTH);
               }

               if (!mc.player.hasEffect(MobEffects.FIRE_RESISTANCE) && isPotionOnHotBar(AutoBuff.Potions.FIRERES) && this.fire.getValue()) {
                  this.throwPotion(AutoBuff.Potions.FIRERES);
               }

               if (mc.player.getHealth() + mc.player.getAbsorptionAmount() < this.healthH.getValue().intValue()
                  && this.heal.getValue().isEnabled()
                  && isPotionOnHotBar(AutoBuff.Potions.HEAL)) {
                  this.throwPotion(AutoBuff.Potions.HEAL);
               }

               if ((
                     !mc.player.hasEffect(MobEffects.REGENERATION) && this.triggerOn.is(AutoBuff.TriggerOn.LackOfRegen)
                        || mc.player.getHealth() + mc.player.getAbsorptionAmount() < this.healthR.getValue().intValue()
                           && this.triggerOn.is(AutoBuff.TriggerOn.Health)
                  )
                  && isPotionOnHotBar(AutoBuff.Potions.REGEN)
                  && this.regen.getValue().isEnabled()) {
                  this.throwPotion(AutoBuff.Potions.REGEN);
               }

               this.sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
               this.timer.reset();
               this.spoofed = false;
            }
         }
      }
   }

   public void throwPotion(AutoBuff.Potions potion) {
      if (this.pauseAura.getValue()) {
         ModuleManager.aura.pause();
      }

      this.sendPacket(new ServerboundSetCarriedItemPacket(getPotionSlot(potion)));
      this.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
   }

   public enum Potions {
      STRENGTH,
      SPEED,
      FIRERES,
      HEAL,
      REGEN;
   }

   public enum TriggerOn {
      LackOfRegen,
      Health;
   }
}
