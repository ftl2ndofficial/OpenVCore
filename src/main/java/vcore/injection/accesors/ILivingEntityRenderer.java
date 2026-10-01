package vcore.injection.accesors;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface ILivingEntityRenderer {
   @Invoker("addLayer")
   <S extends LivingEntityRenderState, M extends EntityModel<? super S>> boolean vcore$addFeature(RenderLayer<S, M> var1);
}
