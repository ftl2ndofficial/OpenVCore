package vcore.injection.accesors;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IMinecraftClient {
   @Accessor("rightClickDelay")
   int getUseCooldown();

   @Accessor("rightClickDelay")
   void setUseCooldown(int var1);

   @Invoker("startUseItem")
   void idoItemUse();

   @Invoker("startAttack")
   boolean idoAttack();

   @Invoker("updateTitle")
   void invokeUpdateWindowTitle();

   @Mutable
   @Accessor("profileKeyPairManager")
   void setProfileKeys(ProfileKeyPairManager var1);

   @Mutable
   @Accessor("user")
   void setSessionT(User var1);

   @Mutable
   @Accessor
   void setUserApiService(UserApiService var1);

   @Mutable
   @Accessor("playerSocialManager")
   void setSocialInteractionsManagerT(PlayerSocialManager var1);

   @Mutable
   @Accessor("reportingContext")
   void setAbuseReportContextT(ReportingContext var1);
}
