// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.menu;

import com.igoa.issspellresearch.data.ResearchPointsData;
import com.igoa.issspellresearch.registry.ModBlocks;
import com.igoa.issspellresearch.registry.ModMenus;
import com.igoa.issspellresearch.reissue.ReissueEntry;
import com.igoa.issspellresearch.reissue.ReissueService;
import com.igoa.issspellresearch.util.EnchantPointCalculator;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 研究テーブルのメニュー。
 * - タブ1「研究ポイント」: エンチャント本の投入口(縦3×横4 = 12スロット)と、本 → 研究ポイントの一括変換(ボタン0)
 * - タブ2「再発行」: 再発行できる研究の一覧(開いた時点のもの)。ボタン 100 + 番号 で、その研究のレポートを再発行する
 * 残高(long)は、16bitずつ4個のDataSlotに分けてクライアントへ同期する。
 */
public class ResearchTableMenu extends AbstractContainerMenu {
    public static final int BUTTON_CONVERT = 0;
    public static final int BUTTON_REISSUE_BASE = 100;

    /** 本の投入口: 横4 × 縦3 = 12スロット。 */
    public static final int BOOK_COLS = 4;
    public static final int BOOK_ROWS = 3;
    public static final int BOOK_SLOTS = BOOK_COLS * BOOK_ROWS;
    /** 本のスロットの左上の座標(画面左上からの相対)。 */
    public static final int BOOK_X = 12;
    public static final int BOOK_Y = 34;

    private static final int INV_START = BOOK_SLOTS;          // 12..38 = メインインベントリ
    private static final int INV_END = BOOK_SLOTS + 27;
    private static final int HOTBAR_END = BOOK_SLOTS + 36;    // 39..47 = ホットバー

    private final Container bookContainer = new SimpleContainer(BOOK_SLOTS);
    private final ContainerLevelAccess access;
    private final Player player;
    private final DataSlot[] balanceParts = new DataSlot[4];
    private final List<ReissueEntry> reissueEntries;

    /** クライアント専用: 「再発行」タブを表示中か。true の間は、本のスロットを無効(非表示)にする。 */
    public boolean reissueTab;

    public ResearchTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access, List<ReissueEntry> reissueEntries) {
        super(ModMenus.RESEARCH_TABLE.get(), containerId);
        this.access = access;
        this.player = inventory.player;
        this.reissueEntries = List.copyOf(reissueEntries);

        for (int i = 0; i < BOOK_SLOTS; i++) {
            int col = i % BOOK_COLS;
            int row = i / BOOK_COLS;
            this.addSlot(new Slot(bookContainer, i, BOOK_X + col * 18, BOOK_Y + row * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(Items.ENCHANTED_BOOK);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return !ResearchTableMenu.this.reissueTab;
                }
            });
        }

        // プレイヤーインベントリ(画面の高さ222: メイン y=140 / ホットバー y=198)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 198));
        }

        for (int i = 0; i < balanceParts.length; i++) {
            balanceParts[i] = this.addDataSlot(DataSlot.standalone());
        }
        if (player instanceof ServerPlayer serverPlayer) {
            updateBalance(serverPlayer);
        }
    }

    /** 画面を開いた時点の、再発行できる研究の一覧(サーバーとクライアントで同じ順序)。 */
    public List<ReissueEntry> getReissueEntries() {
        return reissueEntries;
    }

    private void updateBalance(ServerPlayer serverPlayer) {
        long balance = ResearchPointsData.get(serverPlayer.server).get(serverPlayer.getUUID());
        for (int i = 0; i < balanceParts.length; i++) {
            balanceParts[i].set((int) ((balance >>> (16 * i)) & 0xFFFFL));
        }
    }

    /** クライアント側で、同期された残高を復元する。 */
    public long getBalance() {
        long value = 0L;
        for (int i = 0; i < balanceParts.length; i++) {
            value |= ((long) (balanceParts[i].get() & 0xFFFF)) << (16 * i);
        }
        return value;
    }

    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer serverPlayer) {
            updateBalance(serverPlayer);
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (id == BUTTON_CONVERT) {
            return convert(clicker);
        }
        if (id >= BUTTON_REISSUE_BASE) {
            return reissue(clicker, id - BUTTON_REISSUE_BASE);
        }
        return false;
    }

    /** 12スロットの本を、まとめて研究ポイントに変換する。ポイントにならない本(エンチャントなし)は、スロットに残す。 */
    private boolean convert(Player clicker) {
        if (!(clicker instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        long total = 0L;
        int books = 0;
        boolean anyBook = false;
        for (int i = 0; i < BOOK_SLOTS; i++) {
            ItemStack book = bookContainer.getItem(i);
            if (book.isEmpty()) {
                continue;
            }
            anyBook = true;
            long perBook = EnchantPointCalculator.calculate(book);
            if (perBook <= 0L) {
                continue;
            }
            total += perBook * book.getCount();
            books += book.getCount();
            bookContainer.setItem(i, ItemStack.EMPTY);
        }
        if (books == 0) {
            if (anyBook) {
                serverPlayer.displayClientMessage(Component.translatable("issspell_research.table.no_points"), true);
            }
            return false;
        }
        ResearchPointsData.get(serverPlayer.server).add(serverPlayer.getUUID(), total);
        serverPlayer.displayClientMessage(Component.translatable("issspell_research.table.converted.multi", total, books), true);
        updateBalance(serverPlayer);
        return true;
    }

    private boolean reissue(Player clicker, int index) {
        if (index < 0 || index >= reissueEntries.size()) {
            return false;
        }
        if (!(clicker instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        ReissueService.tryReissue(serverPlayer, reissueEntries.get(index).spell());
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index < BOOK_SLOTS) {
                if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(Items.ENCHANTED_BOOK)) {
                if (!this.moveItemStackTo(stack, 0, BOOK_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < INV_END) {
                if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.bookContainer));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.RESEARCH_TABLE.get());
    }
}
