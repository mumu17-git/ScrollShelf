package com.mumu17.scrollshelf.shelf.gui;

import com.mumu17.scrollshelf.ModNetworks;
import com.mumu17.scrollshelf.ScrollShelf;
import com.mumu17.scrollshelf.shelf.packet.ExtractScrollPayload;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ScrollShelfScreen extends AbstractContainerScreen<ScrollShelfMenu> {

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(ScrollShelf.MODID, "textures/gui/container/scroll_shelf.png");

    private EditBox searchBox;
    private final List<CustomImageButton> spellButtons = new ArrayList<>();
    private final Map<String, Integer> savedLevelIndices = new HashMap<>();

    private int lastScrollsVersion = -1;

    private String searchText = "";

    private static final int LIST_X = 159, LIST_Y = 19, LIST_W = 16, LIST_H = 99;
    private static final int ICON_COLS = 8;
    private static final int ICON_SIZE = 16;
    private static final int VISIBLE_ROWS = 6;

    private int scrollRows = 0;
    private int totalRows = 0;

    // 例: GUIテクスチャ上のスクロールバー定義
    private static final int SCROLL_X = 160;      // GUI内X
    private static final int SCROLL_Y = 20;       // GUI内Y(上端)
    private static final int SCROLL_H = 98;       // トラック高さ
    private static final int KNOB_W = 12;
    private static final int KNOB_H = 15;

    // 通常
    private static final int KNOB_U = 207;
    private static final int KNOB_V = 36;
    // ドラッグ中
    private static final int KNOB_DRAG_U = 219;
    private static final int KNOB_DRAG_V = 36;

    private boolean draggingScrollbar = false;
    private int dragOffsetY = 0; // つまみ上端からクリック位置までの差

    public ScrollShelfScreen(ScrollShelfMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);

        this.imageWidth = 205;  // PNG の幅
        this.imageHeight = 208; // PNG の高さ
        this.titleLabelX = 5;
        this.titleLabelY = 5;
        this.inventoryLabelX = 22;
        this.inventoryLabelY = 117;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.spellButtons.clear();

        int x = this.leftPos + 83;
        int y = this.topPos + 4;

        this.searchBox = new EditBox(
                this.font,
                x, y,
                90, 12,
                Component.literal("Search")
        );

        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(true);
        this.searchBox.setValue("");
        this.searchBox.setResponder(text -> {
            this.searchText = text.toLowerCase();
            this.scrollRows = 0;
            rebuildSpellButtons();
        });

        this.addRenderableWidget(this.searchBox);
        this.createSpellButtons();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, this.leftPos, this.topPos, 1, 1, this.imageWidth, this.imageHeight);

        int x1 = this.leftPos + LIST_X;
        int y1 = this.topPos + LIST_Y;
        int x2 = x1 + LIST_W;
        int y2 = y1 + LIST_H;

        g.enableScissor(x1, y1, x2, y2);
        g.disableScissor();

        int maxScrollRows = Math.max(0, this.totalRows - VISIBLE_ROWS);
        float t = maxScrollRows == 0 ? 0.0F : (float) this.scrollRows / (float) maxScrollRows;

        int travel = SCROLL_H - KNOB_H;
        int knobY = this.topPos + SCROLL_Y + Math.round(t * travel);
        int knobX = this.leftPos + SCROLL_X;

        int u = this.draggingScrollbar ? KNOB_DRAG_U : KNOB_U;
        int v = this.draggingScrollbar ? KNOB_DRAG_V : KNOB_V;

        g.blit(BG, knobX, knobY, u, v, KNOB_W, KNOB_H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.searchBox.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
        this.createSpellButtonTooltips(g, mouseX, mouseY);
    }

    private void createSpellButtons() {
        final int xOffset = this.leftPos + 29;
        final int yOffset = this.topPos + 20;

        record ButtonData(AbstractSpell spell, List<Integer> levels, List<Integer> craftableLevels) {}
        List<ButtonData> allButtons = new ArrayList<>();

        for (AbstractSpell spell : SpellRegistry.getEnabledSpells()) {
            ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(spell, spell.getMinLevel(), scroll);
            String spellName = ((Scroll) scroll.getItem()).getName(scroll).getString().toLowerCase();
            String spellType = spell.getSchoolType().getDisplayName().getString().toLowerCase();
            if (!spellName.contains(searchText) && !spellType.contains(searchText)) {
                continue;
            }

            if (this.menu.getBlockEntity() == null || !this.menu.matchesFilter(spell.getSpellId())) {
                continue;
            }

            List<Integer> levels = new ArrayList<>();
            for (int spellLevel = spell.getMinLevel(); spellLevel <= spell.getMaxLevel(); spellLevel++) {
                if (this.menu.getBlockEntity() != null
                        && this.menu.getBlockEntity().canExtract(spell.getSpellId(), spellLevel)) {
                    levels.add(spellLevel);
                }
            }
            List<Integer> craftableLevels = new ArrayList<>();
            if (!levels.isEmpty()) {
                for (int spellLevel = levels.get(0); spellLevel <= spell.getMaxLevel(); spellLevel++) {
                    if (this.menu.getBlockEntity() != null && this.minecraft != null && this.minecraft.player != null
                            && this.menu.getBlockEntity().canCraft(spell, getBaseLevel(levels, spellLevel), spellLevel, this.minecraft.player.getInventory().items)) {
                        craftableLevels.add(spellLevel);
                    }
                }
            }

            if (!levels.isEmpty()) {
                allButtons.add(new ButtonData(spell, levels, craftableLevels));
            }
        }

        this.totalRows = (allButtons.size() + ICON_COLS - 1) / ICON_COLS;
        int maxScrollRows = Math.max(0, this.totalRows - VISIBLE_ROWS);
        this.scrollRows = Math.max(0, Math.min(this.scrollRows, maxScrollRows));

        int startIndex = this.scrollRows * ICON_COLS;
        int endIndex = Math.min(allButtons.size(), startIndex + (VISIBLE_ROWS * ICON_COLS));

        int visibleIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ButtonData data = allButtons.get(i);

            int x = xOffset + (visibleIndex % ICON_COLS) * ICON_SIZE;
            int y = yOffset + (visibleIndex / ICON_COLS) * ICON_SIZE;

            int defaultLevel = savedLevelIndices.getOrDefault(data.spell().getSpellId(), data.levels().get(0));

            CustomImageButton button = this.addRenderableWidget(new CustomImageButton(
                    this.menu,
                    x, y,
                    ICON_SIZE, ICON_SIZE,
                    data.spell().getSpellIconResource(),
                    btn -> onIconClicked(
                            data.spell(),
                            getBaseLevel(data.levels(), (savedLevelIndices.containsKey(data.spell().getSpellId()) ? savedLevelIndices.getOrDefault(data.spell().getSpellId(), data.spell().getMinLevel()) : btn instanceof CustomImageButton customBtn ? customBtn.level : data.levels().get(0))),
                            (savedLevelIndices.containsKey(data.spell().getSpellId()) ? savedLevelIndices.getOrDefault(data.spell().getSpellId(), data.spell().getMinLevel()) : btn instanceof CustomImageButton customBtn ? customBtn.level : data.levels().get(0)),
                            btn instanceof CustomImageButton customBtn && !customBtn.levels.getOrDefault((savedLevelIndices.containsKey(data.spell().getSpellId()) ? savedLevelIndices.getOrDefault(data.spell().getSpellId(), data.spell().getMinLevel()) : customBtn.level), false)
                    ),
                    data.spell(),
                    data.levels(),
                    data.craftableLevels(),
                    defaultLevel,
                    newLevel -> savedLevelIndices.put(data.spell().getSpellId(), newLevel)
            ));
            this.spellButtons.add(button);
            visibleIndex++;
        }
    }

    public int getBaseLevel(List<Integer> levels, int level) {
        if (levels.isEmpty()) return level;
        int baseLevel = levels.get(0);
        for (int l : levels) {
            if (l < level && l > baseLevel) {
                baseLevel = l;
            }
        }
        return baseLevel;
    }

    public void rebuildSpellButtons() {
        for (CustomImageButton b : this.spellButtons) {
            savedLevelIndices.put(b.spell.getSpellId(), Math.max(b.level, b.spell.getMinLevel()));
        }

        for (CustomImageButton b : this.spellButtons) {
            this.removeWidget(b);
        }
        this.spellButtons.clear();
        this.createSpellButtons();
    }

    private void createSpellButtonTooltips(GuiGraphics g, int mouseX, int mouseY) {
        for (CustomImageButton button : this.spellButtons) {
            if (button.isHoveredOrFocused()) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(button.itemStack.getItem() instanceof Scroll scroll ? scroll.getName(button.itemStack).copy().withStyle(ChatFormatting.YELLOW) : button.itemStack.getDisplayName().copy().withStyle(ChatFormatting.YELLOW));
                tooltip.addAll(button.getTooltipLines());
                g.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        int v = this.menu.getScrollsVersion();
        if (v != this.lastScrollsVersion) {
            this.lastScrollsVersion = v;
            this.rebuildSpellButtons();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        for (CustomImageButton button : this.spellButtons) {
            if (button.isHoveredOrFocused()) {
                return button.onMouseScrolled(scrollY);
            }
        }
        int x = this.leftPos + LIST_X;
        int y = this.topPos + LIST_Y;
        if (mouseX >= x && mouseX < x + LIST_W && mouseY >= y && mouseY < y + LIST_H) {
            int maxRows = Math.max(0, (totalRows - VISIBLE_ROWS));
            if (scrollY > 0) scrollRows = Math.max(0, scrollRows - 1);
            if (scrollY < 0) scrollRows = Math.min(maxRows, scrollRows + 1);
            rebuildSpellButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.searchBox.setFocused(true);
            return true;
        }
        this.searchBox.setFocused(false);

        int knobX = getKnobX();
        int knobY = getKnobY();
        if (button == 0
                && mouseX >= knobX && mouseX < knobX + KNOB_W
                && mouseY >= knobY && mouseY < knobY + KNOB_H) {
            this.draggingScrollbar = true;
            this.dragOffsetY = (int) mouseY - knobY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.draggingScrollbar || button != 0) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        int trackTop = this.topPos + SCROLL_Y;
        int travel = SCROLL_H - KNOB_H;
        if (travel <= 0) return true;

        int rawKnobY = (int) mouseY - this.dragOffsetY;
        int clampedKnobY = Math.max(trackTop, Math.min(trackTop + travel, rawKnobY));

        float t = (float) (clampedKnobY - trackTop) / (float) travel;
        int maxScrollRows = Math.max(0, this.totalRows - VISIBLE_ROWS);
        int newScrollRows = Math.round(t * maxScrollRows);

        if (newScrollRows != this.scrollRows) {
            this.scrollRows = newScrollRows;
            rebuildSpellButtons();
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused()
                && this.minecraft != null
                && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true; // Eキーを消費して閉じない
        }

        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void onIconClicked(AbstractSpell spell, int baseLevel, int level, boolean needCraft) {
        if (this.menu.getBlockEntity() == null) {
            return;
        }
        ExtractScrollPayload payload = new ExtractScrollPayload(this.menu.getBlockEntity().getBlockPos(), spell.getSpellId(), baseLevel, level, needCraft);
        ModNetworks.sendToServer(payload);
        rebuildSpellButtons();
    }

    private int getKnobX() { return this.leftPos + SCROLL_X; }

    private int getKnobY() {
        int maxScrollRows = Math.max(0, this.totalRows - VISIBLE_ROWS);
        float t = maxScrollRows == 0 ? 0.0F : (float) this.scrollRows / (float) maxScrollRows;
        int travel = SCROLL_H - KNOB_H;
        return this.topPos + SCROLL_Y + Math.round(t * travel);
    }

    static class CustomImageButton extends Button {

        private final ScrollShelfMenu menu;
        private final ResourceLocation texture;
        private final AbstractSpell spell;
        private int level;
        private int count;
        private final List<Component> tooltipLines;
        private final Map<Integer, Boolean> levels; // true: available, false: craftable
        private int levelIndex = 0;
        private ItemStack itemStack;
        private final Consumer<Integer> onLevelChanged;

        public CustomImageButton(ScrollShelfMenu menu, int x, int y, int width, int height,
                                 ResourceLocation texture,
                                 OnPress onPress,
                                 AbstractSpell spell,
                                 List<Integer> levels,
                                 List<Integer> craftableLevels,
                                 int defaultViewLevel,
                                 Consumer<Integer> onLevelChanged) {

            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.menu = menu;
            this.texture = texture;
            this.spell = spell;
            this.onLevelChanged = onLevelChanged;
            this.levels = new HashMap<>();
            for (Integer level : craftableLevels) {
                this.levels.put(level, false);
            }
            for (Integer level : levels) {
                this.levels.put(level, true);
            }
            Object[] sortMap = this.levels.keySet().toArray();
            Arrays.sort(sortMap);
            int l = 0;
            for (int i = defaultViewLevel; i >= spell.getMinLevel(); i--) {
                if (this.levels.containsKey(i)) {
                    l = this.levels.keySet().stream().toList().indexOf(i);
                    break;
                }
            }
            if (l <= 0) {
                for (int i = defaultViewLevel; i <= spell.getMaxLevel(); i++) {
                    if (this.levels.containsKey(i)) {
                        l = this.levels.keySet().stream().toList().indexOf(i);
                        break;
                    }
                }
            }
            setLevelAndCount(l);
            notifyLevelChanged();
            this.tooltipLines = new ArrayList<>();
            setTooltipLines();
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, 16, 16);
        }

        public List<Component> getTooltipLines() {
            return tooltipLines;
        }

        public void setTooltipLines() {
            ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(this.spell, this.level, scroll);
            this.itemStack = scroll.copy();
            List<Component> lines = TooltipsUtils.formatScrollTooltip(scroll, Minecraft.getInstance().player);
            this.tooltipLines.clear();
            this.tooltipLines.addAll(lines);
            if (this.count > 0) {
                Component countLine = Component.translatable("tooltip.scrollshelf.scroll_count").append(String.valueOf(this.count)).withStyle(ChatFormatting.GRAY);
                this.tooltipLines.add(countLine);
            } else {
                Component craftableLine = Component.translatable("tooltip.scrollshelf.scroll_craftable").withStyle(ChatFormatting.GRAY);
                this.tooltipLines.add(craftableLine);
            }
        }

        public boolean onMouseScrolled(double scrollY) {
            if (scrollY > 0) {
                this.nextLevelIndex();
            } else if (scrollY < 0) {
                this.prevLevelIndex();
            }
            setTooltipLines();
            return true;
        }

        public void nextLevelIndex() {
            if (levels.isEmpty()) return;
            levelIndex = (levelIndex + 1) % levels.size();
            setLevelAndCount(levelIndex);
            notifyLevelChanged();
        }

        public void prevLevelIndex() {
            if (levels.isEmpty()) return;
            levelIndex = (levelIndex - 1 + levels.size()) % levels.size();
            setLevelAndCount(levelIndex);
            notifyLevelChanged();
        }

        public void setLevelAndCount(int index) {
            if (index >= 0 && index < levels.size()) {
                levelIndex = index;
                level = levels.keySet().stream().toList().get(levelIndex);
                if (levels.get(level)) {
                    Object2IntMap<String> idLevelMap = new Object2IntOpenHashMap<>();
                    idLevelMap.put(spell.getSpellId(), level);
                    count = this.menu.getScrolls().getInt(idLevelMap);
                } else {
                    count = 0;
                }
            }
        }

        private void notifyLevelChanged() {
            if (this.onLevelChanged != null) {
                this.onLevelChanged.accept(this.level);
            }
        }
    }
}
