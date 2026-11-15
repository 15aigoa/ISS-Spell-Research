// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.mixin;

import com.igoa.issspellresearch.item.ResearchReportItem;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scroll Forgeの画面(クライアント)側。魔法リストを「フォーカスの学派」ではなく「研究レポートに記録された魔法」から作る。
 */
@Mixin(ScrollForgeScreen.class)
public abstract class ScrollForgeScreenMixin extends AbstractContainerScreen<ScrollForgeMenu> {
    @Shadow(remap = false) private ItemStack[] oldMenuSlots;

    protected ScrollForgeScreenMixin(ScrollForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** generateSpellList内の「フォーカスのタグを持つか」の判定を、「研究レポートか」に置き換える。 */
    // メソッド名(generateSpellList)はISS独自で難読化されていないので remap = false。
    // ただし @At の remap は「インジェクター側の remap」を既定値として引き継ぐため、
    // バニラの ItemStack.is(...) を狙う @At には remap = true を明示しないと、本番(SRG名)で見つからず起動に失敗する。
    @Redirect(method = "generateSpellList", remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z", remap = true))
    private boolean issspell_research$isReport(ItemStack stack, TagKey<Item> tag) {
        return ResearchReportItem.hasSpells(stack);
    }

    /** generateSpellList内の「学派の魔法一覧」を、「研究レポートの魔法一覧」に置き換える。 */
    @Redirect(method = "generateSpellList", remap = false,
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toList()Ljava/util/List;", remap = false))
    private List<AbstractSpell> issspell_research$reportSpells(Stream<AbstractSpell> original) {
        ItemStack report = this.menu.getFocusSlot().getItem();
        List<AbstractSpell> spells = new ArrayList<>();
        for (ResourceLocation id : ResearchReportItem.getSpells(report)) {
            AbstractSpell spell = SpellRegistry.getSpell(id);
            if (spell != null && !spell.equals(SpellRegistry.none()) && spell.allowCrafting()) {
                spells.add(spell);
            }
        }
        return spells;
    }

    /**
     * レポートはどれも同じItemなので、元の「Itemが変わったか」だけの判定だと、
     * 別のレポートに差し替えたときに魔法リストが更新されない。NBT(魔法の中身)も比較する。
     */
    @Inject(method = "menuSlotsChanged", at = @At("HEAD"), cancellable = true, remap = false)
    private void issspell_research$menuSlotsChanged(CallbackInfoReturnable<Boolean> cir) {
        ItemStack ink = this.menu.getInkSlot().getItem();
        ItemStack paper = this.menu.getBlankScrollSlot().getItem();
        ItemStack focus = this.menu.getFocusSlot().getItem();
        boolean changed = ink.getItem() != this.oldMenuSlots[0].getItem()
                || !ItemStack.isSameItemSameTags(focus, this.oldMenuSlots[2]);
        if (changed) {
            this.oldMenuSlots = new ItemStack[]{ink, paper, focus};
        }
        cir.setReturnValue(changed);
    }
}
