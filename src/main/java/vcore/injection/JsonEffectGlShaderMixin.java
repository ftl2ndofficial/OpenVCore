package vcore.injection;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.shaders.Program.Type;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vcore.utility.render.shaders.satin.impl.SamplerAccess;

@Mixin(EffectInstance.class)
public abstract class JsonEffectGlShaderMixin implements SamplerAccess {
   @WrapOperation(
      at = @At(value = "INVOKE", target = "net/minecraft/util/Identifier.ofVanilla (Ljava/lang/String;)Lnet/minecraft/util/Identifier;", ordinal = 0),
      method = "<init>"
   )
   ResourceLocation constructProgramIdentifier(String arg, Operation<ResourceLocation> original, ResourceProvider unused, String id) {
      if (!id.contains(":")) {
         return (ResourceLocation)original.call(new Object[]{arg});
      }

      ResourceLocation split = ResourceLocation.parse(id);
      return ResourceLocation.fromNamespaceAndPath(split.getNamespace(), "shaders/program/" + split.getPath() + ".json");
   }

   @WrapOperation(
      at = @At(value = "INVOKE", target = "net/minecraft/util/Identifier.ofVanilla (Ljava/lang/String;)Lnet/minecraft/util/Identifier;", ordinal = 0),
      method = "getOrCreate"
   )
   private static ResourceLocation constructProgramIdentifier(String arg, Operation<ResourceLocation> original, ResourceProvider unused, Type shaderType, String id) {
      if (!arg.contains(":")) {
         return (ResourceLocation)original.call(new Object[]{arg});
      }

      ResourceLocation split = ResourceLocation.parse(id);
      return ResourceLocation.fromNamespaceAndPath(split.getNamespace(), "shaders/program/" + split.getPath() + shaderType.getExtension());
   }
}
