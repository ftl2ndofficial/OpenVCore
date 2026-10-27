package vcore.gui.font;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.awt.Color;
import java.awt.Font;
import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import vcore.core.manager.IManager;
import vcore.features.modules.render.HudEditor;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.RenderSystem;

public class FontRenderer implements Closeable {
   private static final Char2IntArrayMap colorCodes = new Char2IntArrayMap() {
      {
         this.put('0', 0);
         this.put('1', 170);
         this.put('2', 43520);
         this.put('3', 43690);
         this.put('4', 11141120);
         this.put('5', 11141290);
         this.put('6', 16755200);
         this.put('7', 11184810);
         this.put('8', 5592405);
         this.put('9', 5592575);
         this.put('A', 5635925);
         this.put('B', 5636095);
         this.put('C', 16733525);
         this.put('D', 16733695);
         this.put('E', 16777045);
         this.put('F', 16777215);
      }
   };
   private static final ExecutorService ASYNC_WORKER = Executors.newCachedThreadPool();
   private final Object2ObjectOpenHashMap<Identifier, ObjectList<FontRenderer.DrawEntry>> GLYPH_PAGE_CACHE = new Object2ObjectOpenHashMap<>();
   private final float originalSize;
   private final ObjectList<GlyphMap> maps = new ObjectArrayList<>();
   private final Char2ObjectOpenHashMap<Glyph> allGlyphs = new Char2ObjectOpenHashMap<>();
   private final Glyph[] asciiGlyphs = new Glyph[256];
   private final Object2FloatOpenHashMap<String> widthCache = new Object2FloatOpenHashMap<>();
   private final int charsPerPage;
   private final int padding;
   private final String prebakeGlyphs;
   private int scaleMul = 0;
   private Font font;
   private int previousGameScale = -1;
   private Future<Void> prebakeGlyphsFuture;
   private boolean initialized;

   public FontRenderer(Font font, float sizePx, int charactersPerPage, int paddingBetweenCharacters, @Nullable String prebakeCharacters) {
      this.originalSize = sizePx;
      this.charsPerPage = charactersPerPage;
      this.padding = paddingBetweenCharacters;
      this.prebakeGlyphs = prebakeCharacters;
      this.widthCache.defaultReturnValue(-1.0F);
      this.init(font, sizePx);
   }

   public FontRenderer(Font font, float sizePx) {
      this(font, sizePx, 256, 5, null);
   }

   private static int floorNearestMulN(int x, int n) {
      return n * (int)Math.floor((double)x / n);
   }

   public static String stripControlCodes(String text) {
      if (text == null || text.isEmpty()) {
         return "";
      }
      if (text.indexOf(167) == -1) {
         return text;
      }

      int len = text.length();
      StringBuilder f = new StringBuilder(len);

      for (int i = 0; i < len; i++) {
         char c = text.charAt(i);
         if (c == 167) {
            i++;
         } else {
            f.append(c);
         }
      }

      return f.toString();
   }

   private void sizeCheck() {
      int gs = (int)IManager.mc.getWindow().getGuiScale();
      if (gs != this.previousGameScale) {
         this.close();
         this.init(this.font, this.originalSize);
      }
   }

   private void init(Font font, float sizePx) {
      if (this.initialized) {
         throw new IllegalStateException("Double call to init()");
      }

      this.initialized = true;
      this.previousGameScale = (int)IManager.mc.getWindow().getGuiScale();
      this.scaleMul = Math.max(2, this.previousGameScale);
      this.font = font.deriveFont(sizePx * this.scaleMul);
      if (this.prebakeGlyphs != null && !this.prebakeGlyphs.isEmpty()) {
         this.prebakeGlyphsFuture = this.prebake();
      }
   }

   private Future<Void> prebake() {
      return ASYNC_WORKER.submit(() -> {
         for (char c : this.prebakeGlyphs.toCharArray()) {
            if (Thread.interrupted()) {
               break;
            }

            this.locateGlyph1(c);
         }

         return null;
      });
   }

