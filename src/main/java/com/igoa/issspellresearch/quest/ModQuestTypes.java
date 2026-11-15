// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.quest;

import com.igoa.issspellresearch.IssSpellResearch;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ModQuestTypes {
    private ModQuestTypes() {}

    /** タスク種別ID: issspell_research:points */
    public static TaskType POINTS;

    /** 報酬種別ID: issspell_research:research_report */
    public static RewardType REPORT_REWARD;

    /** 報酬種別ID: issspell_research:reissue_unlock */
    public static RewardType REISSUE_REWARD;

    /** 報酬種別ID: issspell_research:research_complete (研究レポート + 再発行の有効化) */
    public static RewardType COMPLETE_REWARD;

    public static void register() {
        POINTS = TaskTypes.register(
                        new ResourceLocation(IssSpellResearch.MODID, "points"),
                        ResearchPointsTask::new,
                        () -> Icon.getIcon("minecraft:item/amethyst_shard"))
                .setDisplayName(Component.translatable("issspell_research.task.points"));

        REPORT_REWARD = RewardTypes.register(
                        new ResourceLocation(IssSpellResearch.MODID, "research_report"),
                        ResearchReportReward::new,
                        () -> Icon.getIcon(IssSpellResearch.MODID + ":item/research_report"))
                .setDisplayName(Component.translatable("issspell_research.reward.research_report"));

        REISSUE_REWARD = RewardTypes.register(
                        new ResourceLocation(IssSpellResearch.MODID, "reissue_unlock"),
                        ReissueUnlockReward::new,
                        () -> Icon.getIcon(IssSpellResearch.MODID + ":item/research_report"))
                .setDisplayName(Component.translatable("issspell_research.reward.reissue_unlock"));

        COMPLETE_REWARD = RewardTypes.register(
                        new ResourceLocation(IssSpellResearch.MODID, "research_complete"),
                        ResearchCompleteReward::new,
                        () -> Icon.getIcon(IssSpellResearch.MODID + ":item/research_report"))
                .setDisplayName(Component.translatable("issspell_research.reward.research_complete"));
    }
}
