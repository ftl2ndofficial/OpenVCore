package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.client.ModuleManager;
import vcore.features.cmd.Command;
import vcore.features.cmd.args.SearchArgumentType;

public class BlockESPCommand extends Command {
   public BlockESPCommand() {
      super("blockesp", "Manage BlockESP highlight list.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(literal("reset").executes(context -> {
         ModuleManager.blockESP.selectedBlocks.getValue().clear();
         sendMessage("BlockESP got reset.");
         mc.levelRenderer.allChanged();
         return 1;
      }));
      builder.then(literal("add").then(arg("block", SearchArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("block", String.class);
         Block result = getRegisteredBlock(blockName);
         if (result != null) {
            ModuleManager.blockESP.selectedBlocks.getValue().add(result);
            sendMessage(ChatFormatting.GREEN + blockName + " added to BlockESP");
         } else {
            sendMessage(ChatFormatting.RED + "There is no such block!");
         }

         mc.levelRenderer.allChanged();
         return 1;
      })));
      builder.then(literal("del").then(arg("block", SearchArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("block", String.class);
         Block result = getRegisteredBlock(blockName);
         if (result != null) {
            ModuleManager.blockESP.selectedBlocks.getValue().remove(result);
            sendMessage(ChatFormatting.GREEN + blockName + " removed from BlockESP");
         } else {
            sendMessage(ChatFormatting.RED + "There is no such block!");
         }

         mc.levelRenderer.allChanged();
         return 1;
      })));
      builder.executes(context -> {
         if (ModuleManager.blockESP.selectedBlocks.getValue().getItemsById().isEmpty()) {
            sendMessage("BlockESP list empty");
         } else {
            StringBuilder f = new StringBuilder("BlockESP list: ");

            for (String name : ModuleManager.blockESP.selectedBlocks.getValue().getItemsById()) {
               try {
                  f.append(name).append(", ");
               } catch (Exception var5) {
               }
            }

            sendMessage(f.toString());
         }

         return 1;
      });
   }

   public static Block getRegisteredBlock(String blockName) {
      for (Block block : BuiltInRegistries.BLOCK) {
         if (block.getDescriptionId().replace("block.minecraft.", "").equalsIgnoreCase(blockName.replace("block.minecraft.", ""))) {
            return block;
         }
      }

      return null;
   }
}
