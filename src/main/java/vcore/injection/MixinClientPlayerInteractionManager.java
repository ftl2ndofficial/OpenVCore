package vcore.injection;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAttackBlock;
import vcore.events.impl.EventBreakBlock;
import vcore.events.impl.EventClickSlot;
import vcore.features.modules.Module;
import vcore.features.modules.player.NoInteract;

@Mixin(MultiPlayerGameMode.class)
public class MixinClientPlayerInteractionManager {
   @Shadow
   private int destroyDelay;

   @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
   private void interactBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
      Block bs = Module.mc.level.getBlockState(hitResult.getBlockPos()).getBlock();
      if (ModuleManager.noInteract.isEnabled()
         && (
            bs == Blocks.CHEST
               || bs == Blocks.TRAPPED_CHEST
               || bs == Blocks.FURNACE
               || bs == Blocks.ANVIL
               || bs == Blocks.CRAFTING_TABLE
               || bs == Blocks.HOPPER
               || bs == Blocks.JUKEBOX
               || bs == Blocks.NOTE_BLOCK
               || bs == Blocks.ENDER_CHEST
               || bs == Blocks.DISPENSER
               || bs == Blocks.DROPPER
               || bs instanceof ShulkerBoxBlock
               || bs instanceof FenceBlock
               || bs instanceof FenceGateBlock
               || bs instanceof TrapDoorBlock
         )
         && (ModuleManager.aura.isEnabled() || !NoInteract.onlyAura.getValue())) {
         cir.setReturnValue(InteractionResult.PASS);
      }
   }

   @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
   private void attackBlockHook(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      if (!Module.fullNullCheck()) {
         EventAttackBlock event = new EventAttackBlock(pos, direction);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "destroyBlock", at = @At("RETURN"), cancellable = true)
   public void breakBlockHook(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (!Module.fullNullCheck()) {
         EventBreakBlock event = new EventBreakBlock(pos);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
   public void clickSlotHook(int syncId, int slotId, int button, ClickType actionType, Player player, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventClickSlot event = new EventClickSlot(actionType, slotId, button, syncId);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }
}
