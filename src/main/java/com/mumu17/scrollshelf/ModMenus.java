package com.mumu17.scrollshelf;

import com.mumu17.scrollshelf.shelf.ScrollShelfBlockEntity;
import com.mumu17.scrollshelf.shelf.gui.ScrollShelfMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, ScrollShelf.MODID);

    public static final Supplier<MenuType<ScrollShelfMenu>> SCROLL_SHELF_MENU =
            REGISTER.register("scroll_shelf_menu",
                    () -> IMenuTypeExtension.create((containerId, playerInv, buf) -> new ScrollShelfMenu(containerId, playerInv, ContainerLevelAccess.NULL, buf.readBlockPos())));

}
