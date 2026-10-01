package vcore.features.modules.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.buffer.Unpooled;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventTick;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.injection.accesors.IClientPlayerEntity;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.player.InputUtility;
import vcore.utility.render.Render3DEngine;

public final class Criticals extends Module {
   public final Setting<Criticals.Mode> mode = new Setting<>("Mode", Criticals.Mode.UpdatedNCP);
   public final Setting<Integer> blinkDelayMin = new Setting<>("DelayMin", 300, 0, 1000, v -> this.mode.is(Criticals.Mode.Blink));
   public final Setting<Integer> blinkDelayMax = new Setting<>("DelayMax", 600, 0, 1000, v -> this.mode.is(Criticals.Mode.Blink));
   public final Setting<Float> blinkRange = new Setting<>("Range", 4.0F, 0.0F, 10.0F, v -> this.mode.is(Criticals.Mode.Blink)).step(0.1F);
   public final Setting<Boolean> blinkVisual = new Setting<>("Visual", true, v -> this.mode.is(Criticals.Mode.Blink));
   public final Setting<ColorSetting> blinkLineColor = new Setting<>(
      "Line", new ColorSetting(new Color(0, 128, 255, 255)), v -> this.mode.is(Criticals.Mode.Blink) && this.blinkVisual.getValue()
   );
   public final Setting<ColorSetting> blinkBoxColor = new Setting<>(
      "BoxColor", new ColorSetting(new Color(36, 32, 147, 87)), v -> this.mode.is(Criticals.Mode.Blink) && this.blinkVisual.getValue()
   );
   public static boolean cancelCrit;
   private static final boolean CANCEL_PACKETS = true;
   private Vec3 freezePos = Vec3.ZERO;
   private final Deque<Criticals.QueuedPacket> blinkPackets = new ArrayDeque<>();
   private final Object blinkPacketLock = new Object();
   private int nextBlinkDelay = this.getRandomBlinkDelay();
   private boolean blinkEnemyInRange;
   private boolean blinkInState;

