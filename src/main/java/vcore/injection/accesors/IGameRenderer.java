package vcore.injection.accesors;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface IGameRenderer {
   @Invoker("renderItemInHand")
   void irenderHand(Camera var1, float var2, Matrix4f var3);
}
