package vcore.features.cmd.impl;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import vcore.features.cmd.Command;

public class HClipCommand extends Command {
   public HClipCommand() {
      super("hclip", "Clip horizontally in facing direction.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(literal("s").executes(context -> {
         double x = -(Mth.sin(mc.player.getYRot() * (float) (Math.PI / 180.0)) * 0.8);
         double z = Mth.cos(mc.player.getYRot() * (float) (Math.PI / 180.0)) * 0.8;

         for (int i = 0; i < 10; i++) {
            mc.player.connection.send(new Pos(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z, false, mc.player.horizontalCollision));
         }

         mc.player.setPos(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);
         return 1;
      }));
      builder.then(
         arg("count", DoubleArgumentType.doubleArg())
            .executes(
               context -> {
                  double speed = (Double)context.getArgument("count", Double.class);

                  try {
                     sendMessage(ChatFormatting.GREEN + "Clipping by " + speed + " blocks.");
                     mc.player
                        .setPos(
                           mc.player.getX() - Mth.sin(mc.player.getYRot() * (float) (Math.PI / 180.0)) * speed,
                           mc.player.getY(),
                           mc.player.getZ() + Mth.cos(mc.player.getYRot() * (float) (Math.PI / 180.0)) * speed
                        );
                  } catch (Exception var4) {
                  }

                  return 1;
               }
            )
      );
      builder.executes(context -> {
         sendMessage("Try .hclip <number>, .hclip s");
         return 1;
      });
   }
}
