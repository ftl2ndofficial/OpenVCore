package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.features.cmd.Command;

public class DropAllCommand extends Command {
   public DropAllCommand() {
      super("dropall", "Drop all items from inventory.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(literal("legit").executes(context -> {
         Managers.ASYNC.run(() -> {
            for (int i = 5; i <= 45; i++) {
               mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, i, 1, ClickType.THROW, mc.player);

               try {
                  Thread.sleep(70L);
               } catch (InterruptedException e) {
                  throw new RuntimeException(e);
               }
            }

            mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
         }, 1L);
         sendMessage("ok");
         return 1;
      }));
      builder.executes(context -> {
         for (int i = 5; i <= 45; i++) {
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, i, 1, ClickType.THROW, mc.player);
         }

         mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
         sendMessage("ok");
         return 1;
      });
   }
}
