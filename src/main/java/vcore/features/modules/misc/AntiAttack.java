package vcore.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Criticals;
import vcore.setting.Setting;

public final class AntiAttack extends Module {
   private final Setting<Boolean> friend = new Setting<>("Friend", true);
   private final Setting<Boolean> zoglin = new Setting<>("Zoglin", true);
   private final Setting<Boolean> villager = new Setting<>("Villager", false);
   private final Setting<Boolean> oneHp = new Setting<>("OneHp", false);
   private final Setting<Float> hp = new Setting<>("Hp", 1.0F, 0.0F, 20.0F, v -> this.oneHp.getValue());

   public AntiAttack() {
      super("AntiAttack", "Prevents unnecessary attacks.", Module.Category.PLAYER);
   }

   @EventHandler
   private void onPacketSend(PacketEvent.@NotNull Send e) {
      if (e.getPacket() instanceof ServerboundInteractPacket pac) {
         Entity entity = Criticals.getEntity(pac);
         if (entity == null) {
            return;
         }

         if (Managers.FRIEND.isFriend(entity.getName().getString()) && this.friend.getValue()) {
            e.cancel();
         }

         if (entity instanceof ZombifiedPiglin && this.zoglin.getValue()) {
            e.cancel();
         }

         if (entity instanceof Villager && this.villager.getValue()) {
            e.cancel();
         } else if (this.oneHp.getValue() && entity instanceof LivingEntity lent && lent.getHealth() <= this.hp.getValue()) {
            e.cancel();
         }
      }
   }
}
