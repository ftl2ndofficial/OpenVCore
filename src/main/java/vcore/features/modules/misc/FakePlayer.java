package vcore.features.modules.misc;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAttack;
import vcore.events.impl.EventSync;
import vcore.events.impl.PacketEvent;
import vcore.events.impl.TotemPopEvent;
import vcore.features.modules.Module;
import vcore.injection.accesors.ILivingEntity;
import vcore.setting.Setting;
import vcore.utility.player.InventoryUtility;
import vcore.utility.world.ExplosionUtility;

public class FakePlayer extends Module {
   public static RemotePlayer fakePlayer;
   private final Setting<Boolean> copyInventory = new Setting<>("CopyInventory", true);
   private final Setting<Boolean> autoTotem = new Setting<>("AutoTotem", true);
   private final Setting<String> name = new Setting<>("Name", "Fake Player");
   private static final long AUTO_TOTEM_DELAY_MS = 5L;
   private long lastTotemPopAt = 0L;
   private int deathTime;

   public FakePlayer() {
      super("FakePlayer", "Fake player for testing PvP modules.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.level != null) {
         fakePlayer = new RemotePlayer(mc.level, new GameProfile(UUID.fromString("66123666-6666-6666-6666-666666666600"), this.name.getValue()));
         this.copyPlayerState();
         if (this.copyInventory.getValue()) {
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().copy());
            fakePlayer.setItemInHand(InteractionHand.OFF_HAND, mc.player.getOffhandItem().copy());
            fakePlayer.getInventory().setItem(36, mc.player.getInventory().getItem(36).copy());
            fakePlayer.getInventory().setItem(37, mc.player.getInventory().getItem(37).copy());
            fakePlayer.getInventory().setItem(38, mc.player.getInventory().getItem(38).copy());
            fakePlayer.getInventory().setItem(39, mc.player.getInventory().getItem(39).copy());
         }

         mc.level.addEntity(fakePlayer);
         fakePlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 9999, 2));
         fakePlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 9999, 4));
         fakePlayer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 9999, 1));
      } else {
         this.disable();
      }
   }

   private void copyPlayerState() {
      fakePlayer.copyPosition(mc.player);
      fakePlayer.yRotO = mc.player.yRotO;
      fakePlayer.xRotO = mc.player.xRotO;
      fakePlayer.yHeadRot = mc.player.yHeadRot;
      fakePlayer.yHeadRotO = mc.player.yHeadRotO;
      fakePlayer.yBodyRot = mc.player.yBodyRot;
      fakePlayer.yBodyRotO = mc.player.yBodyRotO;
      fakePlayer.setPose(mc.player.getPose());
      fakePlayer.setShiftKeyDown(mc.player.isShiftKeyDown());
      fakePlayer.setSprinting(mc.player.isSprinting());
      fakePlayer.setSwimming(mc.player.isSwimming());
      fakePlayer.setOnGround(mc.player.onGround());
      fakePlayer.setDeltaMovement(mc.player.getDeltaMovement());
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (e.getPacket() instanceof ClientboundExplodePacket explosion && fakePlayer != null && fakePlayer.hurtTime == 0) {
         fakePlayer.handleDamageEvent(mc.level.damageSources().generic());
         fakePlayer.setHealth(
            fakePlayer.getHealth()
               + fakePlayer.getAbsorptionAmount()
               - ExplosionUtility.getAutoCrystalDamage(explosion.center(), fakePlayer, 0, false)
         );
         if (fakePlayer.isDeadOrDying() && ((ILivingEntity)fakePlayer).invokeCheckTotemDeathProtection(mc.level.damageSources().generic())) {
            this.handleTotemPop();
         }
      }
   }

   @EventHandler
   public void onSync(EventSync e) {
      if (fakePlayer != null) {
         if (this.autoTotem.getValue()
            && fakePlayer.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING
            && System.currentTimeMillis() - this.lastTotemPopAt >= 5L) {
            fakePlayer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
         }

         if (fakePlayer.isDeadOrDying()) {
            this.deathTime++;
            if (this.deathTime > 10) {
               this.disable();
            }
         }
      }
   }

   @EventHandler
   public void onAttack(EventAttack e) {
      if (fakePlayer != null && e.getEntity() == fakePlayer && fakePlayer.hurtTime == 0 && !e.isPre()) {
         mc.level
            .playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 1.0F);
         if (mc.player.fallDistance > 0.0F || ModuleManager.criticals.isEnabled()) {
            mc.level
               .playSound(
                  mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F
               );
         }

         fakePlayer.handleDamageEvent(mc.level.damageSources().generic());
         if (ModuleManager.aura.getAttackCooldown() >= 0.85) {
            fakePlayer.setHealth(
               fakePlayer.getHealth() + fakePlayer.getAbsorptionAmount() - InventoryUtility.getHitDamage(mc.player.getMainHandItem(), fakePlayer)
            );
         } else {
            fakePlayer.setHealth(fakePlayer.getHealth() + fakePlayer.getAbsorptionAmount() - 1.0F);
         }

         if (fakePlayer.isDeadOrDying() && ((ILivingEntity)fakePlayer).invokeCheckTotemDeathProtection(mc.level.damageSources().generic())) {
            this.handleTotemPop();
         }
      }
   }

   private void handleTotemPop() {
      this.lastTotemPopAt = System.currentTimeMillis();
      fakePlayer.setHealth(10.0F);
      this.applyTotemStatus();
      String playerName = fakePlayer.getName().getString();
      int pops = Managers.COMBAT.popList.merge(playerName, 1, Integer::sum);
      Vcore.EVENT_BUS.post(new TotemPopEvent(fakePlayer, pops));
   }

   private void applyTotemStatus() {
      if (mc.player != null && mc.player.connection != null) {
         new ClientboundEntityEventPacket(fakePlayer, (byte)35).handle(mc.player.connection);
      }
   }

   @Override
   public void onDisable() {
      if (fakePlayer != null) {
         Managers.COMBAT.popList.remove(fakePlayer.getName().getString());
         fakePlayer.discard();
         fakePlayer = null;
         this.deathTime = 0;
         this.lastTotemPopAt = 0L;
      }
   }
}
