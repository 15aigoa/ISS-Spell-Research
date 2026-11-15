// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.client;

import com.igoa.issspellresearch.item.ResearchReportItem;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import java.util.List;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 研究レポートの「色付け部分」の色を、記録された魔法の学派の色から決める(クライアント専用)。 */
public final class ReportColors {
    private ReportColors() {}

    /** 魔法が未記録・不明なときの色(ARGB)。 */
    public static final int DEFAULT = 0xFFA078E6;

    /** ISSの学派の表示色(学派名のテキスト色)を、アイテムの色付け用(ARGB)で返す。 */
    public static int getTint(ItemStack stack) {
        List<ResourceLocation> spells = ResearchReportItem.getSpells(stack);
        if (spells.isEmpty()) {
            return DEFAULT;
        }
        // 今は1つ目の魔法の学派の色を使う(複数魔法対応のときは、ここで選び方を決める)
        AbstractSpell spell = SpellRegistry.getSpell(spells.get(0));
        if (spell == null || spell.equals(SpellRegistry.none())) {
            return DEFAULT;
        }
        SchoolType school = spell.getSchoolType();
        TextColor color = school.getDisplayName().getStyle().getColor();
        return color == null ? DEFAULT : (0xFF000000 | color.getValue());
    }
}
