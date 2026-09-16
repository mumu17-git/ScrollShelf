package com.mumu17.scrollshelf;

import com.mumu17.scrollshelf.shelf.ScrollShelfBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ScrollShelf.MODID);

    public static final Supplier<BlockEntityType<ScrollShelfBlockEntity>> SCROLL_SHELF_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "scroll_shelf_block_entity",
            () -> BlockEntityType.Builder.of(
                            ScrollShelfBlockEntity::new,
                            ModBlocks.SCROLL_SHELF_BLOCK.get()
                    )
                    .build(null)
    );
}
