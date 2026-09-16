package vcore.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import vcore.events.Event;

public class EventHeldItemRenderer extends Event {
   private final InteractionHand hand;
   private final ItemStack item;
   private float ep;
   private final PoseStack stack;

   public EventHeldItemRenderer(InteractionHand hand, ItemStack item, float equipProgress, PoseStack stack) {
      this.hand = hand;
      this.item = item;
      this.ep = equipProgress;
      this.stack = stack;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public ItemStack getItem() {
      return this.item;
   }

   public float getEp() {
      return this.ep;
   }

   public PoseStack getStack() {
      return this.stack;
   }
}
