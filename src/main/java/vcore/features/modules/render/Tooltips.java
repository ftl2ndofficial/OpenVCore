package vcore.features.modules.render;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class Tooltips extends Module {
   public static final Setting<Boolean> middleClickOpen = new Setting<>("MiddleClickOpen", false);
   public static final Setting<Boolean> storage = new Setting<>("Storage", false);
   public static final Setting<Boolean> maps = new Setting<>("Maps", false);
   public final Setting<Boolean> shulkerRegear = new Setting<>("ShulkerRegear", false);
   public final Setting<Boolean> shulkerRegearShiftMode = new Setting<>("RegearShift", false);

   public Tooltips() {
      super("Tooltips", "Shows extra item information.", Module.Category.MISC);
   }

   public static boolean hasItems(ItemStack itemStack) {
      ItemContainerContents compoundTag = (ItemContainerContents)itemStack.get(DataComponents.CONTAINER);
      return compoundTag != null && compoundTag.nonEmptyItems().iterator().hasNext();
   }
}
