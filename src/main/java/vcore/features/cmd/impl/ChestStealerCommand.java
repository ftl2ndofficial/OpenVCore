package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.client.ModuleManager;
import vcore.features.cmd.Command;
import vcore.features.cmd.args.ChestStealerArgumentType;

public class ChestStealerCommand extends Command {
   public ChestStealerCommand() {
      super("cheststealer", "Manage ChestStealer whitelist.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(literal("reset").executes(context -> {
         ModuleManager.chestStealer.items.getValue().getItemsById().clear();
         sendMessage("ChestStealer got reset.");
         return 1;
      }));
      builder.then(literal("add").then(arg("item", ChestStealerArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("item", String.class);
         String result = getRegistered(blockName);
         if (result != null) {
            ModuleManager.chestStealer.items.getValue().getItemsById().add(result);
            sendMessage(ChatFormatting.GREEN + blockName + " added to ChestStealer");
         } else {
            sendMessage(ChatFormatting.RED + "There is no such item!");
         }

         return 1;
      })));
      builder.then(literal("del").then(arg("item", ChestStealerArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("item", String.class);
         String result = getRegistered(blockName);
         if (result != null) {
            ModuleManager.chestStealer.items.getValue().getItemsById().remove(result);
            sendMessage(ChatFormatting.GREEN + blockName + " removed from ChestStealer");
         } else {
            sendMessage(ChatFormatting.RED + "There is no such item!");
         }

         return 1;
      })));
      builder.executes(context -> {
         if (ModuleManager.chestStealer.items.getValue().getItemsById().isEmpty()) {
            sendMessage("ChestStealer list empty");
         } else {
            StringBuilder f = new StringBuilder("ChestStealer list: ");

            for (String name : ModuleManager.chestStealer.items.getValue().getItemsById()) {
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

   public static String getRegistered(String Name) {
      for (Block block : BuiltInRegistries.BLOCK) {
         if (block.getDescriptionId().replace("block.minecraft.", "").equalsIgnoreCase(Name)) {
            return block.getDescriptionId().replace("block.minecraft.", "");
         }
      }

      for (Item item : BuiltInRegistries.ITEM) {
         if (item.getDescriptionId().replace("item.minecraft.", "").equalsIgnoreCase(Name)) {
            return item.getDescriptionId().replace("item.minecraft.", "");
         }
      }

      return null;
   }
}
