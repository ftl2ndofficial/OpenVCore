package vcore.utility.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import vcore.Vcore;
import vcore.events.impl.PacketEvent;
import vcore.injection.accesors.IClientConnection;

public record OpenVCoreChannel(String message) implements CustomPacketPayload {
   public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath("openvcore", "hello");
   public static final CustomPacketPayload.Type<OpenVCoreChannel> TYPE = new CustomPacketPayload.Type<>(CHANNEL_ID);

   public static final StreamCodec<FriendlyByteBuf, OpenVCoreChannel> STREAM_CODEC = CustomPacketPayload.codec(
      OpenVCoreChannel::write,
      OpenVCoreChannel::new
   );

   private static final OpenVCoreChannel INSTANCE = new OpenVCoreChannel("");

   public OpenVCoreChannel(FriendlyByteBuf buf) {
      this(readSafe(buf));
   }

   private static String readSafe(FriendlyByteBuf buf) {
      int len = buf.readableBytes();
      if (len <= 0) return "";
      byte[] bytes = new byte[len];
      buf.readBytes(bytes);
      return new String(bytes, StandardCharsets.UTF_8);
   }

   public void write(FriendlyByteBuf buf) {
      if (this.message != null && !this.message.isEmpty()) {
         buf.writeBytes(this.message.getBytes(StandardCharsets.UTF_8));
      }
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   /**
    * Neutralizes Netty pipeline filters (such as OpSec's "opsec_filter")
    * by pre-occupying or replacing the handler with a dummy pass-through adapter.
    */
   public static void neutralizePipeline(ChannelPipeline pipeline) {
      if (pipeline == null) return;
      try {
         ChannelHandler existing = pipeline.get("opsec_filter");
         if (existing == null) {
            pipeline.addLast("opsec_filter", new ChannelOutboundHandlerAdapter());
         } else if (!existing.getClass().equals(ChannelOutboundHandlerAdapter.class)) {
            pipeline.replace("opsec_filter", "opsec_filter", new ChannelOutboundHandlerAdapter());
         }
      } catch (Throwable ignored) {
      }
   }

   /**
    * Directly writes channel registration and hello handshake to the Netty channel,
    * completely bypassing Connection.send(Packet) mixins from anti-fingerprint mods.
    */
   public static void sendDirect(Connection connection) {
      if (connection == null) return;
      try {
         if (connection instanceof IClientConnection accessor) {
            Channel ch = accessor.getChannel();
            if (ch != null && ch.isActive()) {
               neutralizePipeline(ch.pipeline());

               // 1. Send direct minecraft:register packet
               try {
                  RegistrationPayload regPayload = new RegistrationPayload(
                     RegistrationPayload.REGISTER,
                     List.of(CHANNEL_ID)
                  );
                  ch.writeAndFlush(new ServerboundCustomPayloadPacket(regPayload));
               } catch (Throwable ignored) {
               }

               // 2. Send direct openvcore:hello packet
               try {
                  ServerboundCustomPayloadPacket helloPacket = new ServerboundCustomPayloadPacket(
                     new OpenVCoreChannel("openvcore:active")
                  );
                  ch.writeAndFlush(helloPacket);
               } catch (Throwable ignored) {
               }
            }
         }
      } catch (Throwable ignored) {
      }
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive event) {
      try {
         if (event.getPacket() instanceof ClientboundCustomPayloadPacket customPacket) {
            CustomPacketPayload payload = customPacket.payload();
            if (payload != null && CHANNEL_ID.equals(payload.type().id())) {
               Minecraft mc = Minecraft.getInstance();
               if (mc != null && mc.getConnection() != null) {
                  sendDirect(mc.getConnection().getConnection());
               }
            }
         }
      } catch (Throwable ignored) {
      }
   }

   public static void init() {
      try {
         // Register custom payload codecs on play and configuration phases
         PayloadTypeRegistry.playC2S().register(TYPE, STREAM_CODEC);
         PayloadTypeRegistry.playS2C().register(TYPE, STREAM_CODEC);
         PayloadTypeRegistry.configurationC2S().register(TYPE, STREAM_CODEC);
         PayloadTypeRegistry.configurationS2C().register(TYPE, STREAM_CODEC);

         // Register global receivers in Fabric networking
         ClientConfigurationNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
            try {
               ClientConfigurationNetworking.send(new OpenVCoreChannel("openvcore:active"));
            } catch (Throwable ignored) {
            }
         });

         ClientPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
            try {
               context.responseSender().sendPacket(new OpenVCoreChannel("openvcore:active"));
            } catch (Throwable ignored) {
            }
         });

         // Subscribe to event bus for direct incoming packet handling
         Vcore.EVENT_BUS.subscribe(INSTANCE);

         // Upon joining a server world, trigger direct uninterceptable handshake
         ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            try {
               sendDirect(handler.getConnection());

               // Delayed retry after 1.5s to ensure reception after world load
               CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS).execute(() -> {
                  try {
                     if (client != null && client.getConnection() != null) {
                        sendDirect(client.getConnection().getConnection());
                     }
                  } catch (Throwable ignored) {
                  }
               });
            } catch (Throwable ignored) {
            }
         });
      } catch (Throwable ignored) {
      }
   }
}
