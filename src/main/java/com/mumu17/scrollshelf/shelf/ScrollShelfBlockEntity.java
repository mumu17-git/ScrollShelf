package com.mumu17.scrollshelf.shelf;

import com.mumu17.scrollshelf.ModBlockEntities;
import com.mumu17.scrollshelf.ScrollShelf;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

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

    public void extractScroll(String id, int level, @Nullable Player player) {
        if (canExtract(id, level)) {
            Object2IntMap<String> idLevelMap = new Object2IntOpenHashMap<>();
            idLevelMap.put(id, level);
            int currentCount = SCROLLS.getOrDefault(idLevelMap, 0);
            ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(SpellRegistry.getSpell(id), level, scroll);
            if (player != null) {
                player.addItem(scroll);
            } else if (this.level != null) {
                this.level.addFreshEntity(new ItemEntity(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), scroll));
            }
            SCROLLS.put(idLevelMap, currentCount - 1);
            this.setChanged();
        }
    }

    public boolean canExtract(String id, int level) {
        Object2IntMap<String> idLevelMap = new Object2IntOpenHashMap<>();
        idLevelMap.put(id, level);
        return this.SCROLLS.containsKey(idLevelMap) && this.SCROLLS.getInt(idLevelMap) > 0;
    }

    public void loadScrollData(CompoundTag tag) {
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
}
