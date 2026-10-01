package vcore.features.modules.combat;

import java.awt.Color;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vcore.core.InputBlocker;
import vcore.core.Managers;
import vcore.core.manager.client.AsyncManager;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventFixVelocity;
import vcore.events.impl.EventKeyboardInput;
import vcore.events.impl.EventSync;
import vcore.events.impl.PacketEvent;
import vcore.events.impl.TotemPopEvent;
import vcore.features.modules.Module;
import vcore.injection.accesors.IInteractionManager;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.setting.impl.SettingGroup;
import vcore.utility.player.InteractionUtility;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.MovementUtility;
import vcore.utility.player.PlayerUtility;
import vcore.utility.player.SearchInvResult;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public class AutoCart extends Module {
   private static final Color CROSSBOW_DEBUG_FILL = new Color(255, 0, 0, 100);
   private static final int HOTBAR_SLOT_SIZE = 20;
   private static final int HOTBAR_Y_OFFSET = 22;
   private static final int HOTBAR_LEFT_OFFSET = 91;
   private static final int TICK_LENGTH_MS = 50;
   private static final long CART_AURA_TARGET_MEMORY_MS = 1000L;
   private final Setting<AutoCart.Mode> mode = new Setting<>("Mode", AutoCart.Mode.Bow);
   private final Setting<Float> maxDistance = new Setting<>("Max Distance", 4.5F, 2.0F, 6.0F, v -> this.mode.is(AutoCart.Mode.Bow));
   private final Setting<Integer> startDelay = new Setting<>("Start Delay", 25, 0, 60, v -> this.mode.is(AutoCart.Mode.Bow));
   private final Setting<Bind> activeBind = new Setting<>("Active Bind", new Bind(-1, false, false), v -> this.mode.is(AutoCart.Mode.CrossBow));
   private final Setting<Integer> delay = new Setting<>("Delay", 25, 0, 100, v -> this.mode.is(AutoCart.Mode.Bow) || this.mode.is(AutoCart.Mode.CrossBow));
   private final Setting<Integer> cartAuraDelay = new Setting<>("Cart Aura Delay", 0, 0, 20, v -> this.isCartAuraEnabled());
   private final Setting<Integer> refillSlot = new Setting<>("Slot", 9, 1, 9, v -> this.isRefillSlotVisible());
   private final Setting<Boolean> swapBack = new Setting<>("Swap Back", true);
   private final Setting<Boolean> totemCheck = new Setting<>("Totem Check", true, v -> this.isCartAuraEnabled());
   private final Setting<Boolean> changeLook = new Setting<>("Change Look", false);
   private final Setting<Boolean> cartAura = new Setting<>("Cart Aura", false, v -> this.mode.is(AutoCart.Mode.CrossBow));
   private final Setting<AutoCart.ReFillMode> reFill = new Setting<>("ReFill", AutoCart.ReFillMode.None);
   private final Setting<SettingGroup> cartAuraTargets = new Setting<>(
      "Target", new SettingGroup(false, 0), v -> this.mode.is(AutoCart.Mode.CrossBow) && this.cartAura.getValue()
   );
   private final Setting<Boolean> cartAuraTarget = new Setting<>("Aura Target", true, v -> this.mode.is(AutoCart.Mode.CrossBow) && this.cartAura.getValue())
      .addToGroup(this.cartAuraTargets);
   private final Setting<Boolean> cartOtherPlayer = new Setting<>("Other Player", false, v -> this.mode.is(AutoCart.Mode.CrossBow) && this.cartAura.getValue())
      .addToGroup(this.cartAuraTargets);
   private volatile float[] silentRotation = null;
   private volatile boolean rotating = false;
   private boolean crossBowPressed = false;
   private volatile boolean cartAuraExecuting = false;
   private boolean scheduledRefillSwap = false;
   private int scheduledRefillInventorySlot = -1;
   private int scheduledRefillHotbarSlot = -1;
   private long scheduledRefillAt = -1L;
   private UUID lastAuraTargetUuid = null;
   private int lastAuraTargetId = -1;
   private long lastAuraTargetAt = -1L;
   private static final String REFILL_BLOCK_OWNER = "autocart_refill";
   private static final long LEGIT_REFILL_DELAY_MS = 5L;

   public AutoCart() {
      super("AutoCart", "Automates TNT minecart combat setups.", Module.Category.COMBAT);
   }

   private boolean isCartAuraEnabled() {
      return this.mode.is(AutoCart.Mode.CrossBow) && this.cartAura != null && this.cartAura.getValue();
   }

   private boolean isRefillSlotVisible() {
      return this.reFill != null && this.reFill.getValue() != AutoCart.ReFillMode.None;
   }

   private void runOnClientThreadAndWait(Runnable action) {
      if (mc.isSameThread()) {
         action.run();
      } else {
         CountDownLatch latch = new CountDownLatch(1);
         AtomicReference<RuntimeException> errorRef = new AtomicReference<>();
         mc.execute(() -> {
            try {
               action.run();
            } catch (RuntimeException error) {
               errorRef.set(error);
            } finally {
               latch.countDown();
            }
         });

         try {
            latch.await();
         } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
         }

         RuntimeException error = errorRef.get();
         if (error != null) {
            throw error;
         }
      }
   }

   private <T> T callOnClientThreadAndWait(Supplier<T> supplier) {
      AtomicReference<T> resultRef = new AtomicReference<>();
      this.runOnClientThreadAndWait(() -> resultRef.set(supplier.get()));
      return resultRef.get();
   }

   @Override
   public void onEnable() {
      this.resetState();
   }

   @Override
   public void onDisable() {
      this.resetState();
   }

   private void resetState() {
      this.silentRotation = null;
      this.rotating = false;
      this.crossBowPressed = false;
      if (this.cartAuraExecuting) {
         ModuleManager.aura.externalPause = false;
      }

      this.cartAuraExecuting = false;
      this.lastAuraTargetUuid = null;
      this.lastAuraTargetId = -1;
      this.lastAuraTargetAt = -1L;
      this.clearRefillState();
   }

   @EventHandler
   public void onSync(EventSync e) {
      if (!fullNullCheck()) {
         if (this.rotating && this.silentRotation != null) {
            mc.player.setYRot(this.silentRotation[0]);
            mc.player.setXRot(this.silentRotation[1]);
         }
      }
   }

   @EventHandler(priority = -200)
   public void onPlayerMove(EventFixVelocity event) {
      float[] rotation = this.getAutoCartMoveFixRotation();
      if (rotation != null) {
         event.setVelocity(this.fixMovement(rotation[0], event.getMovementInput(), event.getSpeed()));
      }
   }

   @EventHandler(priority = -200)
   public void onKeyboardInput(EventKeyboardInput event) {
      float[] rotation = this.getAutoCartMoveFixRotation();
      if (rotation != null) {
         float moveForward = vcore.utility.player.InputUtility.getForward(mc.player.input);
         float moveSideways = vcore.utility.player.InputUtility.getLeft(mc.player.input);
         float delta = (mc.player.getYRot() - rotation[0]) * (float) (Math.PI / 180.0);
         float cos = Mth.cos(delta);
         float sin = Mth.sin(delta);
         vcore.utility.player.InputUtility.setLeft(mc.player.input, Math.round(moveSideways * cos - moveForward * sin));
         vcore.utility.player.InputUtility.setForward(mc.player.input, Math.round(moveForward * cos + moveSideways * sin));
      }
   }

   @EventHandler
   public void onPacketSendPost(PacketEvent.@NotNull SendPost event) {
      if (!fullNullCheck() && this.mode.is(AutoCart.Mode.Bow)) {
         if (event.getPacket() instanceof ServerboundPlayerActionPacket action
            && action.getAction() == Action.RELEASE_USE_ITEM
            && mc.player.getMainHandItem().getItem() == Items.BOW) {
            this.executeBowMode();
         }
      }
   }

   @EventHandler(priority = 200)
   public void onPacketReceive(PacketEvent.@NotNull Receive event) {
      if (!fullNullCheck() && this.mode.is(AutoCart.Mode.CrossBow) && this.cartAura.getValue()) {
         if (event.getPacket() instanceof ClientboundEntityEventPacket packet && packet.getEventId() == 35 && packet.getEntity(mc.level) instanceof Player player
            )
          {
            this.handleCartAuraPop(player);
         }
      }
   }

   @Override
   public void onUpdate() {
      if (!fullNullCheck()) {
         this.handleScheduledRefill();
         this.handleRefill();
         if (this.mode.is(AutoCart.Mode.CrossBow)) {
            this.updateCartAuraTargetMemory();
            boolean pressed = this.isKeyPressed(this.activeBind);
            if (pressed && !this.crossBowPressed) {
               this.crossBowPressed = true;
               this.executeCrossBowMode();
            }

            if (!pressed) {
               this.crossBowPressed = false;
            }
         }
      }
   }

   @Override
   public void onRender2D(GuiGraphics context) {
      if (!fullNullCheck() && this.mode.is(AutoCart.Mode.CrossBow) && !mc.options.hideGui) {
         if (this.hasCrossbowDebugBaseRequirements()) {
            this.renderCrossbowDebug(context);
         }
      }
   }

   private void handleRefill() {
      if (this.reFill.is(AutoCart.ReFillMode.None)) {
         this.clearRefillState();
      } else if (!this.scheduledRefillSwap && mc.screen == null && mc.player.containerMenu == mc.player.inventoryMenu) {
         int targetHotbarSlot = this.refillSlot.getValue() - 1;
         if (mc.player.getInventory().getItem(targetHotbarSlot).getItem() != Items.TNT_MINECART) {
            SearchInvResult cartResult = InventoryUtility.findItemInInventory(Items.TNT_MINECART);
            if (cartResult.found()) {
               if (this.reFill.is(AutoCart.ReFillMode.Legit)) {
                  this.handleLegitRefill(cartResult.slot(), targetHotbarSlot);
               } else {
                  this.performRefillSwap(cartResult.slot(), targetHotbarSlot);
               }
            }
         }
      }
   }

   private void handleLegitRefill(int inventorySlot, int hotbarSlot) {
      if (MovementUtility.isMoving()) {
         InputBlocker.block("autocart_refill");
         this.scheduleRefillSwap(inventorySlot, hotbarSlot, 5L);
      } else {
         this.performRefillSwap(inventorySlot, hotbarSlot);
      }
   }

   private void scheduleRefillSwap(int inventorySlot, int hotbarSlot, long delayMs) {
      if (inventorySlot == -1) {
         this.clearRefillState();
      } else {
         this.scheduledRefillSwap = true;
         this.scheduledRefillInventorySlot = inventorySlot;
         this.scheduledRefillHotbarSlot = hotbarSlot;
         this.scheduledRefillAt = System.currentTimeMillis() + delayMs;
      }
   }

   private void handleScheduledRefill() {
      if (this.scheduledRefillSwap && System.currentTimeMillis() >= this.scheduledRefillAt) {
         try {
            this.performRefillSwap(this.scheduledRefillInventorySlot, this.scheduledRefillHotbarSlot);
         } finally {
            this.clearRefillState();
         }
      }
   }

   private void performRefillSwap(int inventorySlot, int hotbarSlot) {
      if (inventorySlot != -1 && hotbarSlot >= 0 && hotbarSlot <= 8) {
         if (mc.screen == null && mc.player.containerMenu == mc.player.inventoryMenu) {
            if (mc.player.getInventory().getItem(hotbarSlot).getItem() != Items.TNT_MINECART) {
               clickSlot(inventorySlot, hotbarSlot, ClickType.SWAP);
            }
         }
      }
   }

   private void clearRefillState() {
      this.scheduledRefillSwap = false;
      this.scheduledRefillInventorySlot = -1;
      this.scheduledRefillHotbarSlot = -1;
      this.scheduledRefillAt = -1L;
      InputBlocker.unblock("autocart_refill");
   }

   @EventHandler
   public void onTotemPop(@NotNull TotemPopEvent event) {
      this.handleCartAuraPop(event.getEntity());
   }

   private void handleCartAuraPop(Player popTarget) {
      if (!fullNullCheck()) {
         if (this.mode.is(AutoCart.Mode.CrossBow) && this.cartAura.getValue() && !this.cartAuraExecuting) {
            if (popTarget != null && popTarget != mc.player) {
               this.updateCartAuraTargetMemory();
               boolean isAuraTarget = this.cartAuraTarget.getValue() && this.isRememberedAuraTarget(popTarget);
               boolean isOtherPlayer = false;
               if (this.cartOtherPlayer.getValue()) {
                  isOtherPlayer = !Managers.FRIEND.isFriend(popTarget) && mc.player.position().distanceTo(popTarget.position()) <= 6.0;
               }

               if (isAuraTarget || isOtherPlayer) {
                  mc.execute(() -> {
                     if (!fullNullCheck() && !this.cartAuraExecuting && !this.isDisabled()) {
                        if (this.mode.is(AutoCart.Mode.CrossBow) && this.cartAura.getValue()) {
                           if (!popTarget.isRemoved()) {
                              if (!this.totemCheck.getValue() || !this.isHoldingTotem(popTarget)) {
                                 if (this.hasBasicCartAuraResources()) {
                                    this.executeCartAura(popTarget);
                                 }
                              }
                           }
                        }
                     }
                  });
               }
            }
         }
      }
   }

   private void executeCartAura(Player target) {
      this.cartAuraExecuting = true;
      int delayMs = this.delay.getValue();
      int startDelayMs = this.cartAuraDelay.getValue() * 50;
      Managers.ASYNC.run(() -> {
         try {
            if (startDelayMs > 0) {
               AsyncManager.sleep(startDelayMs);
            }

            if (fullNullCheck() || this.isDisabled() || !this.mode.is(AutoCart.Mode.CrossBow) || !this.cartAura.getValue()) {
               return;
            }

            int[] prevSlotRef = new int[]{-1};
            AutoCart.CartAuraPlan plan = this.callOnClientThreadAndWait(() -> {
               if (this.totemCheck.getValue() && this.isHoldingTotem(target)) {
                  return null;
               }

               AutoCart.CartAuraPlan createdPlan = this.createCartAuraPlan(target);
               if (mc.player != null) {
                  prevSlotRef[0] = mc.player.getInventory().getSelectedSlot();
               }

               return createdPlan;
            });
            if (plan != null) {
               BlockPos basePos = plan.basePos();
               int prevSlot = prevSlotRef[0];
               ModuleManager.aura.externalPause = true;
               Vec3 placeVec = new Vec3(basePos.getX() + 0.5, basePos.above().getY(), basePos.getZ() + 0.5);
               this.runOnClientThreadAndWait(() -> this.applyRotation(InteractionUtility.calculateAngle(placeVec)));
               AsyncManager.sleep(delayMs);
               if (!plan.railExists()) {
                  this.runOnClientThreadAndWait(() -> {
                     if (!this.isRailBlock(mc.level.getBlockState(basePos.above()).getBlock())) {
                        this.selectHotbarLegit(plan.railResult().slot());
                        this.placeRailOn(basePos);
                     }
                  });
                  AsyncManager.sleep(delayMs);
               }

               this.runOnClientThreadAndWait(() -> {
                  this.selectHotbarLegit(plan.cartResult().slot());
                  this.placeMinecartOn(basePos);
               });
               AsyncManager.sleep(delayMs);
               if (!plan.hasFlame()) {
                  BlockPos firePos = this.findFirePosition(basePos);
                  if (firePos != null) {
                     this.runOnClientThreadAndWait(() -> {
                        Vec3 fireVec = new Vec3(firePos.getX() + 0.5, firePos.getY() + 1.0, firePos.getZ() + 0.5);
                        this.applyRotation(InteractionUtility.calculateAngle(fireVec));
                        this.selectHotbarLegit(plan.flintResult().slot());
                        BlockHitResult fireHit = new BlockHitResult(fireVec, Direction.UP, firePos, false);
                        this.interactWithBlock(fireHit);
                     });
                     AsyncManager.sleep(delayMs);
                  }
               }

               this.runOnClientThreadAndWait(() -> {
                  Vec3 minecartCenter = new Vec3(basePos.getX() + 0.5, basePos.getY() + 1.5, basePos.getZ() + 0.5);
                  float[] shootAngle = InteractionUtility.calculateAngle(minecartCenter);
                  this.applyRotation(shootAngle);
                  this.selectHotbarLegit(plan.crossbowResult().slot());
                  this.interactWithItem();
                  mc.player.swing(InteractionHand.MAIN_HAND);
               });
               AsyncManager.sleep(delayMs);
               this.runOnClientThreadAndWait(() -> {
                  if (this.swapBack.getValue()) {
                     this.selectHotbarLegit(prevSlot);
                  }

                  this.endRotation();
               });
               return;
            }
         } finally {
            ModuleManager.aura.externalPause = false;
            this.cartAuraExecuting = false;
         }
      });
   }

   private void updateCartAuraTargetMemory() {
      if (this.isCartAuraEnabled() && this.cartAuraTarget.getValue()) {
         if (Aura.target instanceof Player player && player != mc.player && !player.isRemoved()) {
            this.lastAuraTargetUuid = player.getUUID();
            this.lastAuraTargetId = player.getId();
            this.lastAuraTargetAt = System.currentTimeMillis();
         }
      }
   }

   private boolean isRememberedAuraTarget(Player player) {
      if (player == null) {
         return false;
      } else if (this.isSameAuraTarget(Aura.target, player)) {
         return true;
      } else if (ModuleManager.aura.isOn() && this.isSameAuraTarget(ModuleManager.aura.findTarget(), player)) {
         return true;
      } else {
         return this.lastAuraTargetUuid != null && System.currentTimeMillis() - this.lastAuraTargetAt <= 1000L
            ? player.getUUID().equals(this.lastAuraTargetUuid) || player.getId() == this.lastAuraTargetId
            : false;
      }
   }

   private boolean isSameAuraTarget(Entity auraTarget, Player player) {
      return !(auraTarget instanceof Player auraPlayer)
         ? false
         : auraPlayer == player || auraPlayer.getId() == player.getId() || auraPlayer.getUUID().equals(player.getUUID());
   }

   private void executeBowMode() {
      if (!fullNullCheck()) {
         SearchInvResult bowResult = InventoryUtility.findItemInHotBar(Items.BOW);
         SearchInvResult cartResult = InventoryUtility.findItemInHotBar(Items.TNT_MINECART);
         if (bowResult.found() && cartResult.found()) {
            BlockPos targetPos = this.calcBowTrajectory(mc.player.getYRot());
            if (targetPos != null) {
               BlockPos basePos = this.getCartBasePos(targetPos);
               boolean railExists = this.isRailBlock(mc.level.getBlockState(basePos.above()).getBlock());
               if (railExists || this.findRailInHotBar().found()) {
                  float distSq = PlayerUtility.squaredDistanceFromEyes(Vec3.atCenterOf(basePos.above()));
                  float maxDistSq = this.maxDistance.getValue() * this.maxDistance.getValue();
                  float safeDistSq = 4.0F;
                  if (!(distSq > maxDistSq) && !(distSq < safeDistSq)) {
                     Managers.ASYNC.run(() -> this.executeBowPlacement(targetPos), this.startDelay.getValue().intValue());
                  }
               }
            }
         }
      }
   }

   private void executeBowPlacement(@NotNull BlockPos targetPos) {
      if (!fullNullCheck() && this.mode.is(AutoCart.Mode.Bow)) {
         BlockPos basePos = this.getCartBasePos(targetPos);
         boolean railExists = this.isRailBlock(mc.level.getBlockState(basePos.above()).getBlock());
         SearchInvResult railResult = this.findRailInHotBar();
         SearchInvResult cartResult = InventoryUtility.findItemInHotBar(Items.TNT_MINECART);
         if (cartResult.found()) {
            if (railExists || railResult.found()) {
               int prevSlot = mc.player.getInventory().getSelectedSlot();
               int delayMs = this.delay.getValue();
               Vec3 placeVec = new Vec3(basePos.getX() + 0.5, basePos.above().getY(), basePos.getZ() + 0.5);
               mc.executeIfPossible(() -> this.applyRotation(InteractionUtility.calculateAngle(placeVec)));
               AsyncManager.sleep(delayMs);
               if (!railExists) {
                  mc.executeIfPossible(() -> {
                     if (!this.isRailBlock(mc.level.getBlockState(basePos.above()).getBlock())) {
                        this.selectHotbarLegit(railResult.slot());
                        this.placeRailOn(basePos);
                     }
                  });
                  AsyncManager.sleep(delayMs);
               }

               mc.executeIfPossible(() -> {
                  this.selectHotbarLegit(cartResult.slot());
                  this.placeMinecartOn(basePos);
               });
               AsyncManager.sleep(delayMs);
               mc.executeIfPossible(() -> {
                  if (this.swapBack.getValue()) {
                     this.selectHotbarLegit(prevSlot);
                  }

                  this.endRotation();
               });
            }
         }
      }
   }

   @NotNull
   private BlockPos getCartBasePos(@NotNull BlockPos targetPos) {
      BlockState targetState = mc.level.getBlockState(targetPos);
      return !this.isRailBlock(targetState.getBlock()) && !targetState.canBeReplaced() ? targetPos : targetPos.below();
   }

   private boolean hasBasicCartAuraResources() {
      SearchInvResult crossbowResult = this.findLoadedCrossbowInHotBar();
      SearchInvResult cartResult = InventoryUtility.findItemInHotBar(Items.TNT_MINECART);
      if (crossbowResult.found() && cartResult.found()) {
         boolean hasFlame = this.hasFlameEnchant(mc.player.getInventory().getItem(crossbowResult.slot()));
         return hasFlame || InventoryUtility.findItemInHotBar(Items.FLINT_AND_STEEL).found();
      } else {
         return false;
      }
   }

   @Nullable
   private AutoCart.CartAuraPlan createCartAuraPlan(Player target) {
      if (!this.isValidCartAuraTarget(target)) {
         return null;
      }

      BlockPos basePos = this.findCartAuraPosition(target);
      if (basePos == null) {
         return null;
      }

      SearchInvResult crossbowResult = this.findLoadedCrossbowInHotBar();
      SearchInvResult cartResult = InventoryUtility.findItemInHotBar(Items.TNT_MINECART);
      if (crossbowResult.found() && cartResult.found()) {
         boolean hasFlame = this.hasFlameEnchant(mc.player.getInventory().getItem(crossbowResult.slot()));
         SearchInvResult flintResult = InventoryUtility.findItemInHotBar(Items.FLINT_AND_STEEL);
         if (!hasFlame && !flintResult.found()) {
            return null;
         }

         boolean railExists = this.isRailBlock(mc.level.getBlockState(basePos.above()).getBlock());
         SearchInvResult railResult = this.findRailInHotBar();
         return !railExists && !railResult.found()
            ? null
            : new AutoCart.CartAuraPlan(basePos, crossbowResult, railResult, cartResult, flintResult, hasFlame, railExists);
      } else {
         return null;
      }
   }

   private boolean isValidCartAuraTarget(Player target) {
      return target != null && target != mc.player && !target.isRemoved();
   }

   private boolean isHoldingTotem(Player player) {
      return player.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING || player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING;
   }

   private void executeCrossBowMode() {
      if (!fullNullCheck()) {
         SearchInvResult crossbowResult = this.findLoadedCrossbowInHotBar();
         SearchInvResult railResult = this.findRailInHotBar();
         SearchInvResult cartResult = InventoryUtility.findItemInHotBar(Items.TNT_MINECART);
         if (crossbowResult.found() && railResult.found() && cartResult.found()) {
            boolean hasFlame = this.hasFlameEnchant(mc.player.getInventory().getItem(crossbowResult.slot()));
            SearchInvResult flintResult = InventoryUtility.findItemInHotBar(Items.FLINT_AND_STEEL);
            if (hasFlame || flintResult.found()) {
               BlockHitResult rayResult = this.rayTraceFromEyes(4.5);
               if (rayResult != null && rayResult.getType() == Type.BLOCK) {
                  BlockPos hitPos = rayResult.getBlockPos();
                  if (!(PlayerUtility.squaredDistanceFromEyes(Vec3.atCenterOf(hitPos)) > 20.25F)) {
                     BlockPos basePos;
                     if (mc.level.getBlockState(hitPos).canBeReplaced()) {
                        basePos = hitPos.below();
                     } else {
                        basePos = hitPos;
                     }

                     BlockPos fireBlockPos = null;
                     if (!hasFlame) {
                        fireBlockPos = this.findFirePosition(basePos);
                        if (fireBlockPos == null) {
                           return;
                        }
                     }

                     BlockPos finalFireBlockPos = fireBlockPos;
                     int prevSlot = mc.player.getInventory().getSelectedSlot();
                     int delayMs = this.delay.getValue();
                     Managers.ASYNC.run(() -> {
                        Vec3 placeVec = new Vec3(basePos.getX() + 0.5, basePos.above().getY(), basePos.getZ() + 0.5);
                        mc.executeIfPossible(() -> this.applyRotation(InteractionUtility.calculateAngle(placeVec)));
                        AsyncManager.sleep(delayMs);
                        mc.executeIfPossible(() -> {
                           if (!this.isRailBlock(mc.level.getBlockState(basePos.above()).getBlock())) {
                              this.selectHotbarLegit(railResult.slot());
                              this.placeRailOn(basePos);
                           }
                        });
                        AsyncManager.sleep(delayMs);
                        mc.executeIfPossible(() -> {
                           this.selectHotbarLegit(cartResult.slot());
                           this.placeMinecartOn(basePos);
                        });
                        AsyncManager.sleep(delayMs);
                        if (!hasFlame && finalFireBlockPos != null) {
                           mc.executeIfPossible(() -> {
                              Vec3 fireVec = new Vec3(finalFireBlockPos.getX() + 0.5, finalFireBlockPos.getY() + 1.0, finalFireBlockPos.getZ() + 0.5);
                              this.applyRotation(InteractionUtility.calculateAngle(fireVec));
                              this.selectHotbarLegit(flintResult.slot());
                              BlockHitResult fireHit = new BlockHitResult(fireVec, Direction.UP, finalFireBlockPos, false);
                              this.interactWithBlock(fireHit);
                           });
                           AsyncManager.sleep(delayMs);
                        }

                        mc.executeIfPossible(() -> {
                           Vec3 minecartCenter = new Vec3(basePos.getX() + 0.5, basePos.getY() + 1.5, basePos.getZ() + 0.5);
                           float[] shootAngle = InteractionUtility.calculateAngle(minecartCenter);
                           this.applyRotation(shootAngle);
                           this.selectHotbarLegit(crossbowResult.slot());
                           this.interactWithItem();
                           mc.player.swing(InteractionHand.MAIN_HAND);
                        });
                        AsyncManager.sleep(delayMs);
                        mc.executeIfPossible(() -> {
                           if (this.swapBack.getValue()) {
                              this.selectHotbarLegit(prevSlot);
                           }

                           this.endRotation();
                        });
                     });
                  }
               }
            }
         }
      }
   }

   private void applyRotation(float[] angle) {
      if (this.changeLook.getValue()) {
         mc.player.setYRot(angle[0]);
         mc.player.setXRot(angle[1]);
      } else {
         this.silentRotation = angle;
         this.rotating = true;
      }
   }

   private void endRotation() {
      this.silentRotation = null;
      this.rotating = false;
   }

   public boolean isAutoCartMoveFixActive() {
      return this.getAutoCartMoveFixRotation() != null;
   }

   @Nullable
   private float[] getAutoCartMoveFixRotation() {
      float[] rotation = this.silentRotation;
      return this.isOn() && !fullNullCheck() && !this.changeLook.getValue() && this.rotating && rotation != null && !mc.player.isHandsBusy() ? rotation : null;
   }

   private Vec3 fixMovement(float yaw, Vec3 movementInput, float speed) {
      double lengthSquared = movementInput.lengthSqr();
      if (lengthSquared < 1.0E-7) {
         return Vec3.ZERO;
      }

      Vec3 movement = (lengthSquared > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
      float sin = Mth.sin(yaw * (float) (Math.PI / 180.0));
      float cos = Mth.cos(yaw * (float) (Math.PI / 180.0));
      return new Vec3(movement.x * cos - movement.z * sin, movement.y, movement.z * cos + movement.x * sin);
   }

   private void selectHotbarLegit(int slot) {
      if (mc.player != null && mc.gameMode != null && slot >= 0 && slot <= 8) {
         mc.player.getInventory().setSelectedSlot(slot);
         ((IInteractionManager)mc.gameMode).syncSlot();
      }
   }

   private void runWithInteractionRotation(@Nullable float[] angle, Runnable action) {
      if (mc.player != null) {
         if (!this.changeLook.getValue() && angle != null) {
            float prevYaw = mc.player.getYRot();
            float prevPitch = mc.player.getXRot();
            mc.player.setYRot(angle[0]);
            mc.player.setXRot(angle[1]);

            try {
               action.run();
            } finally {
               mc.player.setYRot(prevYaw);
               mc.player.setXRot(prevPitch);
            }
         } else {
            action.run();
         }
      }
   }

   private void interactWithItem() {
      if (mc.player != null && mc.gameMode != null) {
         this.runWithInteractionRotation(this.silentRotation, () -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
      }
   }

   private void placeRailOn(BlockPos basePos) {
      if (mc.level != null && mc.player != null && mc.gameMode != null) {
         BlockHitResult hitResult = new BlockHitResult(new Vec3(basePos.getX() + 0.5, basePos.above().getY(), basePos.getZ() + 0.5), Direction.UP, basePos, false);
         this.interactWithBlock(hitResult);
      }
   }

   private void placeMinecartOn(BlockPos basePos) {
      if (mc.level != null && mc.player != null && mc.gameMode != null) {
         BlockHitResult hitResult = new BlockHitResult(
            new Vec3(basePos.getX() + 0.5, basePos.above().getY() + 0.125, basePos.getZ() + 0.5), Direction.UP, basePos.above(), false
         );
         this.interactWithBlock(hitResult);
      }
   }

   private void interactWithBlock(BlockHitResult hitResult) {
      this.runWithInteractionRotation(this.silentRotation, () -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult));
      mc.player.swing(InteractionHand.MAIN_HAND);
   }

   @Nullable
   private BlockPos calcBowTrajectory(float yaw) {
      if (mc.player != null && mc.level != null) {
         double x = Render2DEngine.interpolate(mc.player.xo, mc.player.getX(), Render3DEngine.getTickDelta());
         double y = Render2DEngine.interpolate(mc.player.yo, mc.player.getY(), Render3DEngine.getTickDelta());
         double z = Render2DEngine.interpolate(mc.player.zo, mc.player.getZ(), Render3DEngine.getTickDelta());
         y += mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
         float pitch = mc.player.getXRot();
         double motionX = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
         double motionY = -Mth.sin(pitch / 180.0F * (float) Math.PI);
         double motionZ = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
         float power = mc.player.getTicksUsingItem() / 20.0F;
         power = (power * power + power * 2.0F) / 3.0F;
         if (power > 1.0F) {
            power = 1.0F;
         }

         if (power < 0.1F) {
            return null;
         }

         float dist = Mth.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
         motionX /= dist;
         motionY /= dist;
         motionZ /= dist;
         float pow = power * 3.0F;
         motionX *= pow;
         motionY *= pow;
         motionZ *= pow;
         if (!mc.player.onGround()) {
            motionY += mc.player.getDeltaMovement().y();
         }

         for (int i = 0; i < 300; i++) {
            Vec3 lastPos = new Vec3(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;
            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;
            motionY -= 0.05F;

            for (Entity ent : mc.level.entitiesForRendering()) {
               if (!(ent instanceof Arrow)
                  && !ent.equals(mc.player)
                  && ent.getBoundingBox().intersects(new AABB(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.3))) {
                  return null;
               }
            }

            Vec3 pos = new Vec3(x, y, z);
            BlockHitResult bhr = mc.level.clip(new ClipContext(lastPos, pos, Block.OUTLINE, Fluid.NONE, mc.player));
            if (bhr != null && bhr.getType() == Type.BLOCK) {
               return bhr.getBlockPos();
            }

            if (y <= -65.0) {
               break;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   @Nullable
   private BlockPos findCartAuraPosition(Entity target) {
      if (mc.player != null && mc.level != null) {
         Vec3 playerEyes = InteractionUtility.getEyesPos(mc.player);
         AABB targetBox = target.getBoundingBox();
         Vec3 targetFeet = new Vec3(target.getX(), targetBox.minY, target.getZ());
         BlockPos targetBlockPos = target.blockPosition();
         int r = 4;
         BlockPos bestPos = null;
         double bestDist = Double.MAX_VALUE;

         for (int x = targetBlockPos.getX() - r; x <= targetBlockPos.getX() + r; x++) {
            for (int z = targetBlockPos.getZ() - r; z <= targetBlockPos.getZ() + r; z++) {
               for (int y = targetBlockPos.getY() - r; y <= targetBlockPos.getY(); y++) {
                  BlockPos bp = new BlockPos(x, y, z);
                  if (mc.level.getBlockState(bp).isSolid() && !(bp.getY() + 1 > targetBox.minY)) {
                     BlockState aboveState = mc.level.getBlockState(bp.above());
                     if (aboveState.canBeReplaced() || this.isRailBlock(aboveState.getBlock())) {
                        Vec3 surfacePos = new Vec3(bp.getX() + 0.5, bp.getY() + 1.0, bp.getZ() + 0.5);
                        if (!(PlayerUtility.squaredDistanceFromEyes(surfacePos) > 20.25F)) {
                           BlockHitResult blockCheck = mc.level
                              .clip(new ClipContext(playerEyes, surfacePos, Block.COLLIDER, Fluid.NONE, mc.player));
                           if ((blockCheck == null || blockCheck.getType() != Type.BLOCK || blockCheck.getBlockPos().equals(bp))
                              && this.isPathClearOfPlayers(playerEyes, surfacePos, target)) {
                              Vec3 cartCenter = new Vec3(bp.getX() + 0.5, bp.getY() + 1.5, bp.getZ() + 0.5);
                              BlockHitResult damageCheck = mc.level
                                 .clip(new ClipContext(cartCenter, targetFeet, Block.COLLIDER, Fluid.NONE, mc.player));
                              if (damageCheck == null || damageCheck.getType() != Type.BLOCK) {
                                 double distToTarget = cartCenter.distanceToSqr(targetFeet);
                                 if (distToTarget < bestDist) {
                                    bestDist = distToTarget;
                                    bestPos = bp;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return bestPos;
      } else {
         return null;
      }
   }

   private boolean isPathClearOfPlayers(Vec3 from, Vec3 to, Entity exclude) {
      if (mc.level == null) {
         return true;
      }

      for (Player player : mc.level.players()) {
         if (player != mc.player && player != exclude && player.getBoundingBox().clip(from, to).isPresent()) {
            return false;
         }
      }

      return true;
   }

   private SearchInvResult findRailInHotBar() {
      return InventoryUtility.findItemInHotBar(Items.RAIL, Items.ACTIVATOR_RAIL, Items.DETECTOR_RAIL, Items.POWERED_RAIL);
   }

   private void renderCrossbowDebug(GuiGraphics context) {
      int hotbarLeft = mc.getWindow().getGuiScaledWidth() / 2 - 91;
      int hotbarTop = mc.getWindow().getGuiScaledHeight() - 22;

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getItem(slot);
         if (this.isCrossbowDebugCandidate(stack)) {
            float slotX = hotbarLeft + slot * 20 + 1;
            float slotY = hotbarTop + 1;
            Render2DEngine.drawRect(Render2DEngine.toPoseStack(context.pose()), slotX, slotY, 20.0F, 20.0F, CROSSBOW_DEBUG_FILL);
         }
      }
   }

   private boolean hasCrossbowDebugBaseRequirements() {
      if (this.findRailInHotBar().found() && InventoryUtility.findItemInHotBar(Items.TNT_MINECART).found()) {
         if (!this.hasArrowAmmoInInventory()) {
            return false;
         }

         for (int slot = 0; slot < 9; slot++) {
            if (this.isCrossbowDebugCandidate(mc.player.getInventory().getItem(slot))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isCrossbowDebugCandidate(ItemStack stack) {
      return !this.isUnloadedCrossbow(stack) ? false : this.hasFlameEnchant(stack) || InventoryUtility.findItemInHotBar(Items.FLINT_AND_STEEL).found();
   }

   private boolean hasArrowAmmoInInventory() {
      for (ItemStack stack : mc.player.getInventory().getNonEquipmentItems()) {
         if (this.isArrowStack(stack)) {
            return true;
         }
      }

      ItemStack offhand = mc.player.getOffhandItem();
      if (this.isArrowStack(offhand)) {
         return true;
      }

      return false;
   }

   private boolean isArrowStack(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof ArrowItem;
   }

   private boolean isUnloadedCrossbow(ItemStack stack) {
      return stack.getItem() == Items.CROSSBOW && !this.isCrossbowCharged(stack);
   }

   private boolean isCrossbowCharged(ItemStack stack) {
      return stack.getItem() == Items.CROSSBOW
         && stack.get(DataComponents.CHARGED_PROJECTILES) != null
         && !((ChargedProjectiles)stack.get(DataComponents.CHARGED_PROJECTILES)).isEmpty();
   }

   private SearchInvResult findLoadedCrossbowInHotBar() {
      return InventoryUtility.findInHotBar(this::isCrossbowCharged);
   }

   private boolean isRailBlock(net.minecraft.world.level.block.Block block) {
      return block == Blocks.RAIL || block == Blocks.POWERED_RAIL || block == Blocks.DETECTOR_RAIL || block == Blocks.ACTIVATOR_RAIL;
   }

   private boolean hasFlameEnchant(ItemStack stack) {
      return mc.level != null
         && EnchantmentHelper.getItemEnchantmentLevel(
               mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FLAME), stack
            ) > 0;
   }

   @Nullable
   private BlockHitResult rayTraceFromEyes(double maxRange) {
      if (mc.player != null && mc.level != null) {
         Vec3 eyePos = mc.player.getEyePosition();
         Vec3 lookVec = mc.player.getViewVector(1.0F);
         Vec3 endPos = eyePos.add(lookVec.scale(maxRange));
         return mc.level.clip(new ClipContext(eyePos, endPos, Block.OUTLINE, Fluid.NONE, mc.player));
      } else {
         return null;
      }
   }

   @Nullable
   private BlockPos findFirePosition(BlockPos targetPos) {
      if (mc.player != null && mc.level != null) {
         Vec3 eyePos = mc.player.getEyePosition();
         Vec3 minecartPos = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5);
         Vec3 dir = minecartPos.subtract(eyePos).normalize();
         Direction[] horizontals = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
         BlockPos bestPos = null;
         double bestScore = Double.MAX_VALUE;

         for (Direction d : horizontals) {
            BlockPos candidate = targetPos.relative(d);
            if (mc.level.getBlockState(candidate).isSolid() && mc.level.isEmptyBlock(candidate.above())) {
               Vec3 fireCenter = new Vec3(candidate.getX() + 0.5, candidate.getY() + 1.0, candidate.getZ() + 0.5);
               Vec3 eyeToFire = fireCenter.subtract(eyePos);
               double t = eyeToFire.dot(dir);
               if (!(t <= 0.0)) {
                  double eyeToMinecart = minecartPos.subtract(eyePos).dot(dir);
                  if (!(t >= eyeToMinecart)) {
                     Vec3 closest = eyePos.add(dir.scale(t));
                     double distToLine = closest.distanceTo(fireCenter);
                     if (distToLine < bestScore) {
                        bestScore = distToLine;
                        bestPos = candidate;
                     }
                  }
               }
            }
         }

         if (bestPos == null) {
            for (Direction d : horizontals) {
               BlockPos adj = targetPos.relative(d);
               if (mc.level.getBlockState(adj).isSolid() && mc.level.isEmptyBlock(adj.above())) {
                  return adj;
               }
            }
         }

         return bestPos;
      } else {
         return null;
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.getValue().name();
   }

   private record CartAuraPlan(
      BlockPos basePos,
      SearchInvResult crossbowResult,
      SearchInvResult railResult,
      SearchInvResult cartResult,
      SearchInvResult flintResult,
      boolean hasFlame,
      boolean railExists
   ) {
   }

   public enum Mode {
      Bow,
      CrossBow;
   }

   public enum ReFillMode {
      None,
      Normal,
      Legit;
   }
}