   private GlyphMap generateMap(char from, char to) {
      GlyphMap gm = new GlyphMap(from, to, this.font, randomIdentifier(), this.padding);
      this.maps.add(gm);
      return gm;
   }

   private Glyph locateGlyph0(char glyph) {
      for (int i = 0, n = this.maps.size(); i < n; i++) {
         GlyphMap map = this.maps.get(i);
         if (map.contains(glyph)) {
            return map.getGlyph(glyph);
         }
      }

      int basex = floorNearestMulN(glyph, this.charsPerPage);
      GlyphMap glyphMap = this.generateMap((char)basex, (char)(basex + this.charsPerPage));
      return glyphMap.getGlyph(glyph);
   }

   @Nullable
   private Glyph locateGlyph1(char glyph) {
      if (glyph < 256) {
         Glyph g = this.asciiGlyphs[glyph];
         if (g != null) {
            return g;
         }
         g = this.allGlyphs.computeIfAbsent(glyph, this::locateGlyph0);
         this.asciiGlyphs[glyph] = g;
         return g;
      }
      return this.allGlyphs.computeIfAbsent(glyph, this::locateGlyph0);
   }

   public void drawString(PoseStack stack, String s, double x, double y, int color) {
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float a = (color >> 24 & 0xFF) / 255.0F;
      this.drawString(stack, s, (float)x, (float)y, r, g, b, a);
   }

   public void drawString(PoseStack stack, String s, double x, double y, Color color) {
      this.drawString(stack, s, (float)x, (float)y, color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha());
   }

   public void drawString(PoseStack stack, String s, float x, float y, float r, float g, float b, float a) {
      this.drawString(stack, s, x, y, r, g, b, a, false, 0);
   }

   public void drawStringWithHorizontalFade(PoseStack stack, String s, float x, float y, int color, float visibleWidth, float fadeWidth) {
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float a = (color >> 24 & 0xFF) / 255.0F;
      this.drawStringWithHorizontalFade(stack, s, x, y, r, g, b, a, visibleWidth, fadeWidth);
   }

