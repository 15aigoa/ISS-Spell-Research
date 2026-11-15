// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.quest;

import com.igoa.issspellresearch.data.ResearchUnlockData;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 「研究レポートを渡す + その研究の再発行を有効にする」を、1つにまとめたFTB Questsの報酬。
 * 研究が完了したクエストに、この報酬を1つ付けるだけでよい。
 *
 * ResearchReportReward を継承しているので、「研究レポート報酬を含むチャプター = 研究チャプター」の判定
 * (ResearchChapters / ResearchChapterFinder の instanceof)には、そのまま該当する。
 */
public class ResearchCompleteReward extends ResearchReportReward {

    public ResearchCompleteReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return ModQuestTypes.COMPLETE_REWARD;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        // 1. 研究レポートを渡す(魔法IDが不正な場合は、レポートは渡されず、ログに警告が出る)
        super.claim(player, notify);
        // 2. その魔法の再発行を有効にする
        ResourceLocation id = ResourceLocation.tryParse(spell);
        if (id != null) {
            ResearchUnlockData.get(player.server).enableReissue(player.getUUID(), id);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getAltTitle() {
        return Component.translatable("issspell_research.reward.research_complete.title", spell.isEmpty() ? "-" : spell);
    }
}
