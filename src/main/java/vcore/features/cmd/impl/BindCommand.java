package vcore.features.cmd.impl;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.features.cmd.Command;
import vcore.features.cmd.args.ModuleArgumentType;
import vcore.features.hud.impl.KeyBinds;
import vcore.features.modules.Module;
import vcore.setting.impl.Bind;

public class BindCommand extends Command {
   public BindCommand() {
      super("bind", "Bind a module to a key.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(arg("module", ModuleArgumentType.create()).then(arg("key", StringArgumentType.word()).executes(context -> {
         Module module = (Module)context.getArgument("module", Module.class);
         String stringKey = (String)context.getArgument("key", String.class);
         if (stringKey == null) {
            sendMessage(module.getName() + " is bound to " + ChatFormatting.GRAY + module.getBind().getBind());
            return 1;
         }

         int key;
         if (!stringKey.equalsIgnoreCase("none") && !stringKey.equalsIgnoreCase("null")) {
            try {
               key = InputConstants.getKey("key.keyboard." + stringKey.toLowerCase()).getValue();
            } catch (NumberFormatException e) {
               sendMessage("There is no such button");
               return 1;
            }
         } else {
            key = -1;
         }

         if (key == 0) {
            sendMessage("Unknown key '" + stringKey + "'!");
            return 1;
         } else {
            module.setBind(key, !stringKey.equals("M") && stringKey.contains("M"), false);
            sendMessage("Bind for " + ChatFormatting.GREEN + module.getName() + ChatFormatting.WHITE + " set to " + ChatFormatting.GRAY + stringKey.toUpperCase());
            return 1;
         }
      })));
      builder.then(
         literal("list")
            .executes(
               context -> {
                  StringBuilder binds = new StringBuilder("Binds: ");
                  int count = 0;

                  for (Module feature : Managers.MODULE.modules) {
                     if (KeyBinds.hasBind(feature)) {
                        binds.append("\n- ")
                           .append(feature.getName())
                           .append(" -> ")
                           .append(KeyBinds.getShortKeyName(feature))
                           .append(feature.getBind().isHold() ? " [hold]" : "");
                        count++;
                     }
                  }

                  if (count == 0) {
                     sendMessage("No binds set.");
                  } else {
                     sendMessage(binds.toString());
                  }
                  return 1;
               }
            )
      );
      builder.then(literal("clear").executes(context -> {
         for (Module mod : Managers.MODULE.modules) {
            mod.setBind(new Bind(-1, false, false));
         }

         return 1;
      }));
      builder.then(literal("reset").executes(context -> {
         for (Module mod : Managers.MODULE.modules) {
            mod.setBind(new Bind(-1, false, false));
         }

         sendMessage("Done!");
         return 1;
      }));
   }
}
