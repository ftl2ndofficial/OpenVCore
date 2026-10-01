package vcore.features.modules.render;

import java.awt.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.setting.impl.ColorSetting;

public class WorldTweaks extends Module {
   public static final Setting<BooleanSettingGroup> fogModify = new Setting<>("FogModify", new BooleanSettingGroup(true));
   public static final Setting<Integer> fogStart = new Setting<>("FogStart", 0, 0, 256).addToGroup(fogModify);
   public static final Setting<Integer> fogEnd = new Setting<>("FogEnd", 128, 10, 256).addToGroup(fogModify);
   public static final Setting<ColorSetting> fogColor = new Setting<>("FogColor", new ColorSetting(new Color(30975))).addToGroup(fogModify);
   public final Setting<Boolean> ctime = new Setting<>("ChangeTime", true);
   public final Setting<Integer> ctimeVal = new Setting<>("Time", 21, 0, 23);
   private long oldTime;

   public WorldTweaks() {
      super("WorldTweaks", "World rendering tweaks.", Module.Category.RENDER);
   }

   @Override
   public void onEnable() {
      if (mc.level != null) {
         this.oldTime = mc.level.getLevelData().getDayTime();
      }
   }

   @Override
   public void onDisable() {
      if (mc.level != null) {
         mc.level.setTimeFromServer(mc.level.getLevelData().getGameTime(), this.oldTime, true);
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof ClientboundSetTimePacket packet && this.ctime.getValue()) {
         this.oldTime = packet.dayTime();
         event.cancel();
      }
   }

   @Override
   public void onUpdate() {
      if (this.ctime.getValue() && mc.level != null) {
         mc.level.setTimeFromServer(mc.level.getLevelData().getGameTime(), this.ctimeVal.getValue().intValue() * 1000L, false);
      }
   }
}
