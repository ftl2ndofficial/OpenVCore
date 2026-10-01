package vcore.utility.player;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.injection.accesors.IInteractionManager;

public final class InventoryUtility {
   private static int cachedSlot = -1;

   public static int getItemCount(Item item) {
      if (Module.mc.player == null) {
         return 0;
      }

      int counter = 0;

      for (int i = 0; i <= 44; i++) {
         ItemStack itemStack = Module.mc.player.getInventory().getItem(i);
         if (itemStack.getItem() == item) {
            counter += itemStack.getCount();
         }
      }

      return counter;
   }

   public static SearchInvResult getAxe() {
      if (Module.mc.player == null) {
         return SearchInvResult.notFound();
      }

      int slot = -1;
      float f = 1.0F;
      Holder<net.minecraft.world.item.enchantment.Enchantment> sharpness = Module.mc.level != null
         ? Module.mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SHARPNESS).orElse(null)
         : null;

      for (int b1 = 9; b1 < 45; b1++) {
         ItemStack itemStack = Module.mc.player.getInventory().getItem(b1 >= 36 ? b1 - 36 : b1);
         if (itemStack != null && itemStack.getItem() instanceof AxeItem axe) {
            float f1 = ((Integer)axe.components().get(DataComponents.MAX_DAMAGE)).intValue();
            if (sharpness != null) {
               f1 += EnchantmentHelper.getItemEnchantmentLevel(sharpness, itemStack);
            }
            if (f1 > f) {
               f = f1;
               slot = b1;
            }
         }
      }

      if (slot >= 36) {
         slot -= 36;
      }

