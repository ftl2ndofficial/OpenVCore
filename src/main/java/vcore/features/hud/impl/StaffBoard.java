package vcore.features.hud.impl;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import vcore.features.cmd.impl.StaffCommand;
import vcore.features.hud.HudElement;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.setting.impl.PositionSetting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.animation.AnimationUtility;

public class StaffBoard extends HudElement {
   private static final Pattern validUserPattern = Pattern.compile("^\\w{3,16}$");
   private List<String> players = new ArrayList<>();
   private List<String> notSpec = new ArrayList<>();
   private Map<String, Identifier> skinMap = new HashMap<>();
   private float vAnimation;
   private float hAnimation;

   public StaffBoard(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("StaffBoard", 50, 50, position, activeElementsSupplier);
   }

   public static List<String> getOnlinePlayer() {
      if (mc.player == null || mc.player.connection == null) {
         return Collections.emptyList();
      }
      return mc.player
         .connection
         .getOnlinePlayers()
         .stream()
         .map(PlayerInfo::getProfile)
         .<String>map(GameProfile::name)
         .filter(profileName -> validUserPattern.matcher(profileName).matches())
         .collect(Collectors.toList());
   }

   public static List<String> getOnlinePlayerD() {
      List<String> S = new ArrayList<>();
      if (mc.player == null || mc.player.connection == null) {
         return S;
      }

      for (PlayerInfo player : mc.player.connection.getOnlinePlayers()) {
         if (mc.isLocalServer() || player.getTeam() == null) {
            break;
         }

         String prefix = player.getTeam().getPlayerPrefix().getString();
         String playerName = player.getProfile().name();
         if (check(ChatFormatting.stripFormatting(prefix).toLowerCase())
            || StaffCommand.staffNames.toString().toLowerCase().contains(playerName.toLowerCase())
            || playerName.toLowerCase().contains("1danil_mansoru1")
            || playerName.toLowerCase().contains("barslan_")
            || playerName.toLowerCase().contains("timmings")
            || playerName.toLowerCase().contains("timings")
            || playerName.toLowerCase().contains("ruthless")
            || player.getTeam().getPlayerPrefix().getString().contains("YT")
            || player.getTeam().getPlayerPrefix().getString().contains("Y") && player.getTeam().getPlayerPrefix().getString().contains("T")) {
            String name = Arrays.asList(player.getTeam().getPlayers().toArray()).toString().replace("[", "").replace("]", "");
            if (player.getGameMode() == GameType.SPECTATOR) {
               S.add(player.getTeam().getPlayerPrefix().getString() + name + ":gm3");
            } else {
               S.add(player.getTeam().getPlayerPrefix().getString() + name + ":active");
            }
         }
      }

      return S;
   }

   public List<String> getVanish() {
      List<String> list = new ArrayList<>();

      for (PlayerTeam s : mc.level.getScoreboard().getPlayerTeams()) {
         if (!s.getPlayerPrefix().getString().isEmpty() && !mc.isLocalServer()) {
            String name = Arrays.asList(s.getPlayers().toArray()).toString().replace("[", "").replace("]", "");
            if (!getOnlinePlayer().contains(name)
               && !name.isEmpty()
               && (
                  StaffCommand.staffNames.toString().toLowerCase().contains(name.toLowerCase()) && check(s.getPlayerPrefix().getString().toLowerCase())
                     || check(s.getPlayerPrefix().getString().toLowerCase())
                     || name.toLowerCase().contains("1danil_mansoru1")
                     || name.toLowerCase().contains("barslan_")
                     || name.toLowerCase().contains("timmings")
                     || name.toLowerCase().contains("timings")
                     || name.toLowerCase().contains("ruthless")
                     || s.getPlayerPrefix().getString().contains("YT")
                     || s.getPlayerPrefix().getString().contains("Y") && s.getPlayerPrefix().getString().contains("T")
               )) {
               list.add(s.getPlayerPrefix().getString() + name + ":vanish");
            }
         }
      }

      return list;
   }

   public static boolean check(String name) {
      return mc.getCurrentServer() != null && mc.getCurrentServer().ip.contains("mcfunny")
         ? name.contains("helper") || name.contains("moder") || name.contains("Ð¼Ð¾Ð´ÐµÑ€") || name.contains("Ñ…ÐµÐ»Ð¿ÐµÑ€")
         : name.contains("helper")
            || name.contains("moder")
            || name.contains("admin")
            || name.contains("owner")
            || name.contains("curator")
            || name.contains("ÐºÑƒÑ€Ð°Ñ‚Ð¾Ñ€")
            || name.contains("Ð¼Ð¾Ð´ÐµÑ€")
            || name.contains("Ð°Ð´Ð¼Ð¸Ð½")
            || name.contains("Ñ…ÐµÐ»Ð¿ÐµÑ€")
            || name.contains("Ð¿Ð¾Ð´Ð´ÐµÑ€Ð¶ÐºÐ°")
            || name.contains("Ñ\u0081Ð¾Ñ‚Ñ€ÑƒÐ´Ð½Ð¸Ðº")
            || name.contains("Ð·Ð°Ð¼")
            || name.contains("Ñ\u0081Ñ‚Ð°Ð¶Ñ‘Ñ€");
   }

