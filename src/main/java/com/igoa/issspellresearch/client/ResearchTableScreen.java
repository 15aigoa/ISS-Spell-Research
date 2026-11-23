// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.client;

import com.igoa.issspellresearch.menu.ResearchTableMenu;
import com.igoa.issspellresearch.registry.ModItems;
import com.igoa.issspellresearch.reissue.ReissueEntry;
import com.igoa.issspellresearch.reissue.ReissueRequirement;
import dev.ftb.mods.ftbquests.quest.Chapter;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 研究テーブルの画面。クリエイティブタブのように、メイン画面の左側に縦にタブが並ぶ。
 *  - タブ1「研究ポイント」(エンチャント本のアイコン): 本(12スロット) → 研究ポイントの一括変換 / 研究チャプターを開く
 *  - タブ2「再発行」(研究レポートのアイコン): 再発行できる研究の一覧(名前・学派で検索)から選んで、素材を払って再発行する
 * テクスチャ画像は使わず、塗りつぶしで描く(仮デザイン)。
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final int MAX_CHAPTER_BUTTONS = 3;

    // ---- 左側のタブ ----
    private static final int TAB_W = 28;
    private static final int TAB_H = 28;
    private static final int TAB_GAP = 2;
    private static final int TAB_TOP = 8;
    private static final int TAB_COUNT = 2;

    // ---- 「再発行」タブの一覧の配置(画面左上からの相対座標) ----
    private static final int LIST_X = 8;
    private static final int LIST_Y = 37;
    private static final int LIST_W = 152;
    private static final int ROW_H = 18;
    private static final int ROWS = 4;
    private static final int COST_Y = 112;

    private final ItemStack pointsTabIcon = new ItemStack(Items.ENCHANTED_BOOK);
    /** 再発行タブのアイコン: 学派が evocation の研究レポート。見つからなければ既定の紫色。 */
    private final ItemStack reissueTabIcon = evocationReportIcon();

    /**
     * evocation の学派の魔法を1つ記録した研究レポート。学派の見た目は、記録された魔法の学派で決まるため、
     * 魔法IDを決め打ちせず、登録されている魔法から選ぶ(並び順が安定するよう、IDの最小のものを使う)。
     */
    private static ItemStack evocationReportIcon() {
        return SpellRegistry.getEnabledSpells().stream()
                .filter(spell -> !spell.equals(SpellRegistry.none()) && spell.allowCrafting())
                .filter(spell -> SchoolRegistry.EVOCATION_RESOURCE.equals(spell.getSchoolType().getId()))
                .min(Comparator.comparing(spell -> spell.getSpellResource().toString()))
                .map(spell -> ModItems.createReport(spell.getSpellResource()))
                .orElseGet(() -> new ItemStack(ModItems.RESEARCH_REPORT.get()));
    }

    private List<Chapter> chapters = List.of();

    private Button convertButton;
    private final List<Button> chapterButtons = new ArrayList<>();
    private EditBox searchBox;
    private Button reissueButton;

    /** 一覧に出す行(検索で絞り込み・並べ替えたもの)。index は menu.getReissueEntries() の番号。 */
    private record Row(int index, ReissueEntry entry, AbstractSpell spell, Component name, Component school) {}

    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private int selectedIndex = -1;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 130;
    }

    @Override
    protected void init() {
        super.init();
        this.chapterButtons.clear();

        // ---- タブ1: 研究ポイント ----
        this.convertButton = this.addRenderableWidget(Button.builder(Component.translatable("issspell_research.table.convert"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ResearchTableMenu.BUTTON_CONVERT);
            }
        }).bounds(this.leftPos + 12, this.topPos + 92, 72, 18).build());

        this.chapters = ResearchChapterFinder.findResearchChapters();
        int shown = Math.min(this.chapters.size(), MAX_CHAPTER_BUTTONS);
        for (int i = 0; i < shown; i++) {
            Chapter chapter = this.chapters.get(i);
            this.chapterButtons.add(this.addRenderableWidget(
                    Button.builder(chapter.getTitle(), button -> ResearchChapterFinder.open(chapter))
                            .bounds(this.leftPos + 100, this.topPos + 34 + i * 20, 68, 18).build()));
        }

        // ---- タブ2: 再発行 ----
        String previousSearch = this.searchBox == null ? "" : this.searchBox.getValue();
        this.searchBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 20, 160, 14,
                Component.translatable("issspell_research.reissue.search"));
        this.searchBox.setHint(Component.translatable("issspell_research.reissue.search"));
        this.searchBox.setMaxLength(48);
        this.searchBox.setValue(previousSearch);
        this.searchBox.setResponder(text -> rebuildRows());
        this.addRenderableWidget(this.searchBox);

        this.reissueButton = this.addRenderableWidget(Button.builder(Component.translatable("issspell_research.reissue.button"), button -> {
            if (this.selectedIndex >= 0 && this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                        ResearchTableMenu.BUTTON_REISSUE_BASE + this.selectedIndex);
            }
        }).bounds(this.leftPos + 118, this.topPos + COST_Y, 50, 16).build());

        rebuildRows();
        updateVisibility();
    }

    private void setTab(boolean reissue) {
        if (this.menu.reissueTab == reissue) {
            return;
        }
        this.menu.reissueTab = reissue;
        updateVisibility();
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void updateVisibility() {
        boolean reissue = this.menu.reissueTab;
        this.convertButton.visible = !reissue;
        for (Button button : this.chapterButtons) {
            button.visible = !reissue;
        }
        this.searchBox.visible = reissue;
        this.reissueButton.visible = reissue;
        if (!reissue) {
            this.searchBox.setFocused(false);
        }
    }

    // ---------------------------------------------------------------- 左側のタブ

    private int tabX() {
        // メイン画面の左端に、2px重ねて接続する
        return this.leftPos - TAB_W + 2;
    }

    private int tabY(int index) {
        return this.topPos + TAB_TOP + index * (TAB_H + TAB_GAP);
    }

    private int tabAt(double mouseX, double mouseY) {
        int x = tabX();
        if (mouseX < x || mouseX >= x + TAB_W) {
            return -1;
        }
        for (int i = 0; i < TAB_COUNT; i++) {
            int y = tabY(i);
            if (mouseY >= y && mouseY < y + TAB_H) {
                return i;
            }
        }
        return -1;
    }

    private int currentTab() {
        return this.menu.reissueTab ? 1 : 0;
    }

    private void drawTab(GuiGraphics g, int index, boolean selected, int mouseX, int mouseY) {
        int x = tabX();
        int y = tabY(index);
        boolean hover = tabAt(mouseX, mouseY) == index;
        g.fill(x, y, x + TAB_W, y + TAB_H, 0xFF000000);
        int inner = selected ? 0xFFC6C6C6 : (hover ? 0xFF9A9A9A : 0xFF7A7A7A);
        // 右側はメイン画面と重なる(選択中は、メイン画面の左の縁を隠して、つなげる)
        g.fill(x + 1, y + 1, x + TAB_W, y + TAB_H - 1, inner);
        if (selected) {
            g.fill(x + 1, y + 1, x + TAB_W, y + 2, 0xFFFFFFFF);
            g.fill(x + 1, y + 1, x + 2, y + TAB_H - 1, 0xFFFFFFFF);
        }
    }

    // ---------------------------------------------------------------- 一覧

    /** 検索の文字(空白区切りの全語)を、魔法名・学派名・IDに含むものだけを、学派 → 名前の順に並べて一覧にする。 */
    private void rebuildRows() {
        this.rows.clear();
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        String[] tokens = query.isEmpty() ? new String[0] : query.split("\\s+");
        List<ReissueEntry> entries = this.menu.getReissueEntries();
        for (int i = 0; i < entries.size(); i++) {
            ReissueEntry entry = entries.get(i);
            AbstractSpell spell = SpellRegistry.getSpell(entry.spell());
            if (spell == null || spell.equals(SpellRegistry.none())) {
                continue;
            }
            Component name = spell.getDisplayName(this.minecraft == null ? null : this.minecraft.player);
            Component school = spell.getSchoolType().getDisplayName();
            String haystack = (name.getString() + " " + school.getString() + " " + entry.spell()).toLowerCase(Locale.ROOT);
            boolean matches = true;
            for (String token : tokens) {
                if (!haystack.contains(token)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                this.rows.add(new Row(i, entry, spell, name, school));
            }
        }
        this.rows.sort(Comparator.comparing((Row r) -> r.school().getString()).thenComparing(r -> r.name().getString()));
        this.scroll = Math.max(0, Math.min(this.scroll, this.rows.size() - ROWS));
        if (this.selectedIndex >= 0 && this.rows.stream().noneMatch(r -> r.index() == this.selectedIndex)) {
            this.selectedIndex = -1;
        }
    }

    private Row selectedRow() {
        for (Row row : this.rows) {
            if (row.index() == this.selectedIndex) {
                return row;
            }
        }
        return null;
    }

    private boolean canAfford(ReissueEntry entry) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        Inventory inventory = this.minecraft.player.getInventory();
        for (ReissueRequirement requirement : entry.requirements()) {
            if (requirement.countIn(inventory) < requirement.count()) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- 描画

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        Row selected = selectedRow();
        this.reissueButton.active = selected != null && canAfford(selected.entry());
        super.render(g, mouseX, mouseY, partialTick);
        if (this.menu.reissueTab) {
            renderReissueTab(g, mouseX, mouseY, selected);
        }
        this.renderTooltip(g, mouseX, mouseY);
        int hoveredTab = tabAt(mouseX, mouseY);
        if (hoveredTab >= 0) {
            g.renderTooltip(this.font, Component.translatable(hoveredTab == 0
                    ? "issspell_research.table.tab.points" : "issspell_research.table.tab.reissue"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        int current = currentTab();

        // 選択していないタブ(メイン画面の下に潜る)
        for (int i = 0; i < TAB_COUNT; i++) {
            if (i != current) {
                drawTab(g, i, false, mouseX, mouseY);
            }
        }

        // メイン画面
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, 0xFF555555);

        // 選択中のタブ(メイン画面の上に重ねて、つなげる)
        drawTab(g, current, true, mouseX, mouseY);
        g.renderItem(this.pointsTabIcon, tabX() + 5, tabY(0) + 6);
        g.renderItem(this.reissueTabIcon, tabX() + 5, tabY(1) + 6);

        for (Slot slot : this.menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
            g.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        }
        if (this.menu.reissueTab) {
            // 一覧の背景(くぼみ)
            int lx = x + LIST_X;
            int ly = y + LIST_Y;
            g.fill(lx - 1, ly - 1, lx + LIST_W + 9, ly + ROWS * ROW_H + 1, 0xFF373737);
            g.fill(lx, ly, lx + LIST_W + 8, ly + ROWS * ROW_H, 0xFF1E1E26);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        if (this.menu.reissueTab) {
            return;
        }
        g.drawString(this.font, Component.translatable("issspell_research.table.balance",
                String.format("%,d", this.menu.getBalance())), 8, 20, 0x404040, false);
        g.drawWordWrap(this.font, Component.translatable("issspell_research.table.hint"), 92, 92, 78, 0x606060);
        if (this.chapters.isEmpty()) {
            g.drawWordWrap(this.font, Component.translatable("issspell_research.table.no_chapter"), 100, 36, 68, 0x606060);
        }
    }

    private void renderReissueTab(GuiGraphics g, int mouseX, int mouseY, Row selected) {
        int lx = this.leftPos + LIST_X;
        int ly = this.topPos + LIST_Y;

        if (this.rows.isEmpty()) {
            Component message = Component.translatable(this.menu.getReissueEntries().isEmpty()
                    ? "issspell_research.reissue.none" : "issspell_research.reissue.none.filtered");
            g.drawWordWrap(this.font, message, lx + 4, ly + 6, LIST_W - 8, 0xFFAAAAAA);
        }

        // 一覧(はみ出さないように、一覧の範囲で切り取る)
        g.enableScissor(lx, ly, lx + LIST_W, ly + ROWS * ROW_H);
        Row hovered = null;
        for (int r = 0; r < ROWS; r++) {
            int i = this.scroll + r;
            if (i >= this.rows.size()) {
                break;
            }
            Row row = this.rows.get(i);
            int ry = ly + r * ROW_H;
            boolean hover = mouseX >= lx && mouseX < lx + LIST_W && mouseY >= ry && mouseY < ry + ROW_H;
            if (hover) {
                hovered = row;
            }
            boolean isSelected = row.index() == this.selectedIndex;
            g.fill(lx, ry, lx + LIST_W, ry + ROW_H - 1, isSelected ? 0xFF7A5CB5 : hover ? 0xFF4A4A5A : 0xFF2E2E38);
            g.blit(row.spell().getSpellIconResource(), lx + 1, ry + 1, 0, 0, 16, 16, 16, 16);
            TextColor schoolColor = row.school().getStyle().getColor();
            if (schoolColor != null) {
                g.fill(lx + LIST_W - 3, ry, lx + LIST_W, ry + ROW_H - 1, 0xFF000000 | schoolColor.getValue());
            }
            g.drawString(this.font, this.font.plainSubstrByWidth(row.name().getString(), LIST_W - 26),
                    lx + 20, ry + 5, 0xFFFFFFFF, false);
        }
        g.disableScissor();

        // スクロールバー
        if (this.rows.size() > ROWS) {
            int trackX = lx + LIST_W + 2;
            int trackH = ROWS * ROW_H;
            int thumbH = Math.max(8, trackH * ROWS / this.rows.size());
            int maxScroll = this.rows.size() - ROWS;
            int thumbY = ly + (trackH - thumbH) * this.scroll / maxScroll;
            g.fill(trackX, ly, trackX + 6, ly + trackH, 0xFF2E2E38);
            g.fill(trackX, thumbY, trackX + 6, thumbY + thumbH, 0xFF8B8B9B);
        }

        // 必要素材(選んでいる研究の分)
        if (selected != null) {
            int cx = this.leftPos + 8;
            int cy = this.topPos + COST_Y;
            ItemStack hoveredStack = ItemStack.EMPTY;
            for (ReissueRequirement requirement : selected.entry().requirements()) {
                Ingredient ingredient = requirement.ingredient();
                ItemStack[] options = ingredient.getItems();
                if (options.length > 0) {
                    // タグなどで複数の候補があるときは、1秒ごとに切り替えて見せる
                    ItemStack stack = options[(int) ((System.currentTimeMillis() / 1000L) % options.length)];
                    g.renderItem(stack, cx, cy);
                    boolean enough = this.minecraft != null && this.minecraft.player != null
                            && requirement.countIn(this.minecraft.player.getInventory()) >= requirement.count();
                    String countText = String.valueOf(requirement.count());
                    g.pose().pushPose();
                    g.pose().translate(0.0F, 0.0F, 200.0F);
                    g.drawString(this.font, countText, cx + 17 - this.font.width(countText), cy + 9,
                            enough ? 0xFFFFFFFF : 0xFFFF5555, true);
                    g.pose().popPose();
                    if (mouseX >= cx && mouseX < cx + 16 && mouseY >= cy && mouseY < cy + 16) {
                        hoveredStack = stack;
                    }
                }
                cx += 22;
            }
            if (!hoveredStack.isEmpty()) {
                g.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
            }
        }

        // 一覧の行のツールチップ: 魔法名 / 学派 / ID
        if (hovered != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(hovered.name());
            tooltip.add(hovered.school());
            tooltip.add(Component.literal(hovered.entry().spell().toString()).withStyle(ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    // ---------------------------------------------------------------- 入力

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tab = tabAt(mouseX, mouseY);
            if (tab >= 0) {
                setTab(tab == 1);
                return true;
            }
        }
        if (this.menu.reissueTab && button == 0) {
            int lx = this.leftPos + LIST_X;
            int ly = this.topPos + LIST_Y;
            if (mouseX >= lx && mouseX < lx + LIST_W && mouseY >= ly && mouseY < ly + ROWS * ROW_H) {
                int i = this.scroll + (int) ((mouseY - ly) / ROW_H);
                if (i >= 0 && i < this.rows.size()) {
                    this.selectedIndex = this.rows.get(i).index();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** タブは、メイン画面の外にある。タブの上のクリックを「画面の外のクリック」と見なすと、持っているアイテムを捨ててしまうので、除外する。 */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return tabAt(mouseX, mouseY) < 0 && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.menu.reissueTab && this.rows.size() > ROWS) {
            int lx = this.leftPos + LIST_X;
            int ly = this.topPos + LIST_Y;
            if (mouseX >= lx && mouseX < lx + LIST_W + 8 && mouseY >= ly && mouseY < ly + ROWS * ROW_H) {
                this.scroll = Math.max(0, Math.min(this.rows.size() - ROWS, this.scroll - (int) Math.signum(delta)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 検索欄に入力中は、インベントリのキー(E)などで画面が閉じないようにする。Escだけは閉じられる。
        if (this.menu.reissueTab && this.searchBox.isFocused() && this.searchBox.visible && keyCode != 256) {
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers) || this.searchBox.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 「再発行」タブでは、見えない本のスロットへの操作(クリック・シフトクリック)を無効にする。 */
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (this.menu.reissueTab
                && ((slot != null && slot.index < ResearchTableMenu.BOOK_SLOTS) || type == ClickType.QUICK_MOVE)) {
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }
}
