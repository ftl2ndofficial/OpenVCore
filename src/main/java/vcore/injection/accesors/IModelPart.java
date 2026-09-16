package vcore.injection.accesors;

import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.class)
public interface IModelPart {
   @Accessor("cubes")
   List<Cube> vcore$getCuboids();
}
