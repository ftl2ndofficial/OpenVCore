package vcore.features.modules.combat;

import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public final class CrystalOptimizer extends Module {
   private static final long MIN_BLATANT_TIME_MS = 150L;
   private static final double CHAIN_CRYSTAL_RADIUS_SQ = 144.0;
   private final Setting<Boolean> blatant = new Setting<>("Blatant", true);
   private final Map<Integer, Long> inhibitedCrystals = new ConcurrentHashMap<>();

   public CrystalOptimizer() {
      super("CrystalOptimizer", "Removes attacked crystals client-side faster.", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.inhibitedCrystals.clear();
   }

   @Override
   public void onLogout() {
      this.inhibitedCrystals.clear();
   }

   @Override
   public void onUpdate() {
      if (fullNullCheck()) {
         this.inhibitedCrystals.clear();
      } else {
         if (this.blatant.getValue()) {
            this.pruneInhibitedCrystals();
         } else {
            this.inhibitedCrystals.clear();
         }
      }
   }

   @EventHandler
   private void onPacketSend(PacketEvent.@NotNull Send event) {
      if (!fullNullCheck()) {
         if (event.getPacket() instanceof ServerboundInteractPacket packet) {
            if (Criticals.getInteractType(packet) == Criticals.InteractType.ATTACK) {
               if (this.blatant.getValue()) {
                  this.pruneInhibitedCrystals();
               }

               Entity entity = Criticals.getEntity(packet);
               if (this.blatant.getValue()) {
                  this.handleBlatantAttack(event, this.getEntityId(packet), entity);
               } else {
                  if (entity instanceof EndCrystal crystal && !this.cantBreakCrystal()) {
                     this.removeCrystal(crystal);
                  }
               }
            }
         }
      }
   }

   private void handleBlatantAttack(PacketEvent.@NotNull Send event, int entityId, Entity entity) {
      if (this.shouldBlatantInhibit(entityId)) {
         if (entity instanceof EndCrystal crystal) {
            this.removeCrystal(crystal);
         }

         event.cancel();
      } else if (entity instanceof EndCrystal crystal) {
         if (!this.cantBreakCrystal()) {
            this.markInhibited(crystal.getId());
            this.markNearbyCrystalsDead(crystal.getX(), crystal.getY(), crystal.getZ());
            this.removeCrystal(crystal);
         }
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.@NotNull Receive event) {
      if (!fullNullCheck()) {
         if (this.blatant.getValue()) {
            this.pruneInhibitedCrystals();
            if (event.getPacket() instanceof ClientboundExplodePacket explosion) {
               this.markNearbyCrystalsDead(explosion.center().x, explosion.center().y, explosion.center().z);
            }
         }
      }
   }

   private void removeCrystal(@NotNull Entity entity) {
      entity.discard();
      entity.setRemoved(RemovalReason.KILLED);
      entity.onClientRemoval();
   }

   public boolean isAttackInhibited(Entity entity) {
      if (!fullNullCheck() && !this.isOff() && this.blatant.getValue() && entity instanceof EndCrystal crystal) {
         this.pruneInhibitedCrystals();
         if (!this.isInhibited(crystal.getId())) {
            return false;
         }

         this.removeCrystal(crystal);
         return true;
      } else {
         return false;
      }
   }

   public boolean shouldIgnoreForPlacement(Entity entity) {
      if (!fullNullCheck() && !this.isOff() && this.blatant.getValue() && entity instanceof EndCrystal crystal) {
         this.pruneInhibitedCrystals();
         return this.isInhibited(crystal.getId());
      } else {
         return false;
      }
   }

   private void markNearbyCrystalsDead(double x, double y, double z) {
      long time = System.currentTimeMillis();

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof EndCrystal crystal && crystal.distanceToSqr(x, y, z) <= 144.0 && !this.isInhibited(crystal.getId())) {
            this.inhibitedCrystals.put(crystal.getId(), time);
         }
      }
   }

   private void markInhibited(int entityId) {
      this.inhibitedCrystals.put(entityId, System.currentTimeMillis());
   }

   private void pruneInhibitedCrystals() {
      long now = System.currentTimeMillis();
      this.inhibitedCrystals.entrySet().removeIf(entry -> now - entry.getValue() > this.getBlatantTimeMs());
   }

   private boolean shouldBlatantInhibit(int entityId) {
      return this.blatant.getValue() && this.isInhibited(entityId);
   }

   private boolean isInhibited(int entityId) {
      return this.inhibitedCrystals.containsKey(entityId);
   }

   private long getBlatantTimeMs() {
      return Math.max(150L, Managers.SERVER.getPing() * 2L);
   }

   private int getEntityId(@NotNull ServerboundInteractPacket packet) {
      return ((vcore.injection.accesors.IPlayerInteractEntityC2SPacket)packet).getEntityId();
   }

   private boolean cantBreakCrystal() {
      MobEffectInstance weakness = mc.player.getEffect(MobEffects.WEAKNESS);
      MobEffectInstance strength = mc.player.getEffect(MobEffects.STRENGTH);
      return weakness != null && (strength == null || strength.getAmplifier() <= weakness.getAmplifier()) && !this.isTool(mc.player.getMainHandItem());
   }

   private boolean isTool(@NotNull ItemStack stack) {
      return stack.is(ItemTags.SWORDS)
         || stack.is(ItemTags.PICKAXES)
         || stack.getItem() instanceof AxeItem
         || stack.getItem() instanceof ShovelItem;
   }
}
