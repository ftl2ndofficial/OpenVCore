package vcore.injection;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventChunkData;
import vcore.features.modules.Module;
import vcore.features.modules.misc.UnHook;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {
   @Inject(method = "handleEntityEvent", at = @At("HEAD"))
   private void hookPopEffectTotemStatus(@NotNull ClientboundEntityEventPacket packet, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      PacketUtils.ensureRunningOnSameThread(packet, (ClientPacketListener)(Object)this, client.packetProcessor());
      if (client.level != null && packet.getEventId() == 35) {
         Entity entity = packet.getEntity(client.level);
         if (entity != null) {
            ModuleManager.popEffect.addEffect(entity);
         }
      }
   }

   @Inject(
      method = "handleEntityEvent",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/game/ClientboundEntityEventPacket;getEntity(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"
      )
   )
   private void hookPopEffectChamsNewStatus(@NotNull ClientboundEntityEventPacket packet, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null && packet.getEventId() == 35) {
         if (packet.getEntity(client.level) instanceof Player player) {
            ModuleManager.popEffect.addChamsNewFromStatus(player);
         }
      }
   }

   @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
   private void sendChatMessageHook(@NotNull String message, CallbackInfo ci) {
      if (message.equals(String.valueOf(ModuleManager.unHook.code)) && UnHook.isActive()) {
         ModuleManager.unHook.disable();
      }

      if (!Module.fullNullCheck()) {
         if (message.startsWith(Managers.COMMAND.getPrefix())) {
            try {
               Managers.COMMAND.getDispatcher().execute(message.substring(Managers.COMMAND.getPrefix().length()), Managers.COMMAND.getSource());
            } catch (CommandSyntaxException var4) {
            }

            ci.cancel();
         }
      }
   }

   @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
   private void hookChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null) {
         LevelChunk chunk = client.level.getChunkSource().getChunk(packet.getX(), packet.getZ(), false);
         if (chunk != null) {
            Vcore.EVENT_BUS.post(new EventChunkData(chunk));
         }
      }
   }
}
