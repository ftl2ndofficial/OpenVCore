package vcore.injection;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.ClientAsset;
import net.minecraft.util.Util;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.features.modules.misc.UnHook;
import vcore.utility.render.TextureStorage;

@Mixin(PlayerInfo.class)
public class MixinPlayerListEntry {
   @Unique
   private boolean loadedCapeTexture;
   @Unique
   private Identifier customCapeTexture;
   @Unique
   private String playerName;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void initHook(GameProfile profile, boolean secureChatEnforced, CallbackInfo ci) {
      this.playerName = profile.name();
      this.getTexture(profile);
   }

   @Inject(method = "getSkin", at = @At("TAIL"), cancellable = true)
   private void getCapeTexture(CallbackInfoReturnable<PlayerSkin> cir) {
      if (UnHook.isActive()) {
         this.customCapeTexture = null;
      } else {
         this.ensureFriendCape();
         if (this.customCapeTexture != null) {
            PlayerSkin prev = cir.getReturnValue();
            if (prev != null) {
               ClientAsset.ResourceTexture cape = new ClientAsset.ResourceTexture(this.customCapeTexture, this.customCapeTexture);
               PlayerSkin newTextures = new PlayerSkin(
                  prev.body(), cape, cape, prev.model(), prev.secure()
               );
               cir.setReturnValue(newTextures);
            }
         }
      }
   }

   @Unique
   private void getTexture(GameProfile profile) {
      if (!this.loadedCapeTexture) {
         this.loadedCapeTexture = true;
         Util.backgroundExecutor().execute(() -> {
            String name = profile.name();
            if (this.isStarcapeTarget(name)) {
               this.customCapeTexture = TextureStorage.starCape;
            }
         });
      }
   }

   @Unique
   private void ensureFriendCape() {
      if (this.playerName != null && !this.playerName.isEmpty()) {
         if (this.isStarcapeTarget(this.playerName)) {
            this.customCapeTexture = TextureStorage.starCape;
         } else {
            if (this.customCapeTexture == TextureStorage.starCape) {
               this.customCapeTexture = null;
            }
         }
      }
   }

   private boolean isStarcapeTarget(String name) {
      if (name != null && !name.isEmpty()) {
         if (Vcore.mc.player != null) {
            String selfName = Vcore.mc.player.getName().getString();
            if (selfName.equalsIgnoreCase(name)) {
               return true;
            }
         }

         return Managers.FRIEND.isFriend(name);
      } else {
         return false;
      }
   }
}
