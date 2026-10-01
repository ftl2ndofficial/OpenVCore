package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.features.cmd.Command;

public class HelpCommand extends Command {
   public HelpCommand() {
      super("help", "List available commands.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.executes(
         context -> {
            sendMessage("Commands: \n");
            AtomicBoolean flip = new AtomicBoolean(false);
            Managers.COMMAND
               .getCommands()
               .forEach(
                  command -> {
                     mc.player
                        .displayClientMessage(
                           Component.nullToEmpty(
                              (flip.get() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE)
                                 + Managers.COMMAND.getPrefix()
                                 + (flip.get() ? ChatFormatting.AQUA : ChatFormatting.DARK_AQUA)
                                 + command.getName()
                                 + (command.getAliases().isEmpty() ? "" : " (" + command.getAliases() + ")")
                                 + ChatFormatting.DARK_GRAY
                                 + " -> "
                                 + (flip.get() ? ChatFormatting.WHITE : ChatFormatting.GRAY)
                                 + command.getDescription()
                           ),
                           false
                        );
                     flip.set(!flip.get());
                  }
               );
            return 1;
         }
      );
   }
}
