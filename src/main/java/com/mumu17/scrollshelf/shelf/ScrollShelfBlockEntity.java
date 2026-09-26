package com.mumu17.scrollshelf.shelf;

import com.mumu17.scrollshelf.ModBlockEntities;
import com.mumu17.scrollshelf.shelf.packet.SyncShelfScrollsPayload;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class ScrollShelfBlockEntity extends BlockEntity {

    public final Object2IntMap<Object2IntMap<String>> SCROLLS = new Object2IntOpenHashMap<>();

    public ScrollShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCROLL_SHELF_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean depositScroll(ItemStack stack) {
        if (stack.getItem() instanceof Scroll && ISpellContainer.isSpellContainer(stack)) {
            var spellList = ISpellContainer.get(stack);
            if (spellList.isEmpty()) {
                return false;
            }
            var spellData = spellList.getSpellAtIndex(0);
            var spell = spellData.getSpell();
            var spellId = spell.getSpellId();
            var spellLevel = spellData.getLevel();

            Object2IntMap<String> idLevelMap = new Object2IntOpenHashMap<>();
            idLevelMap.put(spellId, spellLevel);
            int currentCount = SCROLLS.getOrDefault(idLevelMap, 0);
            SCROLLS.put(idLevelMap, currentCount + stack.getCount());
            this.setChanged();
            return true;
        }
        return false;
    }

    public void extractScroll(String id, int level, @Nullable Player player, boolean crafted) {
        if (crafted) {
            extract(player, id, level);
            this.setChanged();
        } else if (canExtract(id, level)) {
            Object2IntMap<String> idLevelMap = new Object2IntOpenHashMap<>();
            idLevelMap.put(id, level);
            int currentCount = SCROLLS.getOrDefault(idLevelMap, 0);
            extract(player, id, level);
            SCROLLS.put(idLevelMap, currentCount - 1);
            this.setChanged();
        }
    }

    public void deleteScroll(String id, int level) {
        if (canExtract(id, level)) {
            Object2IntMap<String> idLevelMap = new Object2IntOpenHashMap<>();
            idLevelMap.put(id, level);
            int currentCount = SCROLLS.getOrDefault(idLevelMap, 0);
            SCROLLS.put(idLevelMap, currentCount - 1);
            this.setChanged();
        }
    }

    private void extract(@Nullable Player player, String id, int level) {
        ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(SpellRegistry.getSpell(id), level, scroll);
        if (player != null && InventoryUtils.hasSpaceFor(player, scroll)) {
            player.addItem(scroll);
        } else if (this.level != null) {
            ItemEntity itemEntity = new ItemEntity(this.level, this.getBlockPos().getCenter().x, this.getBlockPos().getCenter().y+1.0D, this.getBlockPos().getCenter().z, scroll);
            itemEntity.setDeltaMovement(0.0D, 0.2D, 0.0D);
            this.level.addFreshEntity(itemEntity);
        }
    }

    public boolean canExtract(String id, int level) {
        Object2IntMap<String> idLevelMap = new Object2IntOpenHashMap<>();
        idLevelMap.put(id, level);
        return this.SCROLLS.containsKey(idLevelMap) && this.SCROLLS.getInt(idLevelMap) > 0;
    }

    public void craftAndExtractScroll(AbstractSpell spell, int baseLevel, int spellLevel, NonNullList<ItemStack> items, ServerPlayer player) {
        if (!canCraft(spell, baseLevel, spellLevel, items)) return;
        outer_loop:
        for (int i = baseLevel; i < spellLevel; i++) {
            ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(spell, i, scroll);
            var spell1 = ISpellContainer.get(scroll).getSpellAtIndex(0);
            if (i < spell1.getSpell().getMaxLevel()) {
                var nextRarity = spell1.getSpell().getRarity(i + 1);
                for (ItemStack item : items) {
                    if (item.getItem() instanceof InkItem ink && item.getCount() > 0 && ink.getRarity().equals(nextRarity)) {
                        items.get(items.indexOf(item)).shrink(1);
                        continue outer_loop;
                    }
                }
            }
        }

        extractScroll(spell.getSpellId(), spellLevel, player, true);
        deleteScroll(spell.getSpellId(), baseLevel);
    }

    public boolean canCraft(AbstractSpell spell, int baseLevel, int spellLevel, NonNullList<ItemStack> items) {
        if (baseLevel >= spellLevel) return false;
        Map<Item, Integer> itemMap = new HashMap<>();
        for (ItemStack itemStack : items) {
            if (itemStack.getItem() instanceof InkItem ink && itemStack.getCount() > 0) {
                if (!itemMap.containsKey(ink)) {
                    itemMap.put(ink, itemStack.getCount());
                } else {
                    itemMap.put(ink, itemMap.get(ink) + itemStack.getCount());
                }
            }
        }

        outer_loop:
        for (int i = baseLevel; i < spellLevel; i++) {
            ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(spell, i, scroll);
            var spell1 = ISpellContainer.get(scroll).getSpellAtIndex(0);
            if (i < spell1.getSpell().getMaxLevel()) {
                var nextRarity = spell1.getSpell().getRarity(i + 1);
                for (Item item : itemMap.keySet()) {
                    if (item instanceof InkItem ink && itemMap.get(item) > 0 && ink.getRarity().equals(nextRarity)) {
                        itemMap.put(item, itemMap.get(item) - 1);
                        if (itemMap.get(item) <= 0) {
                            itemMap.remove(item);
                        }
                        continue outer_loop;
                    }
                }
                return false;
            }
        }
        return true;
    }

    public void loadScrollData(CompoundTag tag) {
        this.SCROLLS.clear();
        if (tag.contains("scrolls")) {
            CompoundTag scrolls = tag.getCompound("scrolls");
            for (String scrollId : scrolls.getAllKeys()) {
                ListTag scrollListTag = scrolls.getList(scrollId, Tag.TAG_COMPOUND);
                for (int i = 0; i < scrollListTag.size(); i++) {
                    Object2IntMap<String> IdLevelMap = new Object2IntOpenHashMap<>();
                    CompoundTag levelAndCountTag = scrollListTag.getCompound(i);
                    int level = levelAndCountTag.getInt("Level");
                    int count = levelAndCountTag.getInt("Count");
                    IdLevelMap.put(scrollId, level);
                    SCROLLS.put(IdLevelMap, count);
                }
            }
        }
    }

    public void saveScrollData(CompoundTag tag) {
        CompoundTag scrolls = new CompoundTag();
        for (Object2IntMap.Entry<Object2IntMap<String>> e : SCROLLS.object2IntEntrySet()) {
            for (Object2IntMap.Entry<String> entry : e.getKey().object2IntEntrySet()) {
                String scrollId = entry.getKey();
                int level = entry.getIntValue();
                int count = e.getIntValue();

                ListTag scrollListTag = scrolls.contains(scrollId) ? scrolls.getList(scrollId, Tag.TAG_COMPOUND) : new ListTag();
                CompoundTag levelAndCountTag = new CompoundTag();
                levelAndCountTag.putInt("Level", level);
                levelAndCountTag.putInt("Count", count);
                scrollListTag.add(levelAndCountTag);
                scrolls.put(scrollId, scrollListTag);
            }
        }
        tag.put("scrolls", scrolls);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadScrollData(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveScrollData(tag);
    }

    public CompoundTag createScrollsSyncTag() {
        CompoundTag tag = new CompoundTag();
        saveScrollData(tag);
        return tag;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        syncToClients(); // 変更があったら自動で同期
    }

    public void syncToClients() {
        if (this.level != null && !this.level.isClientSide) {
            // このブロックエンティティの周辺（またはチャンクをトラッキングしているプレイヤー、あるいはメニューを開いているプレイヤー）にパケットを送信
            CompoundTag syncTag = this.createScrollsSyncTag();

            // 簡単かつ確実に同期させるため、ワールド内のプレイヤーを走査してメニューが開いている、または近いプレイヤーに送る
            // または ServerPlayer なら PacketDistributor.sendToPlayersTrackingChunk を使用することも可能です
            for (net.minecraft.world.entity.player.Player player : this.level.players()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    // コンテナメニューを開いていて、かつ対象のブロックposと一致している場合
                    if (serverPlayer.containerMenu instanceof com.mumu17.scrollshelf.shelf.gui.ScrollShelfMenu menu) {
                        if (menu.getBlockPos().equals(this.getBlockPos())) {
                            PacketDistributor.sendToPlayer(
                                    serverPlayer,
                                    new SyncShelfScrollsPayload(this.getBlockPos(), syncTag)
                            );
                        }
                    }
                }
            }
        }
    }

    private final IItemHandler itemHandler = new IItemHandler() {

        private record ScrollKey(String id, int level) {}

        private List<ScrollKey> getAvailableScrolls() {
            List<ScrollKey> list = new ArrayList<>();
            for (Object2IntMap.Entry<Object2IntMap<String>> e : SCROLLS.object2IntEntrySet()) {
                for (Object2IntMap.Entry<String> entry : e.getKey().object2IntEntrySet()) {
                    int count = e.getIntValue();
                    if (count > 0) {
                        list.add(new ScrollKey(entry.getKey(), entry.getIntValue())); // レベルとカウントの格納構造に依存
                    }
                }
            }
            return list;
        }

        @Override
        public int getSlots() {
            // 最低限の搬入スロット枠＋現在ある種類の数
            return Math.max(1, getAvailableScrolls().size() + 1);
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            var scrolls = getAvailableScrolls();
            if (slot >= 0 && slot < scrolls.size()) {
                ScrollKey key = scrolls.get(slot);
                ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(SpellRegistry.getSpell(key.id()), key.level(), scroll);

                // カウントの設定
                Object2IntMap<String> map = new Object2IntOpenHashMap<>();
                map.put(key.id(), key.level());
                int count = SCROLLS.getInt(map);
                scroll.setCount(Math.min(count, scroll.getMaxStackSize()));
                return scroll;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.getItem() instanceof Scroll && ISpellContainer.isSpellContainer(stack)) {
                if (!simulate) {
                    boolean success = depositScroll(stack);
                    if (success) {
                        setChanged();
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY; // シミュレート時は成功とみなす
                }
            }
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            var scrolls = getAvailableScrolls();
            if (slot >= 0 && slot < scrolls.size()) {
                ScrollKey key = scrolls.get(slot);
                Object2IntMap<String> map = new Object2IntOpenHashMap<>();
                map.put(key.id(), key.level());
                int currentCount = SCROLLS.getInt(map);

                if (currentCount <= 0) return ItemStack.EMPTY;

                int extractCount = Math.min(amount, Math.min(currentCount, 64));
                ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(SpellRegistry.getSpell(key.id()), key.level(), scroll);
                scroll.setCount(extractCount);

                if (!simulate) {
                    for (int i = 0; i < extractCount; i++) {
                        deleteScroll(key.id(), key.level());
                    }
                    setChanged();
                }
                return scroll;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof Scroll && ISpellContainer.isSpellContainer(stack);
        }
    };

    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }
}
