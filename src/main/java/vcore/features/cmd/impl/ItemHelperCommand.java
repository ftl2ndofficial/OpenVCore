package vcore.features.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.client.ModuleManager;
import vcore.features.cmd.Command;
import vcore.features.modules.player.ItemHelper;

public class ItemHelperCommand extends Command {
   public ItemHelperCommand() {
      super("itemhelper", "Manage ItemHelper bindings and items.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(literal("set").executes(context -> {
         ItemHelper helper = ModuleManager.itemHelper;
         if (helper != null && mc.player != null) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack != null && !stack.isEmpty()) {
               int emptyIndex = helper.findEmptyIndex();
               if (emptyIndex == -1) {
                  sendMessage("No empty item slot available.");
                  return 1;
               } else {
                  helper.setItemAt(emptyIndex, stack.getItem());
                  sendMessage(stack.getHoverName().getString() + " has been set to Item " + (emptyIndex + 1));
                  return 1;
               }
            } else {
               sendMessage("Your hand is empty.");
               return 1;
            }
         } else {
            return 1;
         }
      }));
      builder.then(literal("reset").executes(context -> {
         ItemHelper helper = ModuleManager.itemHelper;
         if (helper != null) {
            helper.resetAll();
         }

         sendMessage("Reset ItemHelper successfully.");
         return 1;
      }));
      builder.then(((RequiredArgumentBuilder)arg("slot", StringArgumentType.word()).then(literal("set").executes(context -> {
         ItemHelper helper = ModuleManager.itemHelper;
         if (helper != null && mc.player != null) {
            int index = this.parseSlot((String)context.getArgument("slot", String.class));
            if (index >= 0 && index < helper.getItemSettings().size()) {
               ItemStack stack = mc.player.getMainHandItem();
               if (stack != null && !stack.isEmpty()) {
                  helper.setItemAt(index, stack.getItem());
                  sendMessage("Successfully set item " + stack.getHoverName().getString());
                  return 1;
               } else {
                  sendMessage("Your hand is empty.");
                  return 1;
               }
            } else {
               sendMessage("Invalid slot.");
               return 1;
            }
         } else {
            return 1;
         }
      }))).then(literal("del").executes(context -> {
         ItemHelper helper = ModuleManager.itemHelper;
         if (helper == null) {
            return 1;
         } else {
            int index = this.parseSlot((String)context.getArgument("slot", String.class));
            if (index >= 0 && index < helper.getItemSettings().size()) {
               helper.clearItemAt(index);
               sendMessage("Cleared Item " + (index + 1));
               return 1;
            } else {
               sendMessage("Invalid slot.");
               return 1;
            }
         }
      })));
   }

   private int parseSlot(String raw) {
      String normalized = raw.replace("_", "").replace("-", "").toLowerCase();
      if (normalized.startsWith("item")) {
         normalized = normalized.substring(4);
      }

      try {
         int value = Integer.parseInt(normalized);
         return value - 1;
      } catch (NumberFormatException ignored) {
         return -1;
      }
   }
}
