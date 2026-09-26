package com.mumu17.scrollshelf;

import com.mojang.logging.LogUtils;
import com.mumu17.scrollshelf.shelf.gui.ScrollShelfScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;

@Mod(ScrollShelf.MODID)
public class ScrollShelf {
    public static final String MODID = "scrollshelf";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ScrollShelf() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 各レジストリの登録
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModMenus.REGISTER.register(modEventBus);

        // セットアップイベントの登録
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // サーバーイベントなどのためにMinecraftForgeバスにも登録
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetworks::register);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 1.20.1 Forgeでのメニュー画面（Screen）の登録
            MenuScreens.register(ModMenus.SCROLL_SHELF_MENU.get(), ScrollShelfScreen::new);
        });
    }

    public static void LOGGER(String format, Object... arguments) {
        if (!FMLLoader.isProduction())
            LOGGER.debug(format, arguments);
    }
}