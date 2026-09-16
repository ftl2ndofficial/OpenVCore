package vcore.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(AgeableListModel.class)
public class MixinAnimalModel {
   @Inject(
      method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void renderCustomPlayerModel(PoseStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color, CallbackInfo ci) {
      if ((Object)this instanceof PlayerModel<?> playerModel) {
         if (ModuleManager.customModel != null && ModuleManager.customModel.isEnabled()) {
            AbstractClientPlayer player = ModuleManager.customModel.getRenderingPlayer();
            if (player != null) {
               if (ModuleManager.customModel.renderCustomPlayerModel(player, playerModel, matrices, vertexConsumer, light, overlay, color)) {
                  ci.cancel();
               }
            }
         }
      }
   }
}
