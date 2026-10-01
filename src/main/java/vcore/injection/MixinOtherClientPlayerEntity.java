package vcore.injection;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.misc.FakePlayer;
import vcore.utility.interfaces.IEntityLiving;
import vcore.utility.interfaces.IOtherClientPlayerEntity;

@Mixin(RemotePlayer.class)
public class MixinOtherClientPlayerEntity extends AbstractClientPlayer implements IOtherClientPlayerEntity {
   @Unique
   private double backUpX;
   @Unique
   private double backUpY;
   @Unique
   private double backUpZ;

   public MixinOtherClientPlayerEntity(ClientLevel world, GameProfile profile) {
      super(world, profile);
   }

   @Override
   public void resolve(Aura.Resolver mode) {
      if ((Object)this == FakePlayer.fakePlayer) {
         this.backUpY = -999.0;
      } else {
         this.backUpX = this.getX();
         this.backUpY = this.getY();
         this.backUpZ = this.getZ();
         if (mode == Aura.Resolver.BackTrack) {
            double minDst = 999.0;
            Aura.Position bestPos = null;

            for (Aura.Position p : ((IEntityLiving)(Object)this).getPositionHistory()) {
               double dst = Module.mc.player.distanceToSqr(p.getX(), p.getY(), p.getZ());
               if (dst < minDst) {
                  minDst = dst;
                  bestPos = p;
               }
            }

            if (bestPos != null) {
               this.setPos(bestPos.getX(), bestPos.getY(), bestPos.getZ());
               if ((Object)Aura.target == (Object)this) {
                  ModuleManager.aura.resolvedBox = this.getBoundingBox();
               }
            }
         } else {
            Vec3 from = new Vec3(((IEntityLiving)(Object)this).getPrevServerX(), ((IEntityLiving)(Object)this).getPrevServerY(), ((IEntityLiving)(Object)this).getPrevServerZ());
            Vec3 to = this.getInterpolation() != null ? this.getInterpolation().position() : this.position();
            if (mode == Aura.Resolver.Advantage) {
               if (Module.mc.player.distanceToSqr(from) > Module.mc.player.distanceToSqr(to)) {
                  this.setPos(to.x, to.y, to.z);
               } else {
                  this.setPos(from.x, from.y, from.z);
               }
            }

            if ((Object)Aura.target == (Object)this) {
               ModuleManager.aura.resolvedBox = this.getBoundingBox();
            }
         }
      }
   }

   @Override
   public void releaseResolver() {
      if (this.backUpY != -999.0) {
         this.setPos(this.backUpX, this.backUpY, this.backUpZ);
         this.backUpY = -999.0;
      }
   }
}
