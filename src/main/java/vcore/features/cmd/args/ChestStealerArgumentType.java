package vcore.features.cmd.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ChestStealerArgumentType implements ArgumentType<String> {
   private static final List<String> EXAMPLES = getRegistered().stream().limit(5L).toList();

   public static ChestStealerArgumentType create() {
      return new ChestStealerArgumentType();
   }

   public String parse(@NotNull StringReader reader) throws CommandSyntaxException {
      String blockName = reader.readString();
      if (!getRegistered().contains(blockName)) {
         throw new DynamicCommandExceptionType(name -> Component.literal("There is no such item!")).create(blockName);
      } else {
         return blockName;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggest(getRegistered(), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   @NotNull
   private static List<String> getRegistered() {
      List<String> result = new ArrayList<>();

      for (Block block : BuiltInRegistries.BLOCK) {
         result.add(block.getDescriptionId().replace("block.minecraft.", ""));
      }

      for (Item item : BuiltInRegistries.ITEM) {
         result.add(item.getDescriptionId().replace("item.minecraft.", ""));
      }

      return result;
   }
}
