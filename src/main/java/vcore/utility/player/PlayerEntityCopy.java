package vcore.utility.player;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.player.RemotePlayer;
import vcore.features.modules.Module;

public class PlayerEntityCopy extends RemotePlayer {
   public PlayerEntityCopy() {
      super(Objects.requireNonNull(Module.mc.level), Objects.requireNonNull(Module.mc.player).getGameProfile());
      this.restoreFrom(Module.mc.player);
      this.getPlayerInfo();
      this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (Byte)Module.mc.player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
      this.setUUID(UUID.randomUUID());
   }

   public void spawn() {
      if (Module.mc.level != null) {
         this.unsetRemoved();
         Module.mc.level.addEntity(this);
      }
   }

   public void deSpawn() {
      if (Module.mc.level != null) {
         Module.mc.level.removeEntity(this.getId(), RemovalReason.DISCARDED);
      }
   }
}
