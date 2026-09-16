package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.ItemPhysics;

@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void onRender(ItemEntity entity, float f, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
      ItemPhysics itemPhysics = ModuleManager.itemPhysics;
      if (itemPhysics != null && itemPhysics.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         ItemStack stack = entity.getItem();
         BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, null, entity.getId());
         List<BakedQuad> quads = model.getQuads(null, null, mc.level.random);
         itemPhysics.onRenderItemEntity(entity, matrices, vertexConsumers, light, tickDelta, quads);
         ci.cancel();
      }
   }
}
