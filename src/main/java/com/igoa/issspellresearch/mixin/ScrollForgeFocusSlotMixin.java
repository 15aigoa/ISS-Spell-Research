// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.mixin;

import com.igoa.issspellresearch.item.ResearchReportItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ISS Scroll Forgeの「フォーカススロット」(ScrollForgeMenuの3番目の匿名Slot)に、研究レポートだけを置けるようにする。
 * 従来の学派フォーカスは置けなくなる。
 */
@Mixin(targets = "io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu$3")
public abstract class ScrollForgeFocusSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void issspell_research$onlyReports(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(ResearchReportItem.hasSpells(stack));
    }
}
