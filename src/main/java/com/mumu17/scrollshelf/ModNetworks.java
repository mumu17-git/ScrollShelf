package com.mumu17.scrollshelf;

import com.mumu17.scrollshelf.shelf.ScrollShelfBlockEntity;
import com.mumu17.scrollshelf.shelf.gui.ScrollShelfScreen;
import com.mumu17.scrollshelf.shelf.packet.ExtractScrollPayload;
import com.mumu17.scrollshelf.shelf.packet.SyncShelfScrollsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
@EventBusSubscriber(modid = ScrollShelf.MODID)
public class ModNetworks {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(
                        ExtractScrollPayload.TYPE,
                        ExtractScrollPayload.STREAM_CODEC,
                        (msg, ctx) -> {
                            ServerPlayer player = (ServerPlayer) ctx.player();
                            Level level = player.level();

                            BlockEntity be = level.getBlockEntity(msg.pos());
                            if (be instanceof ScrollShelfBlockEntity myBe) {
                                myBe.extractScroll(msg.spellId(), msg.spellLevel(), player);
                                PacketDistributor.sendToPlayer(
                                        player,
                                        new SyncShelfScrollsPayload(msg.pos(), myBe.createScrollsSyncTag())
                                );
                            }
                        }
                )
                .playToClient(
                        SyncShelfScrollsPayload.TYPE,
                        SyncShelfScrollsPayload.STREAM_CODEC,
                        (msg, ctx) -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.level == null) return;
                            BlockEntity be = mc.level.getBlockEntity(msg.pos());
                            if (be instanceof ScrollShelfBlockEntity shelf) {
                                shelf.SCROLLS.clear();
                                shelf.loadScrollData(msg.tag());
                                if (mc.screen instanceof ScrollShelfScreen screen) {
                                    screen.getMenu().markScrollsChanged();
                                }
                            }
                        }
                );
    }
}
