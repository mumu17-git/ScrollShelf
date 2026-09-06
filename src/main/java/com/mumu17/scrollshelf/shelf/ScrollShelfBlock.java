package com.mumu17.scrollshelf.shelf;

import com.mumu17.scrollshelf.shelf.gui.ScrollShelfMenu;
import com.mumu17.scrollshelf.shelf.packet.SyncShelfScrollsPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ScrollShelfBlock extends Block implements EntityBlock {

    public ScrollShelfBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ScrollShelfBlockEntity(blockPos, blockState);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (containerId, playerInv, player) ->
                        new ScrollShelfMenu(containerId, playerInv, ContainerLevelAccess.create(level, pos), pos),
                Component.translatable("menu.scrollshelf.scroll_shelf.title")
        );
    }

    @Override
    public InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(state.getMenuProvider(level, pos), buf -> buf.writeBlockPos(pos));
            if (level.getBlockEntity(pos) instanceof ScrollShelfBlockEntity shelf) {
                PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new SyncShelfScrollsPayload(pos, shelf.createScrollsSyncTag())
                );
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