   public void drawStringWithHorizontalFade(
      PoseStack stack, String s, float x, float y, float r, float g, float b, float a, float visibleWidth, float fadeWidth
   ) {
      if (s == null || s.isEmpty()) {
         return;
      }
      if (this.prebakeGlyphsFuture != null && !this.prebakeGlyphsFuture.isDone()) {
         try {
            this.prebakeGlyphsFuture.get();
         } catch (InterruptedException | ExecutionException var46) {
         }
      }

      this.sizeCheck();
      float scaledVisibleWidth = Math.max(0.0F, visibleWidth) * this.scaleMul;
      float scaledFadeWidth = Math.max(0.0F, Math.min(fadeWidth, visibleWidth)) * this.scaleMul;
      float fadeStart = Math.max(0.0F, scaledVisibleWidth - scaledFadeWidth);
      float r2 = r;
      float g2 = g;
      float b2 = b;
      stack.pushPose();
      y -= 3.0F;
      stack.translate(MathUtility.roundToDecimal(x, 1), MathUtility.roundToDecimal(y, 1), 0.0);
      stack.scale(1.0F / this.scaleMul, 1.0F / this.scaleMul, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      Matrix4f mat = stack.last().pose();
      int len = s.length();
      float xOffset = 0.0F;
      float yOffset = 0.0F;
      boolean inSel = false;
      int lineStart = 0;
      synchronized (this.GLYPH_PAGE_CACHE) {
         for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (inSel) {
               inSel = false;
               char c1 = Character.toUpperCase(c);
               if (colorCodes.containsKey(c1)) {
                  int ii = colorCodes.get(c1);
                  r2 = (ii >> 16 & 0xFF) / 255.0F;
                  g2 = (ii >> 8 & 0xFF) / 255.0F;
                  b2 = (ii & 0xFF) / 255.0F;
               } else if (c1 == 'R') {
                  r2 = r;
                  g2 = g;
                  b2 = b;
               }
            } else if (c == 167) {
               inSel = true;
            } else if (c == '\n') {
               yOffset += this.getStringHeight(s.substring(lineStart, i)) * this.scaleMul;
               xOffset = 0.0F;
               lineStart = i + 1;
            } else {
               Glyph glyph = this.locateGlyph1(c);
               if (glyph != null) {
                  if (glyph.value() != ' ') {
                     Identifier i1 = glyph.owner().bindToTexture;
                     FontRenderer.DrawEntry entry = new FontRenderer.DrawEntry(xOffset, yOffset, r2, g2, b2, glyph);
                     this.GLYPH_PAGE_CACHE.computeIfAbsent(i1, integer -> new ObjectArrayList<>()).add(entry);
                  }

                  xOffset += glyph.width();
               }
            }
         }

         ObjectIterator<Object2ObjectMap.Entry<Identifier, ObjectList<FontRenderer.DrawEntry>>> iter = this.GLYPH_PAGE_CACHE.object2ObjectEntrySet().fastIterator();

         while (iter.hasNext()) {
            Object2ObjectMap.Entry<Identifier, ObjectList<FontRenderer.DrawEntry>> pageEntry = iter.next();
            List<FontRenderer.DrawEntry> objects = pageEntry.getValue();
            if (objects.isEmpty()) {
               continue;
            }
            Identifier identifier = pageEntry.getKey();
            RenderSystem.setShaderTexture(0, identifier);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            boolean hasVertices = false;

            for (int j = 0, n = objects.size(); j < n; j++) {
               FontRenderer.DrawEntry object = objects.get(j);
               float xo = object.atX;
               float yo = object.atY;
               float cr = object.r;
               float cg = object.g;
               float cb = object.b;
               Glyph glyph = object.toDraw;
               GlyphMap owner = glyph.owner();
               float w = glyph.width();
               float h = glyph.height();
               float u1 = (float)glyph.u() / owner.width;
               float v1 = (float)glyph.v() / owner.height;
               float u2 = (float)(glyph.u() + glyph.width()) / owner.width;
               float v2 = (float)(glyph.v() + glyph.height()) / owner.height;
               float leftAlpha = this.computeHorizontalFadeAlpha(xo, scaledVisibleWidth, fadeStart, a);
               float rightAlpha = this.computeHorizontalFadeAlpha(xo + w, scaledVisibleWidth, fadeStart, a);
               if (!(leftAlpha <= 0.0F) || !(rightAlpha <= 0.0F)) {
                  hasVertices = true;
                  bb.addVertex(mat, xo, yo + h, 0.0F).setUv(u1, v2).setColor(cr, cg, cb, leftAlpha);
                  bb.addVertex(mat, xo + w, yo + h, 0.0F).setUv(u2, v2).setColor(cr, cg, cb, rightAlpha);
                  bb.addVertex(mat, xo + w, yo, 0.0F).setUv(u2, v1).setColor(cr, cg, cb, rightAlpha);
                  bb.addVertex(mat, xo, yo, 0.0F).setUv(u1, v1).setColor(cr, cg, cb, leftAlpha);
               }
            }

            if (hasVertices) {
               Render2DEngine.endBuilding(bb);
            } else {
               try {
                  bb.build().close();
               } catch (Throwable ignored) {
               }
            }
            objects.clear();
         }
      }

      stack.popPose();
   }

