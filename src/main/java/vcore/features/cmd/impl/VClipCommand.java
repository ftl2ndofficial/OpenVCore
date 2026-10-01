package vcore.features.cmd.impl;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import vcore.features.cmd.Command;
import vcore.features.modules.misc.ClientSettings;

public class VClipCommand extends Command {
   public VClipCommand() {
      super("vclip", "Clip up or down by distance.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(literal("down").executes(context -> {
         float y = 0.0F;

         for (int i = 1; i < 255; i++) {
            if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, -i, 0)) == Blocks.AIR.defaultBlockState()) {
               y = -i - 1;
               break;
            }

            if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, -i, 0)) == Blocks.BEDROCK.defaultBlockState()) {
               sendMessage(ChatFormatting.RED + "There's nowhere to clip!");
               return 1;
            }
         }

         this.clip(y);
         return 1;
      }));
      builder.then(literal("up").executes(context -> {
         float y = 0.0F;

         for (int i = 4; i < 255; i++) {
            if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, i, 0)) == Blocks.AIR.defaultBlockState()) {
               y = i + 1;
               break;
            }
         }

         this.clip(y);
         return 1;
      }));
      builder.then(arg("count", DoubleArgumentType.doubleArg()).executes(context -> {
         double count = (Double)context.getArgument("count", Double.class);

         try {
            sendMessage(ChatFormatting.GREEN + "Clipping by " + count + " blocks");
            this.clip(count);
         } catch (Exception var5) {
         }

         return 1;
      }));
      builder.executes(context -> {
         sendMessage("Try .vclip <number>");
         return 1;
      });
   }

   private void clip(double b) {
      if (ClientSettings.clipMode.getValue() == ClientSettings.ClipMode.Matrix) {
         for (int i = 0; i < 10; i++) {
            mc.player.connection.send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, mc.player.horizontalCollision));
         }

         for (int i = 0; i < 10; i++) {
            mc.player.connection.send(new Pos(mc.player.getX(), mc.player.getY() + b, mc.player.getZ(), false, mc.player.horizontalCollision));
         }
      } else {
         mc.player.connection.send(new Pos(mc.player.getX(), mc.player.getY() + b, mc.player.getZ(), false, mc.player.horizontalCollision));
      }

      mc.player.setPos(mc.player.getX(), mc.player.getY() + b, mc.player.getZ());
   }
}
