package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.animal.equine.Horse;
import org.jetbrains.annotations.NotNull;
import vcore.features.cmd.Command;

public class HorseSpeedCommand extends Command {
   public HorseSpeedCommand() {
      super("horsespeed", "Show current horse speed.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.executes(context -> {
         if (mc.player.getVehicle() != null && mc.player.getVehicle() instanceof Horse horse) {
            if (!horse.isSaddled()) {
               sendMessage(ChatFormatting.RED + "You don't have a saddle!");
               return 1;
            }

            float speed = horse.zza * 43.17F;
            float ratio = speed / 14.512F;
            String verbose = "";
            if (ratio < 0.3) {
               verbose = "Your horse is terrible :(";
            }

            if (ratio > 0.3 && ratio < 0.6) {
               verbose = "Your horse is normal";
            }

            if (ratio > 0.6) {
               verbose = "Your horse is good :)";
            }

            if (ratio > 0.9) {
               verbose = "Your horse is very good :)";
            }

            sendMessage(ChatFormatting.GREEN + "Horse speed: " + speed + " out of 14.512. " + verbose);
         } else {
            sendMessage(ChatFormatting.RED + "You don't have a horse!");
         }

         return 1;
      });
   }
}
