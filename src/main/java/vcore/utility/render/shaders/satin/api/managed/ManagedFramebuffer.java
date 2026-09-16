package vcore.utility.render.shaders.satin.api.managed;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface ManagedFramebuffer {
   RenderTarget getFramebuffer();

   void beginWrite(boolean var1);

   void draw();

   void draw(int var1, int var2, boolean var3);

   void clear();

   void clear(boolean var1);
}
