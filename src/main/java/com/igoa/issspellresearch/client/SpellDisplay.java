// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.client;

import com.igoa.issspellresearch.quest.ResearchReportReward;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * FTB Questsの画面に出す、魔法の名前とアイコン(クライアント専用)。
 * 報酬の表示と、クエストの既定のタイトル・アイコンで使う。
 */
public final class SpellDisplay {
    private SpellDisplay() {}

    /** 魔法IDの文字列から、ISSの魔法を探す。空・不正・存在しない魔法は null。 */
    @Nullable
    public static AbstractSpell spellOf(String spellId) {
        if (spellId == null || spellId.isEmpty()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(spellId);
        if (id == null) {
            return null;
        }
        AbstractSpell spell = SpellRegistry.getSpell(id);
        return spell == null || spell.equals(SpellRegistry.none()) ? null : spell;
    }

    /** 魔法の表示名(言語に応じる)。魔法が見つからないときは、IDを灰色で。IDが空なら「-」。 */
    public static Component nameOf(String spellId) {
        AbstractSpell spell = spellOf(spellId);
        if (spell != null) {
            return spell.getDisplayName(null);
        }
        if (spellId == null || spellId.isEmpty()) {
            return Component.literal("-");
        }
        return Component.literal(spellId).withStyle(ChatFormatting.GRAY);
    }

    /** 魔法本体のアイコン。魔法が見つからないときは null。 */
    @Nullable
    public static Icon iconOf(String spellId) {
        AbstractSpell spell = spellOf(spellId);
        return spell == null ? null : new ImageIcon(spell.getSpellIconResource());
    }

    /** クエストが持つ、最初の研究レポート報酬(研究完了報酬を含む)。なければ null。 */
    @Nullable
    private static ResearchReportReward reportRewardOf(Quest quest) {
        for (Reward reward : quest.getRewards()) {
            if (reward instanceof ResearchReportReward report && spellOf(report.getSpellId()) != null) {
                return report;
            }
        }
        return null;
    }

    /** 研究報酬を持つクエストの、既定のタイトル(魔法名)。該当しなければ null。 */
    @Nullable
    public static Component questTitle(Quest quest) {
        ResearchReportReward report = reportRewardOf(quest);
        return report == null ? null : nameOf(report.getSpellId());
    }

    /** 研究報酬を持つクエストの、既定のアイコン(魔法のアイコン)。該当しなければ null。 */
    @Nullable
    public static Icon questIcon(Quest quest) {
        ResearchReportReward report = reportRewardOf(quest);
        return report == null ? null : iconOf(report.getSpellId());
    }
}
