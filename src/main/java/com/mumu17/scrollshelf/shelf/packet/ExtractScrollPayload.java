package com.mumu17.scrollshelf.shelf.packet;

import com.mumu17.scrollshelf.ModNetworks;
import com.mumu17.scrollshelf.shelf.ScrollShelfBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExtractScrollPayload {
    private final BlockPos pos;
    private final String spellId;
    private final int spellLevel;

    public ExtractScrollPayload(BlockPos pos, String spellId, int spellLevel) {
        this.pos = pos;
        this.spellId = spellId;
        this.spellLevel = spellLevel;
    }

    // バッファからの読み込み（デコーダ）
    public ExtractScrollPayload(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.spellId = buf.readUtf();
        this.spellLevel = buf.readInt();
    }

    // バッファへの書き込み（エンコーダ）
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeUtf(this.spellId);
        buf.writeInt(this.spellLevel);
    }

    // パケット受信時の処理 (Server)
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            Level level = player.level();

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ScrollShelfBlockEntity myBe) {
                myBe.extractScroll(spellId, spellLevel, player);

                ModNetworks.sendToPlayer(
                        player,
                        new SyncShelfScrollsPayload(pos, myBe.createScrollsSyncTag())
                );
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}