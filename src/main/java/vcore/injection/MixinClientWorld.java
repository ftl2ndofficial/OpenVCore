package vcore.injection;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.Vcore;
import vcore.events.impl.EventEntityRemoved;
import vcore.events.impl.EventEntitySpawn;
import vcore.events.impl.EventEntitySpawnPost;
import vcore.features.modules.Module;

@Mixin(ClientLevel.class)
public class MixinClientWorld {
   @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
   public void addEntityHook(Entity entity, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventEntitySpawn ees = new EventEntitySpawn(entity);
         Vcore.EVENT_BUS.post(ees);
         if (ees.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "addEntity", at = @At("RETURN"), cancellable = true)
   public void addEntityHookPost(Entity entity, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventEntitySpawnPost ees = new EventEntitySpawnPost(entity);
         Vcore.EVENT_BUS.post(ees);
         if (ees.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "removeEntity", at = @At("HEAD"))
   public void removeEntityHook(int entityId, RemovalReason removalReason, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventEntityRemoved eer = new EventEntityRemoved(Module.mc.level.getEntity(entityId));
         Vcore.EVENT_BUS.post(eer);
      }
   }
}
