package vcore.features.cmd.impl;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import vcore.utility.KeyboardUtility;
import vcore.core.Managers;
import vcore.core.manager.client.MacroManager;
import vcore.features.cmd.Command;
import vcore.features.cmd.args.MacroArgumentType;

public class MacroCommand extends Command {
   public MacroCommand() {
      super("macro", "Manage chat/command macros.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(
         literal("list")
            .executes(
               context -> {
                  sendMessage("Macros:");
                  sendMessage(" ");
                  Managers.MACRO
                     .getMacros()
                     .forEach(
                        macro -> sendMessage(
                           macro.getName() + (macro.getBind() != -1 ? " [" + this.toString(macro.getBind()) + "]" : "") + " {" + macro.getText() + "}"
                        )
                     );
                  return 1;
               }
            )
      );
      builder.then(literal("remove").then(arg("macro", MacroArgumentType.create()).executes(context -> {
         MacroManager.Macro macro = (MacroManager.Macro)context.getArgument("macro", MacroManager.Macro.class);
         if (macro == null) {
            sendMessage("Wrong macro name!");
            return 1;
         } else {
            Managers.MACRO.removeMacro(macro);
            sendMessage("Removed macro " + macro.getName());
            return 1;
         }
      })));
      builder.then(
         literal("add")
            .then(
               arg("name", StringArgumentType.word())
                  .then(arg("bind", StringArgumentType.word()).then(arg("args", StringArgumentType.greedyString()).executes(context -> {
                     String name = (String)context.getArgument("name", String.class);
                     String bind = ((String)context.getArgument("bind", String.class)).toUpperCase();
                     String args = (String)context.getArgument("args", String.class);
                     if (InputConstants.getKey("key.keyboard." + bind.toLowerCase()).getValue() == -1) {
                        sendMessage("Wrong bind!");
                        return 1;
                     } else {
                        MacroManager.Macro macro = new MacroManager.Macro(
                           name, args, InputConstants.getKey("key.keyboard." + bind.toLowerCase()).getValue()
                        );
                        MacroManager.addMacro(macro);
                        sendMessage("Added macro " + name + " to " + this.toString(macro.getBind()));
                        return 1;
                     }
                  })))
            )
      );
      builder.executes(context -> {
         sendMessage(this.usage());
         return 1;
      });
   }

   public String toString(int key) {
      return KeyboardUtility.getBindName(key, false);
   }

   String usage() {
      return "macro add/remove/list (macro add name key text), (macro remove name)";
   }
}
