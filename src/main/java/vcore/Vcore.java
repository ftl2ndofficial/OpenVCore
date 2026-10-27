package vcore;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Objects;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vcore.core.Core;
import vcore.core.Managers;
import vcore.core.hooks.ManagerShutdownHook;
import vcore.core.hooks.ModuleShutdownHook;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.popeffect.PopEffectParticles;
import vcore.utility.player.MovementUtility;
import vcore.utility.render.Render2DEngine;

public class Vcore implements ModInitializer {
   public static final ModMetadata MOD_META = FabricLoader.getInstance().getModContainer("vcore").map(ModContainer::getMetadata).orElse(null);
   public static final String RANK = "DEV";
   public static final String USER = "vu_2007";
   public static final String MOD_ID = "vcore";
   public static final String VERSION = MOD_META != null ? MOD_META.getVersion().getFriendlyString() : "1.0";
   public static final Logger LOGGER = LoggerFactory.getLogger("Vcore");
   public static final Runtime RUNTIME = Runtime.getRuntime();
   public static final boolean baritone = FabricLoader.getInstance().isModLoaded("baritone") || FabricLoader.getInstance().isModLoaded("baritone-meteor");
   public static final IEventBus EVENT_BUS = new EventBus();

   static {
      EVENT_BUS.registerLambdaFactory("vcore", (lookupInMethod, klass) -> (Lookup)lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
      EVENT_BUS.registerLambdaFactory("", (lookupInMethod, klass) -> (Lookup)lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
   }

   public static Color copy_color = new Color(-1);
   public static Vcore.KeyListening currentKeyListener;
   public static BlockPos gps_position;
   public static float TICK_TIMER = 1.0F;
   public static float FRAG_EFFECT_TIMER = 1.0F;
   public static Minecraft mc;
   public static long initTime;
   public static Core core = new Core();

   public void onInitialize() {
      mc = Minecraft.getInstance();
      initTime = System.currentTimeMillis();
      EVENT_BUS.registerLambdaFactory("vcore", (lookupInMethod, klass) -> (Lookup)lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
      EVENT_BUS.registerLambdaFactory("", (lookupInMethod, klass) -> (Lookup)lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
      EVENT_BUS.subscribe(core);
      this.preloadRuntimeClasses();
      PopEffectParticles.register();
      Managers.init();
      Managers.subscribe();
      Render2DEngine.initShaders();
      ModuleManager.rpc.startRpc();
      vcore.utility.network.OpenVCoreChannel.init();
      if (FabricLoader.getInstance().isModLoaded("sodium")) {
         LOGGER.warn("[Vcore] Sodium mod detected! ChunkAnimation module will not work with Sodium.");
      }
      LOGGER.info("[Vcore] Init time: {} ms.", System.currentTimeMillis() - initTime);
      initTime = System.currentTimeMillis();
      RUNTIME.addShutdownHook(new ManagerShutdownHook());
      RUNTIME.addShutdownHook(new ModuleShutdownHook());
   }

   private void preloadRuntimeClasses() {
      Objects.requireNonNull(MovementUtility.class.getClassLoader());
   }

   public static boolean isFuturePresent() {
      return FabricLoader.getInstance().getModContainer("future").isPresent();
   }

   public enum KeyListening {
      ClickGui,
      Search,
      Sliders,
      Strings;
   }
}
