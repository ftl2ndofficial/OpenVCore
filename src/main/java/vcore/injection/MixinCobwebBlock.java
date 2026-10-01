package vcore.injection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.IManager;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.movement.AntiWeb;
import vcore.utility.player.InteractionUtility;

@Mixin(WebBlock.class)
public class MixinCobwebBlock {
   @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
   public void onEntityCollisionHook(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise, CallbackInfo ci) {
      if (ModuleManager.antiWeb.isEnabled() && entity == IManager.mc.player) {
         if (AntiWeb.mode.getValue() == AntiWeb.Mode.Ignore) {
            ci.cancel();
            if (AntiWeb.grim.getValue()) {
               InteractionUtility.sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.UP, id));
            }
         }
      }
   }
}
