package vcore.injection;

import java.util.Comparator;
import java.util.List;
import net.minecraft.Optionull;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.features.modules.Module;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerListHud {
   private static final Comparator<PlayerInfo> ENTRY_ORDERING = Comparator.<PlayerInfo>comparingInt(
         entry -> entry.getGameMode() == GameType.SPECTATOR ? 1 : 0
      )
      .thenComparing(entry -> (String)Optionull.mapOrDefault(entry.getTeam(), PlayerTeam::getName, ""))
      .thenComparing(entry -> entry.getProfile().name(), String::compareToIgnoreCase);

   @Inject(method = "getPlayerInfos", at = @At("HEAD"), cancellable = true)
   private void collectPlayerEntriesHook(CallbackInfoReturnable<List<PlayerInfo>> cir) {
      if (!Vcore.isFuturePresent()) {
         cir.setReturnValue(Module.mc.player.connection.getListedOnlinePlayers().stream().sorted(ENTRY_ORDERING).limit(80L).toList());
      }
   }
}
