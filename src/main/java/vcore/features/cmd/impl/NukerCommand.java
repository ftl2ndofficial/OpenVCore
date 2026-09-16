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

public class NukerCommand extends Command {
   public NukerCommand() {
      super("nuker", "Manage Nuker block whitelist.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(literal("reset").executes(context -> {
         ModuleManager.nuker.selectedBlocks.getValue().clear();
         sendMessage("Nuker got reset!");
         return 1;
      }));
      builder.then(literal("add").then(arg("block", SearchArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("block", String.class);
         Block result = getRegisteredBlock(blockName);
         if (result != null) {
            ModuleManager.nuker.selectedBlocks.getValue().add(result);
            sendMessage(ChatFormatting.GREEN + blockName + " added to Nuker");
         } else {
            sendMessage(ChatFormatting.RED + "There is no such block!");
         }

         return 1;
      })));
      builder.then(literal("del").then(arg("block", SearchArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("block", String.class);
         Block result = getRegisteredBlock(blockName);
         if (result != null) {
            ModuleManager.nuker.selectedBlocks.getValue().remove(blockName);
            sendMessage(ChatFormatting.GREEN + blockName + " removed from Nuker");
         } else {
            sendMessage(ChatFormatting.RED + "There is no such block!");
         }

         return 1;
      })));
      builder.executes(context -> {
         if (ModuleManager.nuker.selectedBlocks.getValue().getItemsById().isEmpty()) {
            sendMessage("Nuker list empty");
         } else {
            StringBuilder f = new StringBuilder("Nuker list: ");

            for (String name : ModuleManager.nuker.selectedBlocks.getValue().getItemsById()) {
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
