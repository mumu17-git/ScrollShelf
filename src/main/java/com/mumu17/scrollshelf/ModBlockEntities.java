package com.mumu17.scrollshelf;

import com.mumu17.scrollshelf.shelf.ScrollShelfBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@EventBusSubscriber(modid = ScrollShelf.MODID)
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

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SCROLL_SHELF_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemHandler()
        );
    }
}
