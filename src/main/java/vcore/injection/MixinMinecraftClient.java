package vcore.injection;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.server.packs.PackResources;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAttack;
import vcore.events.impl.EventHandleBlockBreaking;
import vcore.events.impl.EventItemUse;
import vcore.events.impl.EventPostTick;
import vcore.events.impl.EventScreen;
import vcore.events.impl.EventTick;
import vcore.features.modules.Module;
import vcore.features.modules.misc.UnHook;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.WindowResizeCallback;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {
   @Shadow
   @Final
   private Window window;
   @Unique
   private String[] shittyServers = new String[]{"mineblaze", "musteryworld", "dexland", "masedworld", "vimeworld", "hypemc", "vimemc"};
   @Unique
   private boolean wasInGame;

   @Inject(method = "<init>", at = @At("TAIL"))
   void postWindowInit(GameConfig args, CallbackInfo ci) {
      try {
         FontRenderers.settings = FontRenderers.create(12.0F, "comfortaa");
         FontRenderers.modules = FontRenderers.create(15.0F, "comfortaa");
         FontRenderers.categories = FontRenderers.create(18.0F, "comfortaa");
         FontRenderers.thglitch = FontRenderers.create(36.0F, "glitched");
         FontRenderers.thglitchBig = FontRenderers.create(72.0F, "glitched");
         FontRenderers.monsterrat = FontRenderers.create(18.0F, "monsterrat");
         FontRenderers.inter_target_name = FontRenderers.create(20.0F, "inter_semibold");
         FontRenderers.inter_target_hp = FontRenderers.create(15.0F, "inter_semibold");
         FontRenderers.sf_bold = FontRenderers.create(16.0F, "sf_bold");
         FontRenderers.sf_bold_large = FontRenderers.create(72.0F, "sf_bold");
         FontRenderers.sf_medium = FontRenderers.create(16.0F, "sf_medium");
         FontRenderers.sf_medium_mini = FontRenderers.create(12.0F, "sf_medium");
         FontRenderers.sf_medium_modules = FontRenderers.create(14.0F, "sf_medium");
         FontRenderers.sf_bold_mini = FontRenderers.create(14.0F, "sf_bold");
         FontRenderers.sf_bold_micro = FontRenderers.create(12.0F, "sf_bold");
         FontRenderers.profont = FontRenderers.create(16.0F, "profont");
         FontRenderers.icons = FontRenderers.create(20.0F, "icons");
         FontRenderers.mid_icons = FontRenderers.create(46.0F, "icons");
         FontRenderers.big_icons = FontRenderers.create(72.0F, "icons");
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   @Inject(method = "tick", at = @At("HEAD"))
   void preTickHook(CallbackInfo ci) {
      boolean inGame = Module.mc.player != null && Module.mc.level != null;
      if (this.wasInGame && !inGame) {
         Managers.MODULE.onLogout();
      }

      this.wasInGame = inGame;
      if (!Module.fullNullCheck()) {
         Vcore.EVENT_BUS.post(new EventTick());
      }
   }

   @Inject(method = "tick", at = @At("RETURN"))
   void postTickHook(CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         Vcore.EVENT_BUS.post(new EventPostTick());
      }
   }

   @Inject(method = "resizeDisplay", at = @At("TAIL"))
   private void captureResize(CallbackInfo ci) {
      ((WindowResizeCallback)WindowResizeCallback.EVENT.invoker()).onResized((Minecraft)(Object)this, this.window);
   }

   @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
   private void doItemPickHook(CallbackInfo ci) {
      if (ModuleManager.keyPearlAntiPickup) {
         ci.cancel();
      }
   }

   @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
   public void setScreenHookPre(@Nullable Screen screen, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventScreen event = new EventScreen(screen);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled() || ClickGUI.close && screen == null) {
            ci.cancel();
         }
      }
   }



   @Redirect(
      method = "<init>",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setIcon(Lnet/minecraft/server/packs/PackResources;Lcom/mojang/blaze3d/platform/IconSet;)V")
   )
   private void onChangeIcon(Window instance, PackResources resourcePack, IconSet icons) throws IOException {
      try (
         InputStream img16x16 = MixinMinecraftClient.class.getResourceAsStream("/icon.png");
         InputStream img32x32 = MixinMinecraftClient.class.getResourceAsStream("/icon.png");
      ) {
         if (img16x16 != null && img32x32 != null) {
            this.setWindowIcon(img16x16, img32x32);
            return;
         }
      }

      instance.setIcon(resourcePack, icons);
   }

   public void setWindowIcon(InputStream img16x16, InputStream img32x32) {
      try {
         MemoryStack memorystack = MemoryStack.stackPush();

         try {
            Buffer buffer = GLFWImage.malloc(2, memorystack);
            List<InputStream> imgList = List.of(img16x16, img32x32);
            List<ByteBuffer> buffers = new ArrayList<>();

            for (int i = 0; i < imgList.size(); i++) {
               NativeImage nativeImage = NativeImage.read(imgList.get(i));
               ByteBuffer bytebuffer = MemoryUtil.memAlloc(nativeImage.getWidth() * nativeImage.getHeight() * 4);
               bytebuffer.asIntBuffer().put(nativeImage.getPixelsABGR());
               buffer.position(i);
               buffer.width(nativeImage.getWidth());
               buffer.height(nativeImage.getHeight());
               buffer.pixels(bytebuffer);
               buffers.add(bytebuffer);
            }

            try {
               if (GLFW.glfwGetPlatform() != 393219) {
                  GLFW.glfwSetWindowIcon(this.window.handle(), buffer.position(0));
               }
            } catch (Exception var11) {
            }

            buffers.forEach(MemoryUtil::memFree);
         } catch (Throwable var12) {
            if (memorystack != null) {
               try {
                  memorystack.close();
               } catch (Throwable var10) {
                  var12.addSuppressed(var10);
               }
            }

            throw var12;
         }

         if (memorystack != null) {
            memorystack.close();
         }
      } catch (IOException var13) {
      }
   }

   @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
   private void doAttackHook(CallbackInfoReturnable<Boolean> cir) {
      EventAttack event = new EventAttack(null, true);
      Vcore.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
   private void doItemUseHook(CallbackInfo ci) {
      EventItemUse event = new EventItemUse();
      Vcore.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
   private void handleBlockBreakingHook(boolean breaking, CallbackInfo ci) {
      EventHandleBlockBreaking event = new EventHandleBlockBreaking();
      Vcore.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "updateTitle", at = @At("HEAD"), cancellable = true)
   private void overrideWindowTitle(CallbackInfo ci) {
      if (!UnHook.isActive()) {
         String title = "OpenVCore (1.21.11)";
         this.window.setTitle(title);
         ci.cancel();
      }
   }

   static {
      ClassLoader loader = MixinMinecraftClient.class.getClassLoader();
      String loaderName = loader != null ? loader.getClass().getName() : "null";
      if (!loaderName.contains("LaunchClassLoader") && !loaderName.contains("Mixin") && !loaderName.contains("fabric")) {
         throw new RuntimeException("[SECURITY] MixinMinecraftClient class loader is not allowed: " + loaderName);
      }
   }
}
