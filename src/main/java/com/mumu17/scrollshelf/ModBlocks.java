package com.mumu17.scrollshelf;

import com.mumu17.scrollshelf.shelf.ScrollShelfBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ScrollShelf.MODID);

    public static final RegistryObject<Block> SCROLL_SHELF_BLOCK = BLOCKS.register(
            "scroll_shelf",
            () -> new ScrollShelfBlock(BlockBehaviour.Properties.of()
                    .strength(5.0F, 1200.0F)
                    .explosionResistance(10.0f)
                    .sound(SoundType.WOOD)
                    .mapColor(MapColor.COLOR_BROWN)
                    .lightLevel(state -> 0)
            ));
}