   public Criticals() {
      super("Criticals", "Makes every hit critical.", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.resetAirStuck();
      this.blinkEnemyInRange = false;
      this.blinkInState = false;
      this.flushBlinkPackets();
   }

   @Override
   public void onLogout() {
      this.resetAirStuck();
      this.blinkEnemyInRange = false;
      this.blinkInState = false;
      this.clearBlinkPackets();
   }

   @Override
   public void onRender3D(PoseStack stack) {
      if (this.mode.is(Criticals.Mode.Blink) && this.blinkVisual.getValue() && this.hasBlinkPackets() && mc.player != null) {
         this.renderBlinkLine();
         this.renderBlinkEsp(stack);
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.@NotNull Send event) {
      if (event.getPacket() instanceof ServerboundMovePlayerPacket && this.isAirStuckActive()) {
         event.cancel();
      } else if (!this.handleBlinkPacket(event)) {
         if (event.getPacket() instanceof ServerboundInteractPacket packet && getInteractType(packet) == Criticals.InteractType.ATTACK) {
            Entity ent = getEntity(event.getPacket());
            if (ent == null || ent instanceof EndCrystal || cancelCrit) {
               return;
            }

            this.doCrit();
         }
      }
   }

   @EventHandler
   public void onTick(EventTick event) {
      if (fullNullCheck()) {
         this.blinkEnemyInRange = false;
         this.blinkInState = false;
         this.clearBlinkPackets();
         this.resetAirStuck();
      } else {
         this.updateBlinkState();
         if (!this.shouldAirStuck()) {
            this.resetAirStuck();
         } else {
            if (this.freezePos == Vec3.ZERO) {
               this.freezePos = mc.player.position();
            }

            LocalPlayer player = mc.player;
            if (player == null) {
               this.resetAirStuck();
            } else {
               player.setDeltaMovement(0.0, 0.0, 0.0);
               if (player.input != null) {
                  InputUtility.clearMovement(player.input);
               }

               player.setPosRaw(player.getX(), this.freezePos.y, player.getZ());
            }
         }
      }
   }

   public void doCrit() {
      if (!this.isDisabled() && mc.player != null && mc.level != null) {
         if (!this.mode.is(Criticals.Mode.AirStuck) && !this.mode.is(Criticals.Mode.Blink)) {
            if ((mc.player.onGround() || mc.player.getAbilities().flying || this.mode.is(Criticals.Mode.Grim))
               && !mc.player.isInLava()
               && !mc.player.isUnderWater()) {
               switch ((Criticals.Mode)this.mode.getValue()) {
                  case Grim:
                     if (!mc.player.onGround()) {
                        this.critPacket(-1.0E-6, true);
                     }
                  case AirStuck:
                  case Blink:
                  default:
                     break;
                  case UpdatedNCP:
                     this.critPacket(2.71875E-7, false);
                     this.critPacket(0.0, false);
               }
            }
         }
      }
   }

   private void critPacket(double yDelta, boolean full) {
      if (!full) {
         this.sendPacket(new Pos(mc.player.getX(), mc.player.getY() + yDelta, mc.player.getZ(), false, mc.player.horizontalCollision));
      } else {
         this.sendPacket(
            new PosRot(
               mc.player.getX(),
               mc.player.getY() + yDelta,
               mc.player.getZ(),
               ((IClientPlayerEntity)mc.player).getLastYaw(),
               ((IClientPlayerEntity)mc.player).getLastPitch(),
               false,
               mc.player.horizontalCollision
            )
         );
      }
   }

   public static Entity getEntity(@NotNull Packet<?> packet) {
      if (packet instanceof ServerboundInteractPacket interactPacket) {
         int id = ((vcore.injection.accesors.IPlayerInteractEntityC2SPacket) interactPacket).getEntityId();
         return mc.level != null ? mc.level.getEntity(id) : null;
      }
      return null;
   }

   public static Criticals.InteractType getInteractType(@NotNull Packet<?> packet) {
      if (packet instanceof ServerboundInteractPacket interactPacket) {
         final Criticals.InteractType[] result = new Criticals.InteractType[1];
         interactPacket.dispatch(new ServerboundInteractPacket.Handler() {
            @Override
            public void onInteraction(net.minecraft.world.InteractionHand hand) {
               result[0] = Criticals.InteractType.INTERACT;
            }

            @Override
            public void onInteraction(net.minecraft.world.InteractionHand hand, Vec3 interactionLocation) {
               result[0] = Criticals.InteractType.INTERACT_AT;
            }

            @Override
            public void onAttack() {
               result[0] = Criticals.InteractType.ATTACK;
            }
         });
         return result[0];
      }
      return null;
   }

   private boolean shouldAirStuck() {
      if (!this.mode.is(Criticals.Mode.AirStuck) || mc.player == null || mc.level == null) {
         return false;
      } else if (!ModuleManager.aura.isEnabled() || Aura.target == null) {
         return false;
      } else if (ModuleManager.aura.pauseWhileEating.getValue() && this.isConsumingItem()) {
         return false;
      } else if (mc.player.isFallFlying()) {
         return false;
      } else if (mc.player.onGround()) {
         return false;
      } else {
         return mc.player.fallDistance <= 0.0F ? false : mc.player.distanceTo(Aura.target) <= ModuleManager.aura.attackRange.getValue();
      }
   }

   private boolean isAirStuckActive() {
      return this.mode.is(Criticals.Mode.AirStuck) && this.freezePos != Vec3.ZERO;
   }

   private void resetAirStuck() {
      this.freezePos = Vec3.ZERO;
   }

   private boolean isConsumingItem() {
      if (mc.player != null && mc.player.isUsingItem()) {
         ItemUseAnimation useAction = mc.player.getUseItem().getUseAnimation();
         return useAction == ItemUseAnimation.EAT || useAction == ItemUseAnimation.DRINK;
      } else {
         return false;
      }
   }

   private void updateBlinkState() {
      if (this.mode.is(Criticals.Mode.Blink) && mc.player != null && mc.level != null) {
         this.blinkEnemyInRange = this.hasBlinkEnemyInRange();
         boolean shouldQueue = this.shouldBlinkQueuePackets();
         boolean delayExpired = shouldQueue && this.isBlinkDelayExpired();
         this.blinkInState = shouldQueue && !delayExpired;
         if (!shouldQueue) {
            if (this.hasBlinkPackets()) {
               this.flushBlinkPackets();
            }
         } else {
            if (delayExpired) {
               this.nextBlinkDelay = this.getRandomBlinkDelay();
               if (this.hasBlinkPackets()) {
                  this.flushBlinkPackets();
               }

               this.blinkInState = false;
            }
         }
      } else {
         this.blinkEnemyInRange = false;
         this.blinkInState = false;
         if (this.hasBlinkPackets()) {
            this.flushBlinkPackets();
         }
      }
   }

   private boolean handleBlinkPacket(PacketEvent.@NotNull Send event) {
      if (!this.mode.is(Criticals.Mode.Blink)) {
         this.blinkInState = false;
         if (this.hasBlinkPackets()) {
            this.flushBlinkPackets();
         }

         return false;
      } else if (mc.player != null && mc.level != null) {
         boolean shouldQueue = this.shouldBlinkQueuePackets();
         if (!shouldQueue) {
            this.blinkInState = false;
            if (this.hasBlinkPackets()) {
               this.flushBlinkPackets();
            }

            return false;
         } else if (this.isBlinkDelayExpired()) {
            this.nextBlinkDelay = this.getRandomBlinkDelay();
            this.blinkInState = false;
            if (this.hasBlinkPackets()) {
               this.flushBlinkPackets();
            }

            return false;
         } else {
            Packet<?> packet = event.getPacket();
            if (this.shouldPassBlinkPacket(packet)) {
               this.blinkInState = true;
               return false;
            } else {
               this.blinkInState = true;
               event.cancel();
               this.queueBlinkPacket(new Criticals.QueuedPacket(packet, System.currentTimeMillis()));
               return true;
            }
         }
      } else {
         this.blinkInState = false;
         this.blinkEnemyInRange = false;
         this.clearBlinkPackets();
         return false;
      }
   }

   private boolean shouldBlinkQueuePackets() {
      return this.mode.is(Criticals.Mode.Blink) && this.blinkEnemyInRange && !this.wouldDoBlinkCriticalHit(true);
   }

   private boolean wouldDoBlinkCriticalHit(boolean ignoreSprint) {
      return this.canDoBlinkCriticalHit(false, ignoreSprint) && mc.player.fallDistance > 0.0F;
   }

   private boolean canDoBlinkCriticalHit(boolean ignoreOnGround, boolean ignoreSprint) {
      return this.allowsBlinkCriticalHit(ignoreOnGround) && mc.player.getAttackStrengthScale(0.5F) > 0.9F && (!mc.player.isSprinting() || ignoreSprint);
   }

   private boolean allowsBlinkCriticalHit(boolean ignoreOnGround) {
      return mc.player == null
         ? false
         : !ModuleManager.flight.isEnabled()
            && !mc.player.isInLava()
            && !mc.player.isInWater()
            && !mc.player.isUnderWater()
            && !mc.player.isPassenger()
            && !Managers.PLAYER.isInWeb()
            && !mc.player.hasEffect(MobEffects.LEVITATION)
            && !mc.player.hasEffect(MobEffects.BLINDNESS)
            && !mc.player.hasEffect(MobEffects.SLOW_FALLING)
            && !mc.player.onClimbable()
            && !mc.player.isNoGravity()
            && !mc.player.isUsingItem()
            && !mc.player.getAbilities().flying
            && (ignoreOnGround || !mc.player.onGround());
   }

   private boolean hasBlinkEnemyInRange() {
      if (mc.player != null && mc.level != null) {
         if (this.isBlinkTargetInRange(Aura.target, this.blinkRange.getValue())) {
            return true;
         }

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (this.isValidBlinkEnemy(entity) && this.isBlinkTargetInRange(entity, this.blinkRange.getValue())) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isBlinkTargetInRange(Entity entity, float range) {
      if (entity != null && entity != mc.player && entity.isAlive()) {
         Vec3 eyePos = mc.player.getEyePosition();
         AABB box = entity.getBoundingBox();
         Vec3 closestPoint = new Vec3(
            Mth.clamp(eyePos.x, box.minX, box.maxX), Mth.clamp(eyePos.y, box.minY, box.maxY), Mth.clamp(eyePos.z, box.minZ, box.maxZ)
         );
         return eyePos.distanceToSqr(closestPoint) <= range * range;
      } else {
         return false;
      }
   }

   private boolean isValidBlinkEnemy(Entity entity) {
      if (!(entity instanceof LivingEntity) || entity == mc.player || !entity.isAlive()) {
         return false;
      } else if (!(entity instanceof ArmorStand) && !(entity instanceof Cat)) {
         return entity instanceof Player player && Managers.FRIEND.isFriend(player)
            ? false
            : !(entity instanceof Player p && p.isCreative());
      } else {
         return false;
      }
   }

   private boolean isBlinkDelayExpired() {
      Criticals.QueuedPacket firstPacket = this.peekFirstBlinkPacket();
      return firstPacket == null ? false : System.currentTimeMillis() - firstPacket.timestamp() >= this.nextBlinkDelay;
   }

   private int getRandomBlinkDelay() {
      int min = Math.min(this.blinkDelayMin.getValue(), this.blinkDelayMax.getValue());
      int max = Math.max(this.blinkDelayMin.getValue(), this.blinkDelayMax.getValue());
      return ThreadLocalRandom.current().nextInt(min, max + 1);
   }

   private boolean shouldPassBlinkPacket(Packet<?> packet) {
      return packet instanceof ServerboundUseItemOnPacket
         || packet instanceof ServerboundPlayerActionPacket
         || packet instanceof ServerboundSignUpdatePacket
         || packet instanceof ServerboundInteractPacket
         || packet instanceof ServerboundSwingPacket
         || packet instanceof ServerboundResourcePackPacket;
   }

   private void flushBlinkPackets() {
      List<Criticals.QueuedPacket> packetsToFlush;
      synchronized (this.blinkPacketLock) {
         packetsToFlush = new ArrayList<>(this.blinkPackets);
         this.blinkPackets.clear();
      }

      for (Criticals.QueuedPacket queuedPacket : packetsToFlush) {
         this.sendPacketSilent(queuedPacket.packet());
      }
   }

   private void renderBlinkLine() {
      Color color = this.blinkLineColor.getValue().getColorObject();
      if (color.getAlpha() > 0) {
         List<Vec3> positions = this.getQueuedBlinkPositions();
         if (positions.size() >= 2) {
            for (int i = 0; i < positions.size() - 1; i++) {
               Render3DEngine.drawLine(positions.get(i), positions.get(i + 1), color);
            }
         }
      }
   }

   private void renderBlinkEsp(PoseStack stack) {
      Vec3 pos = this.getFirstBlinkPosition();
      if (pos != null) {
         Entity player = mc.player;
         EntityDimensions dimensions = player.getDimensions(player.getPose());
         double halfWidth = dimensions.width() / 2.0;
         AABB box = new AABB(pos.x - halfWidth, pos.y, pos.z - halfWidth, pos.x + halfWidth, pos.y + dimensions.height(), pos.z + halfWidth).inflate(0.05);
         Render3DEngine.drawFilledBox(stack, box, this.blinkBoxColor.getValue().getColorObject());
      }
   }

   @NotNull
   private List<Vec3> getQueuedBlinkPositions() {
      List<Vec3> positions = new ArrayList<>();

      for (Criticals.QueuedPacket snapshot : this.getBlinkPacketSnapshot()) {
         if (snapshot.packet() instanceof ServerboundMovePlayerPacket movePacket && movePacket.hasPosition()) {
            positions.add(new Vec3(movePacket.getX(mc.player.getX()), movePacket.getY(mc.player.getY()), movePacket.getZ(mc.player.getZ())));
         }
      }

      return positions;
   }

   private Vec3 getFirstBlinkPosition() {
      for (Criticals.QueuedPacket snapshot : this.getBlinkPacketSnapshot()) {
         if (snapshot.packet() instanceof ServerboundMovePlayerPacket movePacket && movePacket.hasPosition()) {
            return new Vec3(movePacket.getX(mc.player.getX()), movePacket.getY(mc.player.getY()), movePacket.getZ(mc.player.getZ()));
         }
      }

      return null;
   }

   private boolean hasBlinkPackets() {
      synchronized (this.blinkPacketLock) {
         return !this.blinkPackets.isEmpty();
      }
   }

   private void clearBlinkPackets() {
      synchronized (this.blinkPacketLock) {
         this.blinkPackets.clear();
      }
   }

   private void queueBlinkPacket(Criticals.QueuedPacket queuedPacket) {
      synchronized (this.blinkPacketLock) {
         this.blinkPackets.addLast(queuedPacket);
      }
   }

   private Criticals.QueuedPacket peekFirstBlinkPacket() {
      synchronized (this.blinkPacketLock) {
         return this.blinkPackets.peekFirst();
      }
   }

   @NotNull
   private List<Criticals.QueuedPacket> getBlinkPacketSnapshot() {
      synchronized (this.blinkPacketLock) {
         return new ArrayList<>(this.blinkPackets);
      }
   }

   public enum InteractType {
      INTERACT,
      ATTACK,
      INTERACT_AT;
   }

   public enum Mode {
      Grim,
      AirStuck,
      UpdatedNCP,
      Blink;
   }

   private record QueuedPacket(Packet<?> packet, long timestamp) {
   }
}
