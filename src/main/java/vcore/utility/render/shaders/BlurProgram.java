package vcore.utility.render.shaders;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL30;
import vcore.features.modules.Module;
import vcore.utility.render.WindowResizeCallback;
import vcore.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import vcore.utility.render.shaders.satin.api.managed.ShaderEffectManager;
import vcore.utility.render.shaders.satin.api.managed.uniform.SamplerUniform;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform1f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform2f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform4f;

public class BlurProgram {
   private Uniform2f uSize;
   private Uniform2f uLocation;
   private Uniform1f radius;
   private Uniform2f inputResolution;
   private Uniform1f opacity;
   private Uniform1f quality;
   private Uniform4f color1;
   private SamplerUniform sampler;
   private RenderTarget input;
   public static final ManagedCoreShader BLUR = ShaderEffectManager.getInstance().manageCoreShader(ResourceLocation.fromNamespaceAndPath("vcore", "blur"), DefaultVertexFormat.POSITION);

   public BlurProgram() {
      this.setup();
   }

   public void setParameters(float x, float y, float width, float height, float r, Color c1, float blurStrenth, float blurOpacity) {
      if (this.input == null) {
         this.input = new TextureTarget(
            Module.mc.getWindow().getGuiScaledWidth(), Module.mc.getWindow().getGuiScaledHeight(), false, Minecraft.ON_OSX
         );
      }

      float i = (float)Module.mc.getWindow().getGuiScale();
      this.radius.set(r * i);
      this.uLocation.set(x * i, -y * i + Module.mc.getWindow().getGuiScaledHeight() * i - height * i);
      this.uSize.set(width * i, height * i);
      this.opacity.set(blurOpacity);
      this.quality.set(blurStrenth);
      this.color1.set(c1.getRed() / 255.0F, c1.getGreen() / 255.0F, c1.getBlue() / 255.0F, 1.0F);
      this.sampler.set(this.input.getColorTextureId());
   }

   public void use() {
      RenderTarget buffer = Minecraft.getInstance().getMainRenderTarget();
      this.input.bindWrite(false);
      GL30.glBindFramebuffer(36008, buffer.frameBufferId);
      GL30.glBlitFramebuffer(0, 0, buffer.width, buffer.height, 0, 0, buffer.width, buffer.height, 16384, 9729);
      buffer.bindWrite(false);
      if (this.input != null
         && (this.input.width != Module.mc.getWindow().getWidth() || this.input.height != Module.mc.getWindow().getHeight())
         )
       {
         this.input.resize(Module.mc.getWindow().getWidth(), Module.mc.getWindow().getHeight(), Minecraft.ON_OSX);
      }

      this.inputResolution.set(buffer.width, buffer.height);
      this.sampler.set(this.input.getColorTextureId());
      RenderSystem.setShader(BLUR::getProgram);
   }

   protected void setup() {
      this.inputResolution = BLUR.findUniform2f("InputResolution");
      this.opacity = BLUR.findUniform1f("Opacity");
      this.quality = BLUR.findUniform1f("Quality");
      this.color1 = BLUR.findUniform4f("color1");
      this.uSize = BLUR.findUniform2f("uSize");
      this.uLocation = BLUR.findUniform2f("uLocation");
      this.radius = BLUR.findUniform1f("radius");
      this.sampler = BLUR.findSampler("InputSampler");
      WindowResizeCallback.EVENT.register((WindowResizeCallback)(client, window) -> {
         if (this.input != null) {
            this.input.resize(window.getWidth(), window.getHeight(), Minecraft.ON_OSX);
         }
      });
   }
}
