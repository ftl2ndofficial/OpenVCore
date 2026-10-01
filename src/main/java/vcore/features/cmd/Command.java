package vcore.features.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public abstract class Command {
   protected static final CommandBuildContext REGISTRY_ACCESS = Commands.createValidationContext(VanillaRegistries.createLookup());
   protected static final Minecraft mc = Minecraft.getInstance();
   protected final List<String> names;
   private final String description;

   public Command(String name) {
      this(List.of(name), "No description provided.");
   }

   public Command(String name, String description, String... aliases) {
      this(resolveNames(name, aliases), description);
   }

   private Command(List<String> names, String description) {
      this.names = names;
      this.description = description;
   }

   private static List<String> resolveNames(String name, String... aliases) {
      List<String> resolvedNames = new ArrayList<>();
      resolvedNames.add(name);
      resolvedNames.addAll(Arrays.asList(aliases));
      return List.copyOf(resolvedNames);
   }

   public abstract void executeBuild(LiteralArgumentBuilder<SharedSuggestionProvider> var1);

   public static void sendMessage(String message) {
      if (mc.player != null) {
         mc.player.displayClientMessage(Component.nullToEmpty(vcore.core.manager.client.CommandManager.getClientMessage() + " " + message), false);
      }
   }

   @NotNull
   protected static <T> RequiredArgumentBuilder<SharedSuggestionProvider, T> arg(String name, ArgumentType<T> type) {
      return RequiredArgumentBuilder.argument(name, type);
   }

   @NotNull
   protected static LiteralArgumentBuilder<SharedSuggestionProvider> literal(String name) {
      return LiteralArgumentBuilder.literal(name);
   }

   public void register(CommandDispatcher<SharedSuggestionProvider> dispatcher) {
      for (String name : this.names) {
         LiteralArgumentBuilder<SharedSuggestionProvider> builder = LiteralArgumentBuilder.literal(name);
         this.executeBuild(builder);
         dispatcher.register(builder);
      }
   }

   public String getName() {
      return this.names.get(0);
   }

   public String getAliases() {
      return String.join(", ", this.names.stream().filter(n -> !n.equals(this.names.get(0))).toList());
   }

   public String getDescription() {
      return this.description;
   }
}