   public void drawString(PoseStack stack, String s, float x, float y, float r, float g, float b, float a, boolean gradient, int offset) {
      if (s == null || s.isEmpty()) {
         return;
      }
      if (this.prebakeGlyphsFuture != null && !this.prebakeGlyphsFuture.isDone()) {
         try {
            this.prebakeGlyphsFuture.get();
         } catch (InterruptedException | ExecutionException var41) {
         }
      }

      this.sizeCheck();
      float r2 = r;
      float g2 = g;
      float b2 = b;
      stack.pushPose();
      y -= 3.0F;
      stack.translate(MathUtility.roundToDecimal(x, 1), MathUtility.roundToDecimal(y, 1), 0.0);
      stack.scale(1.0F / this.scaleMul, 1.0F / this.scaleMul, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      Matrix4f mat = stack.last().pose();
      int len = s.length();
      float xOffset = 0.0F;
      float yOffset = 0.0F;
      boolean inSel = false;
      int lineStart = 0;
      synchronized (this.GLYPH_PAGE_CACHE) {
         for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (inSel) {
               inSel = false;
               char c1 = Character.toUpperCase(c);
               if (colorCodes.containsKey(c1)) {
                  int ii = colorCodes.get(c1);
                  r2 = (ii >> 16 & 0xFF) / 255.0F;
                  g2 = (ii >> 8 & 0xFF) / 255.0F;
                  b2 = (ii & 0xFF) / 255.0F;
               } else if (c1 == 'R') {
                  r2 = r;
                  g2 = g;
                  b2 = b;
               }
            } else {
               if (gradient) {
                  Color color = HudEditor.getColor(i * offset);
                  r2 = color.getRed() / 255.0F;
                  g2 = color.getGreen() / 255.0F;
                  b2 = color.getBlue() / 255.0F;
                  a = color.getAlpha() / 255.0F;
               }

               if (c == 167) {
                  inSel = true;
               } else if (c == '\n') {
                  yOffset += this.getStringHeight(s.substring(lineStart, i)) * this.scaleMul;
                  xOffset = 0.0F;
                  lineStart = i + 1;
               } else {
                  Glyph glyph = this.locateGlyph1(c);
                  if (glyph != null) {
                     if (glyph.value() != ' ') {
                        Identifier i1 = glyph.owner().bindToTexture;
                        FontRenderer.DrawEntry entry = new FontRenderer.DrawEntry(xOffset, yOffset, r2, g2, b2, glyph);
                        this.GLYPH_PAGE_CACHE.computeIfAbsent(i1, integer -> new ObjectArrayList<>()).add(entry);
                     }

                     xOffset += glyph.width();
                  }
               }
            }
         }

         ObjectIterator<Object2ObjectMap.Entry<Identifier, ObjectList<FontRenderer.DrawEntry>>> iter = this.GLYPH_PAGE_CACHE.object2ObjectEntrySet().fastIterator();

         while (iter.hasNext()) {
            Object2ObjectMap.Entry<Identifier, ObjectList<FontRenderer.DrawEntry>> pageEntry = iter.next();
            List<FontRenderer.DrawEntry> objects = pageEntry.getValue();
            if (objects.isEmpty()) {
               continue;
            }
            Identifier identifier = pageEntry.getKey();
            RenderSystem.setShaderTexture(0, identifier);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            for (int j = 0, n = objects.size(); j < n; j++) {
               FontRenderer.DrawEntry object = objects.get(j);
               float xo = object.atX;
               float yo = object.atY;
               float cr = object.r;
               float cg = object.g;
               float cb = object.b;
               Glyph glyph = object.toDraw;
               GlyphMap owner = glyph.owner();
               float w = glyph.width();
               float h = glyph.height();
               float u1 = (float)glyph.u() / owner.width;
               float v1 = (float)glyph.v() / owner.height;
               float u2 = (float)(glyph.u() + glyph.width()) / owner.width;
               float v2 = (float)(glyph.v() + glyph.height()) / owner.height;
               bb.addVertex(mat, xo + 0.0F, yo + h, 0.0F).setUv(u1, v2).setColor(cr, cg, cb, a);
               bb.addVertex(mat, xo + w, yo + h, 0.0F).setUv(u2, v2).setColor(cr, cg, cb, a);
               bb.addVertex(mat, xo + w, yo + 0.0F, 0.0F).setUv(u2, v1).setColor(cr, cg, cb, a);
               bb.addVertex(mat, xo + 0.0F, yo + 0.0F, 0.0F).setUv(u1, v1).setColor(cr, cg, cb, a);
            }

            Render2DEngine.endBuilding(bb);
            objects.clear();
         }
      }

      stack.popPose();
   }

   public void drawCenteredString(PoseStack stack, String s, double x, double y, int color) {
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float a = (color >> 24 & 0xFF) / 255.0F;
      this.drawString(stack, s, (float)(x - this.getStringWidth(s) / 2.0F), (float)y, r, g, b, a);
   }

