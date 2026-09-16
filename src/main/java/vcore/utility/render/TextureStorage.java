package vcore.utility.render;

import net.minecraft.resources.ResourceLocation;

public final class TextureStorage {
   public static final ResourceLocation star = id("textures/effects/particle_star.png");
   public static final ResourceLocation particleBloom = id("textures/effects/particle_firefly.png");
   public static final ResourceLocation particleStar = star;
   public static final ResourceLocation particleSnowflake = id("textures/effects/particle_snowflake.png");
   public static final ResourceLocation particleHeart = id("textures/effects/particle_heart.png");
   public static final ResourceLocation particleGenshin = id("textures/effects/particle_genshin.png");
   public static final ResourceLocation particleRhombus = id("textures/effects/particle_rhombus.png");
   public static final ResourceLocation firefly = id("textures/effects/firefly.png");
   public static final ResourceLocation arrow = id("textures/effects/triangle.png");
   public static final ResourceLocation bubble = id("textures/effects/hit_bubble.png");
   public static final ResourceLocation defaultCircle = id("textures/effects/circle.png");
   public static final ResourceLocation container = id("textures/ui/container.png");
   public static final ResourceLocation guiArrow = id("textures/ui/arrow.png");
   public static final ResourceLocation setting = id("textures/ui/settings.png");
   public static final ResourceLocation brokenShield = id("textures/ui/broken_shield.png");
   public static final ResourceLocation miniLogo = id("textures/ui/mini_logo.png");
   public static final ResourceLocation playerIcon = id("textures/ui/player.png");
   public static final ResourceLocation speedometerIcon = id("textures/ui/speedometer.png");
   public static final ResourceLocation lagIcon = id("textures/ui/lag.png");
   public static final ResourceLocation fpsIcon = id("textures/ui/fps.png");
   public static final ResourceLocation pingIcon = id("textures/ui/ping.png");
   public static final ResourceLocation tpsIcon = id("textures/ui/tps.png");
   public static final ResourceLocation coordsIcon = id("textures/ui/coords.png");
   public static final ResourceLocation themeHudIcon = id("textures/ui/theme_hud.png");
   public static final ResourceLocation starCape = id("textures/cosmetics/cape.png");
   public static final ResourceLocation dashBloom = id("textures/effects/dashbloom.png");
   public static final ResourceLocation dashBloomSample = id("textures/effects/dashbloomsample.png");
   public static final ResourceLocation glow = id("textures/effects/glow.png");
   public static final ResourceLocation targetImage = id("textures/effects/target.png");

   private TextureStorage() {
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("vcore", path);
   }
}
