package vcore.features.cmd.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import vcore.core.Managers;
import vcore.features.modules.Module;

public class CategoryArgumentType implements ArgumentType<String> {
   private static final List<String> EXAMPLES = Managers.MODULE.getCategories().stream().map(Module.Category::getName).toList();

   public static CategoryArgumentType create() {
      return new CategoryArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      String cat = reader.readString();
      if (!EXAMPLES.contains(cat)) {
         throw new DynamicCommandExceptionType(name -> Component.literal("Category " + name + " does not exist :(")).create(cat);
      } else {
         return cat;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggest(EXAMPLES, builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
