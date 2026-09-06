package com.mumu17.scrollshelf.shelf.gui;

import com.mumu17.scrollshelf.ModBlocks;
import com.mumu17.scrollshelf.ModMenus;
import com.mumu17.scrollshelf.shelf.ScrollShelfBlockEntity;
import com.mumu17.scrollshelf.shelf.packet.SyncShelfScrollsPayload;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ScrollShelfMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;

    private final ScrollShelfBlockEntity blockEntity;
    private final BlockPos blockPos;

    private int scrollsVersion = 0;

    @Nullable
    private String filterSpellId;

    private final Container inputContainer;

    public ScrollShelfMenu(int containerId, Inventory playerInv, ContainerLevelAccess access, BlockPos pos) {
        super(ModMenus.SCROLL_SHELF_MENU.get(), containerId);
        this.access = access;
        this.blockPos = pos;
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        this.blockEntity = be instanceof ScrollShelfBlockEntity shelf ? shelf : null;
        this.inputContainer = new SimpleContainer(2);
        this.addSlot(new Slot(this.inputContainer, 0, 7, 51) {

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof Scroll && ISpellContainer.isSpellContainer(stack);
            }


            @Override
            public void setChanged() {
                super.setChanged();

                ItemStack stack = this.getItem();
                if (!stack.isEmpty()) {
                    access.execute((level, pos) -> {
                        if (level.getBlockEntity(pos) instanceof ScrollShelfBlockEntity shelf) {
                            if (shelf.depositScroll(stack)) {
                                this.set(ItemStack.EMPTY);
                                if (!level.isClientSide && playerInv.player instanceof ServerPlayer serverPlayer) {
                                    PacketDistributor.sendToPlayer(
                                            serverPlayer,
                                            new SyncShelfScrollsPayload(pos, shelf.createScrollsSyncTag())
                                    );
                                }
                            }
                        }
                    });
                }
            }
        });

        this.addSlot(new Slot(this.inputContainer, 1, 180, 51) {

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof Scroll && ISpellContainer.isSpellContainer(stack);
            }


            @Override
            public void setChanged() {
                super.setChanged();
                updateFilterFromStack(this.getItem());
                access.execute((level, pos) -> {
                    if (level.getBlockEntity(pos) instanceof ScrollShelfBlockEntity shelf) {
                        if (!level.isClientSide && playerInv.player instanceof ServerPlayer serverPlayer) {
                            PacketDistributor.sendToPlayer(
                                    serverPlayer,
                                    new SyncShelfScrollsPayload(pos, shelf.createScrollsSyncTag())
                            );
                        }
                    }
                });
            }
        });

        // --- プレイヤーインベントリ ---
        addPlayerInventory(playerInv);
    }


    public boolean matchesFilter(String spellId) {
        if (filterSpellId == null) return true; // 空なら全表示
        return filterSpellId.equals(spellId);
    }

    private void updateFilterFromStack(ItemStack stack) {
        if (stack.getItem() instanceof Scroll && ISpellContainer.isSpellContainer(stack)) {
            var container = ISpellContainer.get(stack);
            if (!container.isEmpty()) {
                var data = container.getSpellAtIndex(0);
                this.filterSpellId = data.getSpell().getSpellId();
                return;
            }
        }
        this.filterSpellId = null;
    }

    private void addPlayerInventory(Inventory playerInv) {
        final int xOffset = 22;
        final int yOffset = 127;

        // 3×9
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        xOffset + col * 18, yOffset + row * 18));
            }
        }

        // ホットバー
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, xOffset + col * 18, yOffset + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, ModBlocks.SCROLL_SHELF_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            // 自作スロット（0番）
            if (index == 0) {
                // → プレイヤーインベントリへ移動
                if (!this.moveItemStackTo(stackInSlot, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // プレイヤーインベントリ → 自作スロットへ移動
                if (!(stackInSlot.getItem() instanceof Scroll) || !ISpellContainer.isSpellContainer(stackInSlot)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            this.clearContainer(player, this.inputContainer);
        }
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public @Nullable ScrollShelfBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public Object2IntMap<Object2IntMap<String>> getScrolls() {
        return getBlockEntity() != null ? getBlockEntity().SCROLLS : new Object2IntOpenHashMap<>();
    }

    public int getScrollsVersion() { return scrollsVersion; }
    public void markScrollsChanged() { scrollsVersion++; }
}