   public void drawCenteredString(PoseStack stack, String s, double x, double y, Color color) {
      this.drawString(
         stack,
         s,
         (float)(x - this.getStringWidth(s) / 2.0F),
         (float)y,
         color.getRed() / 255.0F,
         color.getGreen() / 255.0F,
         color.getBlue() / 255.0F,
         color.getAlpha() / 255.0F
      );
   }

   public void drawCenteredString(PoseStack stack, String s, float x, float y, float r, float g, float b, float a) {
      this.drawString(stack, s, x - this.getStringWidth(s) / 2.0F, y, r, g, b, a);
   }

   public float getStringWidth(String text) {
      if (text == null || text.isEmpty()) {
         return 0.0F;
      }
      this.sizeCheck();
      float cached = this.widthCache.getFloat(text);
      if (cached >= 0.0F) {
         return cached;
      }

      String stripped = stripControlCodes(text);
      float currentLine = 0.0F;
      float maxPreviousLines = 0.0F;

      for (int i = 0, len = stripped.length(); i < len; i++) {
         char c1 = stripped.charAt(i);
         if (c1 == '\n') {
            maxPreviousLines = Math.max(currentLine, maxPreviousLines);
            currentLine = 0.0F;
         } else {
            Glyph glyph = this.locateGlyph1(c1);
            currentLine += glyph == null ? 0.0F : (float)glyph.width() / this.scaleMul;
         }
      }

      float result = Math.max(currentLine, maxPreviousLines);
      if (this.widthCache.size() >= 4096) {
         this.widthCache.clear();
      }
      this.widthCache.put(text, result);
      return result;
   }

   public float getStringHeight(String text) {
      this.sizeCheck();
      String stripped = stripControlCodes(text);
      if (stripped.isEmpty()) {
         Glyph space = this.locateGlyph1(' ');
         return space == null ? 0.0F : (float)space.height() / this.scaleMul;
      }

      float currentLine = 0.0F;
      float previous = 0.0F;

      for (int i = 0, len = stripped.length(); i < len; i++) {
         char c1 = stripped.charAt(i);
         if (c1 == '\n') {
            if (currentLine == 0.0F) {
               Glyph space = this.locateGlyph1(' ');
               currentLine = space == null ? 0.0F : (float)space.height() / this.scaleMul;
            }

            previous += currentLine;
            currentLine = 0.0F;
         } else {
            Glyph glyph = this.locateGlyph1(c1);
            currentLine = Math.max(glyph == null ? 0.0F : (float)glyph.height() / this.scaleMul, currentLine);
         }
      }

      return currentLine + previous;
   }

   private float computeHorizontalFadeAlpha(float x, float visibleWidth, float fadeStart, float baseAlpha) {
      if (x <= fadeStart) {
         return baseAlpha;
      }

      if (x >= visibleWidth) {
         return 0.0F;
      }

      if (visibleWidth <= fadeStart) {
         return 0.0F;
      }

      float progress = (x - fadeStart) / (visibleWidth - fadeStart);
      progress = Math.max(0.0F, Math.min(1.0F, progress));
      return baseAlpha * (float)Math.pow(1.0F - progress, 2.35F);
   }

   @Override
   public void close() {
      try {
         if (this.prebakeGlyphsFuture != null && !this.prebakeGlyphsFuture.isDone() && !this.prebakeGlyphsFuture.isCancelled()) {
            this.prebakeGlyphsFuture.cancel(true);
            this.prebakeGlyphsFuture.get();
            this.prebakeGlyphsFuture = null;
         }

         ObjectListIterator<GlyphMap> var1 = this.maps.iterator();

         while (var1.hasNext()) {
            GlyphMap map = var1.next();
            map.destroy();
         }

         this.maps.clear();
         this.allGlyphs.clear();
         Arrays.fill(this.asciiGlyphs, null);
         this.widthCache.clear();
         this.GLYPH_PAGE_CACHE.clear();
         this.initialized = false;
      } catch (Exception var3) {
      }
   }

