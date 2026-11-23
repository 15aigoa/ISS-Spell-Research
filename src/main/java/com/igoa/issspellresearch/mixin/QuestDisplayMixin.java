// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.mixin;

import com.igoa.issspellresearch.client.SpellDisplay;
import com.igoa.issspellresearch.config.ModConfigs;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 研究レポート報酬を持つクエストの、既定のタイトルとアイコンを、魔法の名前とアイコンにする(クライアント側)。
 *
 * FTB Questsは、タイトルとアイコンを、手で設定していないときだけ getAltTitle / getAltIcon を使う
 * (標準では、最初のタスクのタイトルと、タスクのアイコン)。ここだけを差し替えるので、
 * タイトルやアイコンを手で設定したクエストや、研究報酬のないクエストには、影響しない。
 *
 * 装飾だけの変更なので、狙うメソッドが見つからなくても(require = 0)、ゲームは止めない。
 */
@Mixin(value = Quest.class, remap = false)
public abstract class QuestDisplayMixin {

    @Inject(method = "getAltTitle", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void issspell_research$title(CallbackInfoReturnable<Component> cir) {
        if (!ModConfigs.QUEST_USES_SPELL_DISPLAY.get()) {
            return;
        }
        Component title = SpellDisplay.questTitle((Quest) (Object) this);
        if (title != null) {
            cir.setReturnValue(title);
        }
    }

    @Inject(method = "getAltIcon", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void issspell_research$icon(CallbackInfoReturnable<Icon> cir) {
        if (!ModConfigs.QUEST_USES_SPELL_DISPLAY.get()) {
            return;
        }
        Icon icon = SpellDisplay.questIcon((Quest) (Object) this);
        if (icon != null) {
            cir.setReturnValue(icon);
        }
    }
}
