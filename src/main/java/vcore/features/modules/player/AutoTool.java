package vcore.features.modules.player;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.phys.BlockHitResult;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class AutoTool extends Module {
   public static Setting<Boolean> swapBack = new Setting<>("SwapBack", true);
   public static Setting<Boolean> saveItem = new Setting<>("SaveItem", true);
   public static Setting<Boolean> silent = new Setting<>("Silent", false);
   public static Setting<Boolean> echestSilk = new Setting<>("EchestSilk", true);
   public static int itemIndex;
   private boolean swap;
   private long swapDelay;
   private final List<Integer> lastItem = new ArrayList<>();

   public AutoTool() {
      super("AutoTool", "Auto switches to best tool.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (mc.hitResult instanceof BlockHitResult result) {
         BlockPos pos = result.getBlockPos();
         if (!mc.level.getBlockState(pos).isAir()) {
            if (getTool(pos) != -1 && mc.options.keyAttack.isDown()) {
               this.lastItem.add(mc.player.getInventory().getSelectedSlot());
               if (silent.getValue()) {
                  mc.player.connection.send(new ServerboundSetCarriedItemPacket(getTool(pos)));
               } else {
                  mc.player.getInventory().setSelectedSlot(getTool(pos));
               }

               itemIndex = getTool(pos);
               this.swap = true;
               this.swapDelay = System.currentTimeMillis();
            } else if (this.swap && !this.lastItem.isEmpty() && System.currentTimeMillis() >= this.swapDelay + 300L && swapBack.getValue()) {
               if (silent.getValue()) {
                  mc.player.connection.send(new ServerboundSetCarriedItemPacket(this.lastItem.get(0)));
               } else {
                  mc.player.getInventory().setSelectedSlot(this.lastItem.get(0));
               }

               itemIndex = this.lastItem.get(0);
               this.lastItem.clear();
               this.swap = false;
            }
         }
      }
   }

   public static int getTool(BlockPos pos) {
      int index = -1;
      float CurrentFastest = 1.0F;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack != ItemStack.EMPTY
            && (mc.player.getInventory().getItem(i).getMaxDamage() - mc.player.getInventory().getItem(i).getDamageValue() > 10 || !saveItem.getValue())) {
            float digSpeed = EnchantmentHelper.getItemEnchantmentLevel(
               mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), stack
            );
            float destroySpeed = stack.getDestroySpeed(mc.level.getBlockState(pos));
            if (mc.level.getBlockState(pos).getBlock() instanceof AirBlock) {
               return -1;
            }

            if (mc.level.getBlockState(pos).getBlock() instanceof EnderChestBlock && echestSilk.getValue()) {
               if (EnchantmentHelper.getItemEnchantmentLevel(
                        mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
                        stack
                     )
                     > 0
                  && digSpeed + destroySpeed > CurrentFastest) {
                  CurrentFastest = digSpeed + destroySpeed;
                  index = i;
               }
            } else if (digSpeed + destroySpeed > CurrentFastest) {
               CurrentFastest = digSpeed + destroySpeed;
               index = i;
            }
         }
      }

      return index;
   }
}