   @Contract(value = "-> new", pure = true)
   @NotNull
   public static Identifier randomIdentifier() {
      return Identifier.fromNamespaceAndPath("vcore", "temp/" + randomString());
   }

   private static String randomString() {
      return IntStream.range(0, 32).mapToObj(operand -> String.valueOf((char)new Random().nextInt(97, 123))).collect(Collectors.joining());
   }

   @Contract(value = "_ -> new", pure = true)
   public static int @NotNull [] RGBIntToRGB(int in) {
      int red = in >> 16 & 0xFF;
      int green = in >> 8 & 0xFF;
      int blue = in & 0xFF;
      return new int[]{red, green, blue};
   }

   public float getFontHeight(String str) {
      return this.getStringHeight(str);
   }

   public void drawGradientString(PoseStack stack, String s, float x, float y, int offset) {
      this.drawString(stack, s, x, y, 255.0F, 255.0F, 255.0F, 255.0F, true, offset);
   }

   public void drawGradientCenteredString(PoseStack matrices, String s, float x, float y, int i) {
      this.drawGradientString(matrices, s, x - this.getStringWidth(s) / 2.0F, y, i);
   }

   public void drawString(org.joml.Matrix3x2fStack stack, String s, double x, double y, int color) {
      this.drawString(vcore.utility.render.Render2DEngine.toPoseStack(stack), s, x, y, color);
   }

   public void drawString(org.joml.Matrix3x2fStack stack, String s, double x, double y, Color color) {
      this.drawString(vcore.utility.render.Render2DEngine.toPoseStack(stack), s, x, y, color);
   }

   public void drawString(org.joml.Matrix3x2fStack stack, String s, float x, float y, float r, float g, float b, float a) {
      this.drawString(vcore.utility.render.Render2DEngine.toPoseStack(stack), s, x, y, r, g, b, a);
   }

   public void drawCenteredString(org.joml.Matrix3x2fStack stack, String s, double x, double y, int color) {
      this.drawCenteredString(vcore.utility.render.Render2DEngine.toPoseStack(stack), s, x, y, color);
   }

   public void drawCenteredString(org.joml.Matrix3x2fStack stack, String s, double x, double y, Color color) {
      this.drawCenteredString(vcore.utility.render.Render2DEngine.toPoseStack(stack), s, x, y, color);
   }

   public void drawCenteredString(org.joml.Matrix3x2fStack stack, String s, float x, float y, float r, float g, float b, float a) {
      this.drawCenteredString(vcore.utility.render.Render2DEngine.toPoseStack(stack), s, x, y, r, g, b, a);
   }

   public void drawGradientString(org.joml.Matrix3x2fStack stack, String s, float x, float y, int offset) {
      this.drawGradientString(vcore.utility.render.Render2DEngine.toPoseStack(stack), s, x, y, offset);
   }

   public void drawGradientCenteredString(org.joml.Matrix3x2fStack stack, String s, float x, float y, int i) {
      this.drawGradientCenteredString(vcore.utility.render.Render2DEngine.toPoseStack(stack), s, x, y, i);
   }

   public void drawStringWithHorizontalFade(org.joml.Matrix3x2fStack stack, String s, float x, float y, int color, float visibleWidth, float fadeWidth) {
      this.drawStringWithHorizontalFade(vcore.utility.render.Render2DEngine.toPoseStack(stack), s, x, y, color, visibleWidth, fadeWidth);
   }

   public void drawString(org.joml.Matrix3x2f stack, String s, double x, double y, int color) {
      this.drawString(vcore.utility.render.Render2DEngine.toPoseStack(stack), s, x, y, color);
   }

   public void drawCenteredString(org.joml.Matrix3x2f stack, String s, double x, double y, int color) {
      this.drawCenteredString(vcore.utility.render.Render2DEngine.toPoseStack(stack), s, x, y, color);
   }

   record DrawEntry(float atX, float atY, float r, float g, float b, Glyph toDraw) {
   }
}
