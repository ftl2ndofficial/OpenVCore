package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.features.cmd.Command;
import vcore.features.cmd.args.FriendArgumentType;
import vcore.features.cmd.args.PlayerArgumentType;

public class FriendCommand extends Command {
   public FriendCommand() {
      super("friend", "Manage the friend list.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(literal("reset").executes(context -> {
         Managers.FRIEND.clear();
         sendMessage("Friends got reset.");
         return 1;
      }));
      builder.then(literal("add").then(arg("player", PlayerArgumentType.create()).executes(context -> {
         PlayerInfo player = (PlayerInfo)context.getArgument("player", PlayerInfo.class);
         Managers.FRIEND.addFriend(player.getProfile().name());
         sendMessage(player.getProfile().name() + " has been friended");
         return 1;
      })));
      builder.then(literal("remove").then(arg("player", FriendArgumentType.create()).executes(context -> {
         String nickname = (String)context.getArgument("player", String.class);
         Managers.FRIEND.removeFriend(nickname);
         sendMessage(nickname + " has been unfriended");
         return 1;
      })));
      builder.then(literal("is").then(arg("player", PlayerArgumentType.create()).executes(context -> {
         PlayerInfo player = (PlayerInfo)context.getArgument("player", PlayerInfo.class);
         sendMessage(player.getProfile().name() + (Managers.FRIEND.isFriend(player.getProfile().name()) ? " is friended." : " isn't friended."));
         return 1;
      })));
      builder.executes(context -> {
         if (Managers.FRIEND.getFriends().isEmpty()) {
            sendMessage("Friend list empty D:");
         } else {
            StringBuilder f = new StringBuilder("Friends: ");

            for (String friend : Managers.FRIEND.getFriends()) {
               try {
                  f.append(friend).append(", ");
               } catch (Exception var5) {
               }
            }

            sendMessage(f.toString());
         }

         return 1;
      });
   }
}
