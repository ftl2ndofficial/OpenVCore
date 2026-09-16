package vcore.gui.font;

import net.minecraft.resources.ResourceLocation;

public class Texture {
   final ResourceLocation id;

   public Texture(String path) {
      this.id = ResourceLocation.fromNamespaceAndPath("vcore", this.validatePath(path));
   }

   public Texture(ResourceLocation i) {
      this.id = ResourceLocation.fromNamespaceAndPath(i.getNamespace(), i.getPath());
   }

   String validatePath(String path) {
      if (ResourceLocation.isValidPath(path)) {
         return path;
      }

      StringBuilder ret = new StringBuilder();

      for (char c : path.toLowerCase().toCharArray()) {
         if (ResourceLocation.validPathChar(c)) {
            ret.append(c);
         }
      }

      return ret.toString();
   }

   public ResourceLocation getId() {
      return this.id;
   }
}
