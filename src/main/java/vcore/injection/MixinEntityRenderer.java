package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {
   @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
   private void renderLabelIfPresent(
      T entity, Component text, PoseStack matrices, MultiBufferSource vertexConsumers, int light, float tickDelta, CallbackInfo info
   ) {
      if (entity instanceof ArmorStand && ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noArmorStands.getValue()) {
         info.cancel();
      }

      if (entity instanceof LivingEntity living && ModuleManager.esp.isEnabled() && ModuleManager.esp.shouldHideVanillaNameTag(living)) {
         info.cancel();
      }
   }
}
