package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import vcore.features.cmd.Command;

public class TreasureCommand extends Command {
   public TreasureCommand() {
      super("gettreasure", "Read treasure map coordinates.");
   }

   @Override
   public void executeBuild(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.executes(context -> {
         if (mc.player.getMainHandItem().getItem().toString().equals("filled_map")) {
            Record nbt = (Record)mc.player.getMainHandItem().getOrDefault(DataComponents.MAP_DECORATIONS, PotionContents.EMPTY);
            if (nbt == null) {
               return 1;
            }

            StringBuilder result = new StringBuilder();
            String rawNbt = nbt.toString();

            for (int i = rawNbt.indexOf("x="); i < rawNbt.indexOf(", rotation") - 2; i++) {
               result.append(rawNbt.charAt(i));
            }

            sendMessage("Found! Coords: " + result);
         } else {
            sendMessage("Take a map into your hand!");
         }

         return 1;
      });
   }
}
