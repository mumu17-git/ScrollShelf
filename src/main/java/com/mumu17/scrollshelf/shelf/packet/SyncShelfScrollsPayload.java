package com.mumu17.scrollshelf.shelf.packet;

import com.mumu17.scrollshelf.ScrollShelf;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record SyncShelfScrollsPayload(BlockPos pos, CompoundTag tag) implements CustomPacketPayload {

    public static final Type<SyncShelfScrollsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ScrollShelf.MODID, "sync_shelf_scrolls"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncShelfScrollsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {
                        buf.writeBlockPos(msg.pos());
                        buf.writeNbt(msg.tag());
                    },
                    buf -> new SyncShelfScrollsPayload(buf.readBlockPos(), buf.readNbt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}