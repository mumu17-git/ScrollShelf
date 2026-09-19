package com.mumu17.scrollshelf.shelf.packet;

import com.mumu17.scrollshelf.ModNetworks;
import com.mumu17.scrollshelf.shelf.ScrollShelfBlockEntity;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
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
    private final int baseSpellLevel;
    private final int spellLevel;
    private final boolean needCraft;

    public ExtractScrollPayload(BlockPos pos, String spellId, int baseSpellLevel, int spellLevel, boolean needCraft) {
        this.pos = pos;
        this.spellId = spellId;
        this.baseSpellLevel = baseSpellLevel;
        this.spellLevel = spellLevel;
        this.needCraft = needCraft;
    }

    // バッファからの読み込み（デコーダ）
    public ExtractScrollPayload(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.spellId = buf.readUtf();
        this.baseSpellLevel = buf.readInt();
        this.spellLevel = buf.readInt();
        this.needCraft = buf.readBoolean();
    }

    // バッファへの書き込み（エンコーダ）
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeUtf(this.spellId);
        buf.writeInt(this.baseSpellLevel);
        buf.writeInt(this.spellLevel);
        buf.writeBoolean(this.needCraft);
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
                if (needCraft) {
                    AbstractSpell spell = SpellRegistry.getSpell(spellId);
                    myBe.craftAndExtractScroll(spell, baseSpellLevel, spellLevel, player.getInventory().items, player);
                } else {
                    myBe.extractScroll(spellId, spellLevel, player, false);
                }

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