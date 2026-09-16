package vcore.core.manager.player;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.IManager;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventPostTick;
import vcore.events.impl.PacketEvent;
import vcore.events.impl.TotemPopEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.AntiBot;

public class CombatManager implements IManager {
   public HashMap<String, Integer> popList = new HashMap<>();

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!Module.fullNullCheck()) {
         if (event.getPacket() instanceof ClientboundEntityEventPacket pac && pac.getEventId() == 35) {
            Entity ent = pac.getEntity(mc.level);
            if (!(ent instanceof Player)) {
               return;
            }

            if (this.popList == null) {
               this.popList = new HashMap<>();
            }

            if (this.popList.get(ent.getName().getString()) == null) {
               this.popList.put(ent.getName().getString(), 1);
            } else if (this.popList.get(ent.getName().getString()) != null) {
               this.popList.put(ent.getName().getString(), this.popList.get(ent.getName().getString()) + 1);
            }

            Vcore.EVENT_BUS.post(new TotemPopEvent((Player)ent, this.popList.get(ent.getName().getString())));
         }
      }
   }

   @EventHandler
   public void onPostTick(EventPostTick event) {
      if (!Module.fullNullCheck()) {
         for (Player player : mc.level.players()) {
            if ((!ModuleManager.antiBot.isEnabled() || ModuleManager.antiBot.mode.getValue() != AntiBot.Mode.Matrix || !AntiBot.isBot(player))
               && player.getHealth() <= 0.0F
               && this.popList.containsKey(player.getName().getString())) {
               this.popList.remove(player.getName().getString(), this.popList.get(player.getName().getString()));
            }
         }
      }
   }

   public int getPops(@NotNull Player entity) {
      return this.popList.get(entity.getName().getString()) == null ? 0 : this.popList.get(entity.getName().getString());
   }

   public List<Player> getTargets(float range) {
      return mc.level
         .players()
         .stream()
         .filter(e -> !e.isDeadOrDying())
         .filter(entityPlayer -> !Managers.FRIEND.isFriend(entityPlayer.getName().getString()))
         .filter(entityPlayer -> entityPlayer != mc.player)
         .filter(entityPlayer -> mc.player.distanceToSqr(entityPlayer) < range * range)
         .sorted(Comparator.comparing(e -> mc.player.distanceToSqr(e)))
         .collect(Collectors.toList());
   }

   @Nullable
   public Player getTarget(float range, @NotNull CombatManager.TargetBy targetBy) {
      Player target = null;
      switch (targetBy) {
         case Distance:
            target = this.getNearestTarget(range);
            break;
         case FOV:
            target = this.getTargetByFOV(range);
            break;
         case Health:
            target = this.getTargetByHealth(range);
      }

      return target;
   }

   @Nullable
   public Player getNearestTarget(float range) {
      return this.getTargets(range).stream().min(Comparator.comparing(t -> mc.player.distanceTo(t))).orElse(null);
   }

   public Player getTargetByHealth(float range) {
      return this.getTargets(range).stream().min(Comparator.comparing(t -> t.getHealth() + t.getAbsorptionAmount())).orElse(null);
   }

   public Player getTargetByFOV(float range) {
      return this.getTargets(range).stream().min(Comparator.comparing(this::getFOVAngle)).orElse(null);
   }

   public Player getTargetByFOV(float range, float fov) {
      return this.getTargets(range)
         .stream()
         .filter(entityPlayer -> this.getFOVAngle(entityPlayer) < fov)
         .min(Comparator.comparing(this::getFOVAngle))
         .orElse(null);
   }

   @Nullable
   public Player getTarget(float range, @NotNull CombatManager.TargetBy targetBy, @NotNull Predicate<Player> predicate) {
      Player target = null;
      switch (targetBy) {
         case Distance:
            target = this.getNearestTarget(range, predicate);
            break;
         case FOV:
            target = this.getTargetByFOV(range, predicate);
            break;
         case Health:
            target = this.getTargetByHealth(range, predicate);
      }

      return target;
   }

   @Nullable
   public Player getNearestTarget(float range, Predicate<Player> predicate) {
      return this.getTargets(range).stream().filter(predicate).min(Comparator.comparing(t -> mc.player.distanceTo(t))).orElse(null);
   }

   public Player getTargetByHealth(float range, Predicate<Player> predicate) {
      return this.getTargets(range).stream().filter(predicate).min(Comparator.comparing(t -> t.getHealth() + t.getAbsorptionAmount())).orElse(null);
   }

   public Player getTargetByFOV(float range, Predicate<Player> predicate) {
      return this.getTargets(range).stream().filter(predicate).min(Comparator.comparing(this::getFOVAngle)).orElse(null);
   }

   private float getFOVAngle(@NotNull LivingEntity e) {
      float yaw = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(e.getZ() - mc.player.getZ(), e.getX() - mc.player.getX())) - 90.0);
      return Math.abs(yaw - Mth.wrapDegrees(mc.player.getYRot()));
   }

   public enum TargetBy {
      Distance,
      FOV,
      Health;
   }
}
