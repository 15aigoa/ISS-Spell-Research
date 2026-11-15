// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.mixin;

import com.igoa.issspellresearch.item.ResearchReportItem;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scroll Forgeの「結果の計算」を、研究レポート方式に置き換える。
 * 条件: 紙 + インク + 研究レポート(選んだ魔法が記録済み) + インクのレア度が魔法の最低レア度以上。
 * 学派フォーカスによる従来の作り方は行わない。
 */
@Mixin(ScrollForgeMenu.class)
public abstract class ScrollForgeMenuMixin {
    @Shadow(remap = false) @Final private Slot inkSlot;
    @Shadow(remap = false) @Final private Slot blankScrollSlot;
    @Shadow(remap = false) @Final private Slot focusSlot;
    @Shadow(remap = false) @Final private Slot resultSlot;
    @Shadow(remap = false) private AbstractSpell spellRecipeSelection;

    // ISS独自のメソッド(難読化されていない)なので、remap = false にする
    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void issspell_research$setupResultSlot(AbstractSpell spell, CallbackInfo ci) {
        ItemStack paper = this.blankScrollSlot.getItem();
        ItemStack ink = this.inkSlot.getItem();
        ItemStack report = this.focusSlot.getItem();
        ItemStack result = ItemStack.EMPTY;

        if (!paper.isEmpty() && !ink.isEmpty() && !report.isEmpty()
                && !spell.equals(SpellRegistry.none())
                && spell.allowCrafting()
                && ResearchReportItem.containsSpell(report, spell.getSpellId())
                && paper.is(Items.PAPER)
                && ink.getItem() instanceof InkItem inkItem
                && inkItem.getRarity().compareRarity(SpellRarity.values()[spell.getMinRarity()]) >= 0) {
            result = new ItemStack(ItemRegistry.SCROLL.get());
            result.setCount(1);
            ISpellContainer.createScrollContainer(spell, spell.getMinLevelForRarity(inkItem.getRarity()), result);
        }

        if (!ItemStack.matches(result, this.resultSlot.getItem())) {
            if (result.isEmpty()) {
                this.spellRecipeSelection = SpellRegistry.none();
            }
            this.resultSlot.set(result);
        }
        ci.cancel();
    }
}
