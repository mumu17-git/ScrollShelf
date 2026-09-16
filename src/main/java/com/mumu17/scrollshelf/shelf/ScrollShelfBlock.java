package com.mumu17.scrollshelf.shelf;

import com.mumu17.scrollshelf.ModItems;
import com.mumu17.scrollshelf.ModNetworks;
import com.mumu17.scrollshelf.shelf.gui.ScrollShelfMenu;
import com.mumu17.scrollshelf.shelf.packet.SyncShelfScrollsPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ScrollShelfBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public ScrollShelfBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    public InteractionResult use(
            BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            net.minecraftforge.network.NetworkHooks.openScreen(
                    serverPlayer,
                    state.getMenuProvider(level, pos),
                    buf -> buf.writeBlockPos(pos)
            );

            if (level.getBlockEntity(pos) instanceof ScrollShelfBlockEntity shelf) {
                ModNetworks.sendToPlayer(
                        serverPlayer,
                        new SyncShelfScrollsPayload(pos, shelf.createScrollsSyncTag())
                );
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (oldState.getBlock() != newState.getBlock()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof ScrollShelfBlockEntity shelf) {
                ItemStack drop = new ItemStack(ModItems.SCROLL_SHELF_ITEM.get());

                CompoundTag beTag = shelf.createScrollsSyncTag();
                beTag.putString("id", "scrollshelf:scroll_shelf_block_entity");

                // 1.20.1のNBT付与方法に修正
                CompoundTag tag = new CompoundTag();
                tag.put("BlockEntityTag", beTag);
                drop.setTag(tag);

                popResource(level, pos, drop);
            }
            super.onRemove(oldState, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide) return;
        if (!(level.getBlockEntity(pos) instanceof ScrollShelfBlockEntity shelf)) return;

        // 1.20.1のNBT取得方法に修正
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("BlockEntityTag", 10)) {
            CompoundTag beTag = tag.getCompound("BlockEntityTag");
            shelf.load(beTag); // 1.20.1のloadメソッドを使用
            shelf.setChanged();
        }
    }
}