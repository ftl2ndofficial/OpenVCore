package vcore.injection.accesors;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface ILivingEntityRenderer {
   @Invoker("addLayer")
   <T extends LivingEntity, M extends EntityModel<T>> boolean vcore$addFeature(RenderLayer<T, M> var1);
}
