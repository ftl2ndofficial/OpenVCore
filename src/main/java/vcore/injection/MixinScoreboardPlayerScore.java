package vcore.injection;

import net.minecraft.world.scores.PlayerScoreEntry;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerScoreEntry.class)
public class MixinScoreboardPlayerScore {
}
