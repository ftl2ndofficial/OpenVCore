package vcore.core;

import com.mojang.blaze3d.platform.InputConstants;
import vcore.utility.render.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import vcore.Vcore;
import vcore.core.manager.client.MacroManager;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventDeath;
import vcore.events.impl.EventKeyPress;
import vcore.events.impl.EventMouse;
import vcore.events.impl.PacketEvent;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.cmd.Command;
import vcore.features.modules.Module;
import vcore.features.modules.misc.ClientSettings;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.gui.notification.Notification;
import vcore.utility.Timer;
import vcore.utility.player.InteractionUtility;
import vcore.utility.render.Render2DEngine;

public final class Core {
   public static boolean lockSprint;
   public static boolean serverSprint;
   public static boolean hold_mouse0;
   public static final Map<String, Identifier> HEADS = new ConcurrentHashMap<>();
   public final Set<Packet<?>> silentPackets = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
   public final Set<Packet<?>> silentPostPackets = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
   private final Timer lastPacket = new Timer();
   private final Timer autoSave = new Timer();
   private final Timer setBackTimer = new Timer();

   @EventHandler
   public void onTick(PlayerUpdateEvent event) {
      if (!Module.fullNullCheck()) {
         Managers.NOTIFICATION.onUpdate();
         Managers.MODULE.onUpdate();
         if (ModuleManager.clickGui.getBind().getKey() == -1) {
            Command.sendMessage(ChatFormatting.RED + "Default clickgui keybind --> Right Shift");
            ModuleManager.clickGui.setBind(InputConstants.getKey("key.keyboard.right.shift").getValue(), false, false);
         }

         for (Player p : Module.mc.level.players()) {
            if (p.isDeadOrDying() || p.getHealth() == 0.0F) {
               Vcore.EVENT_BUS.post(new EventDeath(p));
            }
         }

         if (!Objects.equals(Managers.COMMAND.getPrefix(), ClientSettings.prefix.getValue())) {
            Managers.COMMAND.setPrefix(ClientSettings.prefix.getValue());
         }

         new HashMap<>(InteractionUtility.awaiting).forEach((bp, time) -> {
            if ((float)(System.currentTimeMillis() - time) > Managers.SERVER.getPing() * 2.0F) {
               InteractionUtility.awaiting.remove(bp);
            }
         });
         if (this.autoSave.every(600000L)) {
            Managers.FRIEND.saveFriends();
            Managers.CONFIG.save(Managers.CONFIG.getCurrentConfig());
            Managers.MACRO.saveMacro();
            Managers.NOTIFICATION.publicity("AutoSave", "Saving config..", 3, Notification.Type.INFO);
         }
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.@NotNull Send e) {
      if (e.getPacket() instanceof ServerboundMovePlayerPacket && !(e.getPacket() instanceof StatusOnly)) {
         this.lastPacket.reset();
      }

      if (e.getPacket() instanceof ServerboundPlayerCommandPacket c && (c.getAction() == Action.START_SPRINTING || c.getAction() == Action.STOP_SPRINTING)) {
         if (lockSprint) {
            e.cancel();
            return;
         }

         switch (c.getAction()) {
            case START_SPRINTING:
               serverSprint = true;
               break;
            case STOP_SPRINTING:
               serverSprint = false;
         }
      }
   }

   public void onRender2D(GuiGraphics e) {
      this.drawGps(e);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (!Module.fullNullCheck()) {
         if (e.getPacket() instanceof ClientboundLoginPacket) {
            Managers.MODULE.onLogin();
         }

         if (e.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.setBackTimer.reset();
         }
      }
   }

   public void drawGps(GuiGraphics e) {
      if (Vcore.gps_position != null) {
         float dst = this.getDistance(Vcore.gps_position);
         float xOffset = Module.mc.getWindow().getGuiScaledWidth() / 2.0F;
         float yOffset = Module.mc.getWindow().getGuiScaledHeight() / 6.0F;
         float radius = 5.0F;
         float yaw = getRotations(new Vec2(Vcore.gps_position.getX(), Vcore.gps_position.getZ())) - Module.mc.player.getYRot();
         float pointerHeight = 12.5F;
         float pointerCenterOffset = pointerHeight * 2.0F / 3.0F - 2.0F;
         float px = (float)(Math.sin(Math.toRadians(yaw)) * radius) + xOffset;
         float py = (float)(yOffset - Math.cos(Math.toRadians(yaw)) * radius) + pointerCenterOffset;
         PoseStack pose = new PoseStack();
         pose.translate(px, py, 0.0F);
         pose.mulPose(Axis.ZP.rotationDegrees(yaw));
         Render2DEngine.drawTracerPointer(pose, 0.0F, -pointerCenterOffset, pointerHeight, 0.5F, 3.63F, true, true, HudEditor.getColor(1).getRGB());
         pose.mulPose(Axis.ZP.rotationDegrees(-yaw));
         pose.translate(-px, -py, 0.0F);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         FontRenderers.modules.drawCenteredString(pose, "gps (" + dst + "m)", xOffset, yOffset + 20.0F, -1);
         if (dst < 10.0F) {
            Vcore.gps_position = null;
         }
      }
   }

   @EventHandler
   public void onKeyPress(EventKeyPress event) {
      if (event.getKey() != -1) {
         for (MacroManager.Macro m : Managers.MACRO.getMacros()) {
            if (m.getBind() == event.getKey()) {
               m.runMacro();
            }
         }
      }
   }

   @EventHandler
   public void onMouse(EventMouse event) {
      if (event.getAction() == 0) {
         hold_mouse0 = false;
      }

      if (event.getAction() == 1) {
         hold_mouse0 = true;
      }
   }

   public int getDistance(BlockPos bp) {
      double d0 = Module.mc.player.getX() - bp.getX();
      double d2 = Module.mc.player.getZ() - bp.getZ();
      return (int)Mth.sqrt((float)(d0 * d0 + d2 * d2));
   }

   public long getSetBackTime() {
      return this.setBackTimer.getPassedTimeMs();
   }

   public static float getRotations(Vec2 vec) {
      if (Module.mc.player == null) {
         return 0.0F;
      }

      double x = vec.x - Module.mc.player.position().x;
      double z = vec.y - Module.mc.player.position().z;
      return (float)(-(Math.atan2(x, z) * (180.0 / Math.PI)));
   }

   public void bobView(PoseStack matrices, float tickDelta) {
      if (Module.mc.getCameraEntity() instanceof AbstractClientPlayer playerEntity) {
         float var6 = playerEntity.avatarState().getBackwardsInterpolatedWalkDistance(tickDelta);
         float h = playerEntity.avatarState().getInterpolatedBob(tickDelta);
         matrices.translate(Mth.sin(var6 * (float) Math.PI) * h * 0.1F, -Math.abs(Mth.cos(var6 * (float) Math.PI) * h) * 0.3, 0.0);
         matrices.mulPose(Axis.ZP.rotationDegrees(Mth.sin(var6 * (float) Math.PI) * h * 3.0F));
         matrices.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(var6 * (float) Math.PI - 0.2F) * h) * 0.3F));
      }
   }
}
