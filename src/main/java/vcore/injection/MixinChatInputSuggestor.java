package vcore.injection;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.CommandSuggestions.SuggestionsList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import vcore.core.Managers;
import vcore.features.modules.Module;

@Mixin(CommandSuggestions.class)
public abstract class MixinChatInputSuggestor {
   @Final
   @Shadow
   EditBox input;
   @Shadow
   boolean keepSuggestions;
   @Shadow
   private ParseResults<ClientSuggestionProvider> currentParse;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   private SuggestionsList suggestions;

   @Shadow
   protected abstract void updateUsageInfo();

   @Inject(
      method = "updateCommandInfo",
      at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;canRead()Z", remap = false),
      cancellable = true,
      locals = LocalCapture.CAPTURE_FAILHARD
   )
   @SuppressWarnings("unchecked")
   public void refreshHook(CallbackInfo ci, String string, StringReader reader) {
      if (!Module.fullNullCheck()) {
         if (reader.canRead(Managers.COMMAND.getPrefix().length()) && reader.getString().startsWith(Managers.COMMAND.getPrefix(), reader.getCursor())) {
            reader.setCursor(reader.getCursor() + 1);
            if (this.currentParse == null) {
               this.currentParse = (ParseResults<ClientSuggestionProvider>) (ParseResults<?>) Managers.COMMAND.getDispatcher().parse(reader, Managers.COMMAND.getSource());
            }

            int cursor = this.input.getCursorPosition();
            if (cursor >= 1 && (this.suggestions == null || !this.keepSuggestions)) {
               this.pendingSuggestions = (CompletableFuture<Suggestions>) (CompletableFuture<?>) Managers.COMMAND.getDispatcher().getCompletionSuggestions((ParseResults) this.currentParse, cursor);
               this.pendingSuggestions.thenRun(() -> {
                  if (this.pendingSuggestions.isDone()) {
                     this.updateUsageInfo();
                  }
               });
            }

            ci.cancel();
         }
      }
   }
}