      return slot == -1 ? SearchInvResult.notFound() : new SearchInvResult(slot, true, Module.mc.player.getInventory().getItem(slot));
   }

   public static SearchInvResult getSwordHotBar() {
      if (Module.mc.player == null) {
         return SearchInvResult.notFound();
      }

      int slot = -1;
      float f = 1.0F;
      Holder<net.minecraft.world.item.enchantment.Enchantment> sharpness = Module.mc.level != null
         ? Module.mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SHARPNESS).orElse(null)
         : null;

      for (int b1 = 0; b1 < 9; b1++) {
         ItemStack itemStack = Module.mc.player.getInventory().getItem(b1);
         if (itemStack != null && itemStack.is(ItemTags.SWORDS)) {
            float f1 = (float)itemStack.getMaxDamage();
            if (sharpness != null) {
               f1 += EnchantmentHelper.getItemEnchantmentLevel(sharpness, itemStack);
            }
            if (f1 > f) {
               f = f1;
               slot = b1;
            }
         }
      }

      return slot == -1 ? SearchInvResult.notFound() : new SearchInvResult(slot, true, Module.mc.player.getInventory().getItem(slot));
   }

   public static int getElytra() {
      ItemStack chestStack = Module.mc.player.getItemBySlot(EquipmentSlot.CHEST);
      if (chestStack.getItem() == Items.ELYTRA && chestStack.getDamageValue() < 430) {
         return -2;
      }

      int slot = -1;

      for (int i = 0; i < 36; i++) {
         ItemStack s = Module.mc.player.getInventory().getItem(i);
         if (s.getItem() == Items.ELYTRA && s.getDamageValue() < 430) {
            slot = i;
            break;
         }
      }

      if (slot < 9 && slot != -1) {
         slot += 36;
      }

      return slot;
   }

   public static SearchInvResult findInHotBar(InventoryUtility.Searcher searcher) {
      if (Module.mc.player != null) {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = Module.mc.player.getInventory().getItem(i);
            if (searcher.isValid(stack)) {
               return new SearchInvResult(i, true, stack);
            }
         }
      }

      return SearchInvResult.notFound();
   }

   public static SearchInvResult findItemInHotBar(List<Item> items) {
      return findInHotBar(stack -> items.contains(stack.getItem()));
   }

   public static SearchInvResult findItemInHotBar(Item... items) {
      return findItemInHotBar(Arrays.asList(items));
   }

   public static SearchInvResult findInInventory(InventoryUtility.Searcher searcher) {
      if (Module.mc.player != null) {
         for (int i = 36; i >= 0; i--) {
            ItemStack stack = Module.mc.player.getInventory().getItem(i);
            if (searcher.isValid(stack)) {
               if (i < 9) {
                  i += 36;
               }

               return new SearchInvResult(i, true, stack);
            }
         }
      }

      return SearchInvResult.notFound();
   }

   public static SearchInvResult findItemInInventory(List<Item> items) {
      return findInInventory(stack -> items.contains(stack.getItem()));
   }

   public static SearchInvResult findItemInInventory(Item... items) {
      return findItemInInventory(Arrays.asList(items));
   }

   public static SearchInvResult findBlockInHotBar(@NotNull List<Block> blocks) {
      return findItemInHotBar(blocks.stream().<Item>map(Block::asItem).toList());
   }

   public static SearchInvResult findBlockInHotBar(Block... blocks) {
      return findItemInHotBar(Arrays.stream(blocks).<Item>map(Block::asItem).toList());
   }

   public static SearchInvResult findBlockInInventory(@NotNull List<Block> blocks) {
      return findItemInInventory(blocks.stream().<Item>map(Block::asItem).toList());
   }

   public static SearchInvResult findBlockInInventory(Block... blocks) {
      return findItemInInventory(Arrays.stream(blocks).<Item>map(Block::asItem).toList());
   }

   public static void saveSlot() {
      cachedSlot = Module.mc.player.getInventory().getSelectedSlot();
   }

   public static void returnSlot() {
      if (cachedSlot != -1) {
         switchTo(cachedSlot);
      }

      cachedSlot = -1;
   }

   public static void saveAndSwitchTo(int slot) {
      saveSlot();
      if (Module.mc.player != null && Module.mc.getConnection() != null) {
         if (Module.mc.player.getInventory().getSelectedSlot() != slot || Managers.PLAYER.serverSideSlot != slot) {
            Module.mc.player.getInventory().setSelectedSlot(slot);
            ((IInteractionManager)Module.mc.gameMode).syncSlot();
         }
      }
   }

   public static void switchTo(int slot) {
      if (Module.mc.player != null && Module.mc.getConnection() != null) {
         if (Module.mc.player.getInventory().getSelectedSlot() != slot || Managers.PLAYER.serverSideSlot != slot) {
            Module.mc.player.getInventory().setSelectedSlot(slot);
            ((IInteractionManager)Module.mc.gameMode).syncSlot();
         }
      }
   }

   public static void switchToSilent(int slot) {
      if (Module.mc.player != null && Module.mc.getConnection() != null) {
         Module.mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
      }
   }

   public static SearchInvResult getAntiWeaknessItem() {
      if (Module.mc.player == null) {
         return SearchInvResult.notFound();
      }

      ItemStack mainStack = Module.mc.player.getMainHandItem();
      Item mainHand = mainStack.getItem();
      return !mainStack.is(ItemTags.SWORDS) && !mainStack.is(ItemTags.PICKAXES) && !(mainHand instanceof AxeItem) && !(mainHand instanceof ShovelItem)
         ? findInHotBar(
            itemStack -> itemStack.is(ItemTags.SWORDS)
               || itemStack.is(ItemTags.PICKAXES)
               || itemStack.getItem() instanceof AxeItem
               || itemStack.getItem() instanceof ShovelItem
         )
         : new SearchInvResult(Module.mc.player.getInventory().getSelectedSlot(), true, Module.mc.player.getMainHandItem());
   }

   public static float getHitDamage(@NotNull ItemStack weapon, Player ent) {
      if (Module.mc.player == null) {
         return 0.0F;
      }

      float baseDamage = 1.0F;
      if (weapon.is(ItemTags.SWORDS)) {
         baseDamage = 7.0F;
      }

      if (weapon.getItem() instanceof AxeItem axeItem) {
         baseDamage = 9.0F;
      }

      if (Module.mc.player.fallDistance > 0.0F || ModuleManager.criticals.isEnabled()) {
         baseDamage += baseDamage / 2.0F;
      }

      if (Module.mc.player.hasEffect(MobEffects.STRENGTH)) {
         int strength = Objects.requireNonNull(Module.mc.player.getEffect(MobEffects.STRENGTH)).getAmplifier() + 1;
         baseDamage += 3 * strength;
      }

      return CombatRules.getDamageAfterAbsorb(
         ent,
         baseDamage,
         Module.mc.level.damageSources().generic(),
         ent.getArmorValue(),
         (float)ent.getAttribute(Attributes.ARMOR_TOUGHNESS).getValue()
      );
   }

   public interface Searcher {
      boolean isValid(ItemStack var1);
   }
}
