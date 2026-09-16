package vcore.injection;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.injection.accesors.IPostProcessShader;
import vcore.utility.interfaces.IShaderEffect;

@Mixin(PostChain.class)
public class MixinShaderEffect implements IShaderEffect {
   @Unique
   private final List<String> fakedBufferNames = new ArrayList<>();
   @Shadow
   @Final
   private Map<String, RenderTarget> customRenderTargets;
   @Shadow
   @Final
   private List<PostPass> passes;

   @Override
   public void addFakeTargetHook(String name, RenderTarget buffer) {
      RenderTarget previousFramebuffer = this.customRenderTargets.get(name);
      if (previousFramebuffer != buffer) {
         if (previousFramebuffer != null) {
            for (PostPass pass : this.passes) {
               if (pass.inTarget == previousFramebuffer) {
                  ((IPostProcessShader)pass).setInput(buffer);
               }

               if (pass.outTarget == previousFramebuffer) {
                  ((IPostProcessShader)pass).setOutput(buffer);
               }
            }

            this.customRenderTargets.remove(name);
            this.fakedBufferNames.remove(name);
         }

         this.customRenderTargets.put(name, buffer);
         this.fakedBufferNames.add(name);
      }
   }

   @Inject(method = "close", at = @At("HEAD"))
   void deleteFakeBuffersHook(CallbackInfo ci) {
      for (String fakedBufferName : this.fakedBufferNames) {
         this.customRenderTargets.remove(fakedBufferName);
      }
   }
}
