package com.mumu17.scrollshelf;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ScrollShelf.MODID);

    public static final Supplier<BlockItem> SCROLL_SHELF_ITEM = ITEMS.register(
            "scroll_shelf",
            () -> new BlockItem(ModBlocks.SCROLL_SHELF_BLOCK.get(), new Item.Properties())
    );
}
