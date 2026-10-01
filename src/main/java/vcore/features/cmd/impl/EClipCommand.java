package vcore.features.cmd.impl;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import vcore.features.cmd.Command;
import vcore.utility.player.InventoryUtility;

public class EClipCommand extends Command {
   public EClipCommand() {
      super("eclip", "Elytra clip vertically.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(((LiteralArgumentBuilder)literal("bedrock").executes(context -> {
         this.execute(-((float)mc.player.getY()) - 3.0F);
         return 1;
      })).then(arg("number", FloatArgumentType.floatArg()).executes(context -> {
         float y = -((float)mc.player.getY()) - 3.0F;
         if (y == 0.0F) {
            y = (Float)context.getArgument("number", Float.class);
         }

         this.execute(y);
         return 1;
      })));
      builder.then(((LiteralArgumentBuilder)literal("down").executes(context -> {
         float y = 0.0F;

         for (int i = 1; i < 255; i++) {
            if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, -i, 0)) == Blocks.AIR.defaultBlockState()) {
               y = -i - 1;
               break;
            }

            if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, -i, 0)) == Blocks.BEDROCK.defaultBlockState()) {
               sendMessage(ChatFormatting.RED + "You can only teleport under bedrock");
               sendMessage(ChatFormatting.RED + " eclip bedrock");
               return 1;
            }
         }

         this.execute(y);
         return 1;
      })).then(arg("number", FloatArgumentType.floatArg()).executes(context -> {
         float y = 0.0F;

         for (int i = 1; i < 255; i++) {
            if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, -i, 0)) == Blocks.AIR.defaultBlockState()) {
               y = -i - 1;
               break;
            }

            if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, -i, 0)) == Blocks.BEDROCK.defaultBlockState()) {
               sendMessage(ChatFormatting.RED + "You can only teleport under bedrock");
               sendMessage(ChatFormatting.RED + " eclip bedrock");
               return 1;
            }
         }

         if (y == 0.0F) {
            y = (Float)context.getArgument("number", Float.class);
         }

         this.execute(y);
         return 1;
      })));
      builder.then(((LiteralArgumentBuilder)literal("up").executes(context -> {
         float y = 0.0F;

         for (int i = 4; i < 255; i++) {
            if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, i, 0)) == Blocks.AIR.defaultBlockState()) {
               y = i + 1;
               break;
            }
         }

         this.execute(y);
         return 1;
      })).then(arg("number", FloatArgumentType.floatArg()).executes(context -> {
         float y = 0.0F;

         for (int i = 4; i < 255; i++) {
            if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, i, 0)) == Blocks.AIR.defaultBlockState()) {
               y = i + 1;
               break;
            }
         }

         if (y == 0.0F) {
            y = (Float)context.getArgument("number", Float.class);
         }

         this.execute(y);
         return 1;
      })));
   }

   private void execute(float y) {
      int elytra;
      if ((elytra = InventoryUtility.findItemInInventory(Items.ELYTRA).slot()) == -1) {
         sendMessage(ChatFormatting.RED + "You need an elytra in your inventory");
      } else {
         if (elytra != -2) {
            mc.gameMode.handleInventoryMouseClick(0, elytra, 1, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(0, 6, 1, ClickType.PICKUP, mc.player);
         }

         mc.player.connection.send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, mc.player.horizontalCollision));
         mc.player.connection.send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, mc.player.horizontalCollision));
         mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
         mc.player.connection.send(new Pos(mc.player.getX(), mc.player.getY() + y, mc.player.getZ(), false, mc.player.horizontalCollision));
         mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
         if (elytra != -2) {
            mc.gameMode.handleInventoryMouseClick(0, 6, 1, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(0, elytra, 1, ClickType.PICKUP, mc.player);
         }

         mc.player.setPos(mc.player.getX(), mc.player.getY() + y, mc.player.getZ());
      }
   }
}
