package com.mumu17.scrollshelf;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ScrollShelf.MODID);

    public static final Supplier<BlockItem> SCROLL_SHELF_ITEM = ITEMS.registerSimpleBlockItem(
            "scroll_shelf",
            ModBlocks.SCROLL_SHELF_BLOCK, new Item.Properties()
    );
}
