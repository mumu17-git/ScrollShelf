package com.mumu17.scrollshelf;

import com.mumu17.scrollshelf.shelf.packet.ExtractScrollPayload;
import com.mumu17.scrollshelf.shelf.packet.SyncShelfScrollsPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworks {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ScrollShelf.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        // サーバー行きパケットの登録
        CHANNEL.messageBuilder(ExtractScrollPayload.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ExtractScrollPayload::toBytes)
                .decoder(ExtractScrollPayload::new)
                .consumerNetworkThread(ExtractScrollPayload::handle)
                .add();

        // クライアント行きパケットの登録
        CHANNEL.messageBuilder(SyncShelfScrollsPayload.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShelfScrollsPayload::toBytes)
                .decoder(SyncShelfScrollsPayload::new)
                .consumerNetworkThread(SyncShelfScrollsPayload::handle)
                .add();
    }

    // サーバーから特定プレイヤーへパケットを送信するヘルパー
    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    // クライアントからサーバーへパケットを送信するヘルパー（必要に応じて利用）
    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}