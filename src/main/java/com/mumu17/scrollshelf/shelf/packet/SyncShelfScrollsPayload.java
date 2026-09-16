package com.mumu17.scrollshelf.shelf.packet;

import com.mumu17.scrollshelf.shelf.ScrollShelfBlockEntity;
import com.mumu17.scrollshelf.shelf.gui.ScrollShelfScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

public class SyncShelfScrollsPayload {
    private final BlockPos pos;
    private final CompoundTag tag;

    public SyncShelfScrollsPayload(BlockPos pos, CompoundTag tag) {
        this.pos = pos;
        this.tag = tag;
    }

    // バッファからの読み込み（デコーダ）
    public SyncShelfScrollsPayload(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.tag = buf.readNbt();
    }

    // バッファへの書き込み（エンコーダ）
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeNbt(this.tag);
    }

    // パケット受信時の処理 (Client)
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // クライアント側でのみ実行を安全に保証
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) return;
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be instanceof ScrollShelfBlockEntity shelf) {
                    shelf.SCROLLS.clear();
                    shelf.loadScrollData(tag);
                    if (mc.screen instanceof ScrollShelfScreen screen) {
                        screen.getMenu().markScrollsChanged();
                    }
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}