// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.quest;

import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import java.util.HashSet;
import java.util.Set;

/** 「研究チャプター」の判定。研究レポート報酬を1つ以上含むクエストがあるチャプターを、研究チャプターとみなす。 */
public final class ResearchChapters {
    private ResearchChapters() {}

    public static boolean isResearchChapter(Chapter chapter) {
        if (chapter == null) {
            return false;
        }
        for (Quest quest : chapter.getQuests()) {
            for (Reward reward : quest.getRewards()) {
                if (reward instanceof ResearchReportReward) {
                    return true;
                }
            }
        }
        return false;
    }

    /** クエストファイル内の、研究チャプターのID一覧。 */
    public static Set<Long> researchChapterIds(BaseQuestFile file) {
        Set<Long> ids = new HashSet<>();
        for (Chapter chapter : file.getAllChapters()) {
            if (isResearchChapter(chapter)) {
                ids.add(chapter.id);
            }
        }
        return ids;
    }
}
