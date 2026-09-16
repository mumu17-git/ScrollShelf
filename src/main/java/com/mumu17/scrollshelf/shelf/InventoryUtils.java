package com.mumu17.scrollshelf.shelf;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InventoryUtils {
    public static boolean hasSpaceFor(Player player, ItemStack stack) {
        Inventory inventory = player.getInventory();

        // 1. 完全に空いているスロットがあるかチェック
        int emptySlot = inventory.getFreeSlot();
        if (emptySlot != -1) {
            return true;
        }

        // 2. 既存の同じアイテムのスタックにまとめられる（まだ最大数に達していない）かチェック
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack existingStack = inventory.items.get(i);

            // アイテムの種類が同じで、かつ最大スタック数に余裕があるか
            if (ItemStack.isSameItemSameTags(existingStack, stack)) {
                int totalCount = existingStack.getCount() + stack.getCount();
                if (totalCount <= existingStack.getMaxStackSize()) {
                    return true;
                }
            }
        }

        return false;
    }
}
