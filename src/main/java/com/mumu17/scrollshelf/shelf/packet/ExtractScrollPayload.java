package com.mumu17.scrollshelf.shelf.packet;

import com.mumu17.scrollshelf.ScrollShelf;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ExtractScrollPayload(BlockPos pos, String spellId, int baseSpellLevel, int spellLevel, boolean needCraft) implements CustomPacketPayload {

    public static final Type<ExtractScrollPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ScrollShelf.MODID, "extract_scroll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractScrollPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {
                        buf.writeBlockPos(msg.pos());
                        buf.writeUtf(msg.spellId());
                        buf.writeInt(msg.baseSpellLevel());
                        buf.writeInt(msg.spellLevel());
                        buf.writeBoolean(msg.needCraft());
                    },
                    buf -> new ExtractScrollPayload(buf.readBlockPos(), buf.readUtf(), buf.readInt(), buf.readInt(), buf.readBoolean())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
