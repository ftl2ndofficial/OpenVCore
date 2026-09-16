package vcore.features.cmd.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import vcore.core.Managers;
import vcore.features.modules.Module;

public class ModuleArgumentType implements ArgumentType<Module> {
   private static final Collection<String> EXAMPLES = Managers.MODULE.modules.stream().map(Module::getName).limit(5L).toList();

   public static ModuleArgumentType create() {
      return new ModuleArgumentType();
   }

   public Module parse(StringReader reader) throws CommandSyntaxException {
      Module module = Managers.MODULE.get(reader.readString());
      if (module == null) {
         throw new DynamicCommandExceptionType(name -> Component.literal("Module " + name + " does not exist :(")).create(reader.readString());
      } else {
         return module;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggest(Managers.MODULE.modules.stream().map(Module::getName), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
