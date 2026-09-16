package com.mumu17.scrollshelf;

import com.mumu17.scrollshelf.shelf.gui.ScrollShelfMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, ScrollShelf.MODID);

    public static final RegistryObject<MenuType<ScrollShelfMenu>> SCROLL_SHELF_MENU =
            REGISTER.register("scroll_shelf_menu",
                    () -> IForgeMenuType.create((containerId, playerInv, buf) -> new ScrollShelfMenu(containerId, playerInv, ContainerLevelAccess.NULL, buf.readBlockPos())));

}