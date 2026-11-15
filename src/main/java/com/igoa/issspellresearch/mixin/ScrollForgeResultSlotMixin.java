// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.mixin;

import com.igoa.issspellresearch.item.ResearchReportItem;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Scroll Forgeの「結果スロット」(ScrollForgeMenuの4番目の匿名Slot)から取り出したとき、
 * フォーカススロットを1個消す代わりに、研究レポートの使用回数を1回消費する。
 * onTake内の remove(1) 呼び出しは、順に インク(0) / 紙(1) / フォーカス(2)。
 */
@Mixin(targets = "io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu$4")
public abstract class ScrollForgeResultSlotMixin {
    @Redirect(method = "onTake",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;remove(I)Lnet/minecraft/world/item/ItemStack;",
                    ordinal = 2))
    private ItemStack issspell_research$useReport(Slot focusSlot, int amount) {
        ItemStack report = focusSlot.getItem();
        if (report.getItem() instanceof ResearchReportItem) {
            ResearchReportItem.consumeUse(report);
            // set() を通して、Scroll Forge側の「スロット変更」処理(結果の再計算・同期)を走らせる
            focusSlot.set(report.isEmpty() ? ItemStack.EMPTY : report);
            return ItemStack.EMPTY;
        }
        return focusSlot.remove(amount);
    }
}
