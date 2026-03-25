package savage.commoneconomy.shop;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

/**
 * Calculates stock or available space in a shop container.
 */
public class ShopStockCalculator {

    public static int calculateStock(ServerLevel world, Shop shop) {
        if (shop.isAdmin()) {
            return -1; // Infinite stock
        }

        BlockEntity blockEntity = world.getBlockEntity(shop.getChestLocation());
        if (!(blockEntity instanceof Container container)) {
            return 0;
        }

        if (shop.isBuying()) {
            // Shop is buying -> Calculate how much space is available for the item
            return calculateSpaceForItems(container, shop.getItem());
        } else {
            // Shop is selling -> Calculate how many items are in the chest
            return countItemsInInventory(container, shop.getItem());
        }
    }

    private static int countItemsInInventory(Container container, ItemStack template) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int calculateSpaceForItems(Container container, ItemStack template) {
        int space = 0;
        int maxStackSize = template.getMaxStackSize();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                space += maxStackSize;
            } else if (ItemStack.isSameItemSameComponents(stack, template)) {
                space += (maxStackSize - stack.getCount());
            }
        }
        return space;
    }
}
