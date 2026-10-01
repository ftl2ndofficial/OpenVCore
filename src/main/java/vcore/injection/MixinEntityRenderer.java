package vcore.injection;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {
   @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
   private void extractRenderStateHook(T entity, S state, float partialTicks, CallbackInfo info) {
      if (entity instanceof ArmorStand && ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noArmorStands.getValue()) {
         state.nameTag = null;
         return;
      }

      if (entity instanceof LivingEntity living && ModuleManager.esp.isEnabled() && ModuleManager.esp.shouldHideVanillaNameTag(living)) {
         state.nameTag = null;
      }
   }
}