   @Override
   public void render(GuiGraphics context) {
      super.render(context);
      List<String> all = new ArrayList<>();
      all.addAll(this.players);
      all.addAll(this.notSpec);
      int y_offset1 = 0;
      float max_width = 50.0F;
      float pointerX = 0.0F;

      for (String player : all) {
         if (y_offset1 == 0) {
            y_offset1 += 4;
         }

         y_offset1 += 9;
         float nameWidth = FontRenderers.sf_bold_mini.getStringWidth(player.split(":")[0]);
         float timeWidth = FontRenderers.sf_bold_mini
            .getStringWidth(
               player.split(":")[1].equalsIgnoreCase("vanish")
                  ? ChatFormatting.RED + "V"
                  : (player.split(":")[1].equalsIgnoreCase("gm3") ? ChatFormatting.RED + "V " + ChatFormatting.YELLOW + "(GM3)" : ChatFormatting.GREEN + "Z")
            );
         float width = (nameWidth + timeWidth) * 1.4F;
         if (width > max_width) {
            max_width = width;
         }

         if (timeWidth > pointerX) {
            pointerX = timeWidth;
         }
      }

      this.vAnimation = AnimationUtility.fast(this.vAnimation, 14 + y_offset1, 15.0F);
      this.hAnimation = AnimationUtility.fast(this.hAnimation, max_width, 15.0F);
      PoseStack matrices = Render2DEngine.toPoseStack(context.pose());
      Render2DEngine.drawHudBase(matrices, this.getPosX(), this.getPosY(), this.hAnimation, this.vAnimation, 4.0F);
      if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
         FontRenderers.sf_bold
            .drawCenteredString(context.pose(), "Staff", this.getPosX() + this.hAnimation / 2.0F, this.getPosY() + 4.0F, HudEditor.getTextColor());
      } else {
         FontRenderers.sf_bold.drawGradientCenteredString(context.pose(), "Staff", this.getPosX() + this.hAnimation / 2.0F, this.getPosY() + 4.0F, 10);
      }

      if (y_offset1 > 0) {
         if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawRectDumbWay(
               matrices,
               this.getPosX() + 4.0F,
               this.getPosY() + 13.0F,
               this.getPosX() + this.getWidth() - 8.0F,
               this.getPosY() + 14.0F,
               new Color(1426063359, true)
            );
         } else {
            Render2DEngine.horizontalGradient(
               matrices,
               this.getPosX() + 2.0F,
               this.getPosY() + 13.7F,
               this.getPosX() + 2.0F + this.hAnimation / 2.0F - 2.0F,
               this.getPosY() + 13.5F,
               Render2DEngine.injectAlpha(HudEditor.getTextColor(), 0),
               HudEditor.getTextColor()
            );
            Render2DEngine.horizontalGradient(
               matrices,
               this.getPosX() + 2.0F + this.hAnimation / 2.0F - 2.0F,
               this.getPosY() + 13.7F,
               this.getPosX() + 2.0F + this.hAnimation - 4.0F,
               this.getPosY() + 14.0F,
               HudEditor.getTextColor(),
               Render2DEngine.injectAlpha(HudEditor.getTextColor(), 0)
            );
         }
      }

      Render2DEngine.addWindow(matrices, this.getPosX(), this.getPosY(), this.getPosX() + this.hAnimation, this.getPosY() + this.vAnimation, 1.0);
      int y_offset = 0;

      for (String player : all) {
         float px = this.getPosX() + (max_width - pointerX - 10.0F);
         Identifier tex = this.getTexture(player);
         if (tex != null) {
            PlayerFaceRenderer.draw(context, tex, (int)(this.getPosX() + 3.0F), (int)(this.getPosY() + 16.0F + y_offset), 8, true, false, -1);
         }

         FontRenderers.sf_bold_mini
            .drawString(
               context.pose(), player.split(":")[0], this.getPosX() + 13.0F, this.getPosY() + 19.0F + y_offset, HudEditor.getTextColor().getRGB()
            );
         FontRenderers.sf_bold_mini
            .drawCenteredString(
               context.pose(),
               player.split(":")[1].equalsIgnoreCase("vanish")
                  ? ChatFormatting.RED + "O"
                  : (player.split(":")[1].equalsIgnoreCase("gm3") ? ChatFormatting.YELLOW + "O" : ChatFormatting.GREEN + "O"),
               px + (this.getPosX() + max_width - px) / 2.0F,
               this.getPosY() + 19.0F + y_offset,
               HudEditor.getTextColor().getRGB()
            );
         Render2DEngine.drawRect(matrices, px, this.getPosY() + 17.0F + y_offset, 0.5F, 8.0F, new Color(1157627903, true));
         y_offset += 9;
      }

      Render2DEngine.popWindow();
      this.setBounds(this.getPosX(), this.getPosY(), this.hAnimation, this.vAnimation);
   }

   @Override
   public void tick() {
      if (mc.player != null && mc.player.tickCount % 10 == 0) {
         this.players = this.getVanish();
         this.notSpec = getOnlinePlayerD();
         this.players.sort(String::compareTo);
         this.notSpec.sort(String::compareTo);
      }
   }

   private Identifier getTexture(String n) {
      Identifier id = null;
      if (this.skinMap.containsKey(n)) {
         id = this.skinMap.get(n);
      }

      if (mc.getConnection() != null) {
         for (PlayerInfo ple : mc.getConnection().getOnlinePlayers()) {
            if (n.contains(ple.getProfile().name())) {
               id = ple.getSkin().body().texturePath();
               if (!this.skinMap.containsKey(n)) {
                  this.skinMap.put(n, id);
               }
               break;
            }
         }
      }

      return id;
   }
}
