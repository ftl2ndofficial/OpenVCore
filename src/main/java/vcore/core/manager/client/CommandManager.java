package vcore.core.manager.client;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.IManager;
import vcore.features.cmd.Command;
import vcore.features.cmd.impl.BindCommand;
import vcore.features.cmd.impl.BlockESPCommand;
import vcore.features.cmd.impl.CfgCommand;
import vcore.features.cmd.impl.ChestStealerCommand;
import vcore.features.cmd.impl.DropAllCommand;
import vcore.features.cmd.impl.EClipCommand;
import vcore.features.cmd.impl.FriendCommand;
import vcore.features.cmd.impl.GarbageCleanerCommand;
import vcore.features.cmd.impl.GpsCommand;
import vcore.features.cmd.impl.HClipCommand;
import vcore.features.cmd.impl.HelpCommand;
import vcore.features.cmd.impl.HorseSpeedCommand;
import vcore.features.cmd.impl.InvCleanerCommand;
import vcore.features.cmd.impl.ItemHelperCommand;
import vcore.features.cmd.impl.LoginCommand;
import vcore.features.cmd.impl.MacroCommand;
import vcore.features.cmd.impl.ModuleCommand;
import vcore.features.cmd.impl.NukerCommand;
import vcore.features.cmd.impl.PrefixCommand;
import vcore.features.cmd.impl.StaffCommand;
import vcore.features.cmd.impl.TabParseCommand;
import vcore.features.cmd.impl.TreasureCommand;
import vcore.features.cmd.impl.VClipCommand;

import net.minecraft.server.permissions.PermissionSet;

public class CommandManager implements IManager {
   private String prefix = "@";
   private final CommandDispatcher<SharedSuggestionProvider> dispatcher = new CommandDispatcher();
   private final SharedSuggestionProvider source = new ClientSuggestionProvider(null, Minecraft.getInstance(), PermissionSet.ALL_PERMISSIONS);
   private final List<Command> commands = new ArrayList<>();

   public CommandManager() {
      this.add(new GpsCommand());
      this.add(new CfgCommand());
      this.add(new BindCommand());
      this.add(new HelpCommand());
      this.add(new NukerCommand());
      this.add(new EClipCommand());
      this.add(new HClipCommand());
      this.add(new LoginCommand());
      this.add(new MacroCommand());
      this.add(new StaffCommand());
      this.add(new VClipCommand());
      this.add(new FriendCommand());
      this.add(new ModuleCommand());
      this.add(new PrefixCommand());
      this.add(new DropAllCommand());
      this.add(new TreasureCommand());
      this.add(new TabParseCommand());
      this.add(new BlockESPCommand());
      this.add(new HorseSpeedCommand());
      this.add(new InvCleanerCommand());
      this.add(new ChestStealerCommand());
      this.add(new GarbageCleanerCommand());
      this.add(new ItemHelperCommand());
   }

   private void add(@NotNull Command command) {
      command.register(this.dispatcher);
      this.commands.add(command);
   }

   public String getPrefix() {
      return this.prefix;
   }

   public void setPrefix(String prefix) {
      this.prefix = prefix;
   }

   public Command get(Class<? extends Command> commandClass) {
      for (Command command : this.commands) {
         if (command.getClass().equals(commandClass)) {
            return command;
         }
      }

      return null;
   }

   @NotNull
   public static String getClientMessage() {
      return ChatFormatting.WHITE + "⌊" + ChatFormatting.GOLD + "⚡" + ChatFormatting.WHITE + "⌉" + ChatFormatting.RESET;
   }

   public List<Command> getCommands() {
      return this.commands;
   }

   public SharedSuggestionProvider getSource() {
      return this.source;
   }

   public CommandDispatcher<SharedSuggestionProvider> getDispatcher() {
      return this.dispatcher;
   }

   public void registerCommand(Command command) {
      if (command != null) {
         command.register(this.dispatcher);
         this.commands.add(command);
      }
   }
}
