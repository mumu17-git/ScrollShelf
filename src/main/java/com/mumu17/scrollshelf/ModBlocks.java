package com.mumu17.scrollshelf;

import com.mumu17.scrollshelf.shelf.ScrollShelfBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ScrollShelf.MODID);

    public static final DeferredBlock<Block> SCROLL_SHELF_BLOCK = BLOCKS.register(
            "scroll_shelf",
            () -> new ScrollShelfBlock(BlockBehaviour.Properties.of()
                    .strength(5.0F, 1200.0F)
                    .explosionResistance(10.0f)
                    .sound(SoundType.WOOD)
                    .mapColor(MapColor.COLOR_BROWN)
                    .lightLevel(state -> 0)
            ));
}
