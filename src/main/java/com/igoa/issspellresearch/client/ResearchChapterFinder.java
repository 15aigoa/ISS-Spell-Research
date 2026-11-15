// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.client;

import com.igoa.issspellresearch.quest.ResearchReportReward;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import java.util.ArrayList;
import java.util.List;

/**
 * 「研究チャプター」の判定と、FTBQ画面でのチャプター表示。
 * 研究チャプター = 「研究レポート報酬」を持つクエストを1つ以上含むチャプター(設定不要で自動判定)。
 */
public final class ResearchChapterFinder {
    private ResearchChapterFinder() {}

    public static List<Chapter> findResearchChapters() {
        List<Chapter> result = new ArrayList<>();
        if (!ClientQuestFile.exists()) {
            return result;
        }
        for (Chapter chapter : ClientQuestFile.INSTANCE.getAllChapters()) {
            if (containsResearchReward(chapter)) {
                result.add(chapter);
            }
        }
        return result;
    }

    private static boolean containsResearchReward(Chapter chapter) {
        for (Quest quest : chapter.getQuests()) {
            for (Reward reward : quest.getRewards()) {
                if (reward instanceof ResearchReportReward) {
                    return true;
                }
            }
        }
        return false;
    }

    /** FTBクエスト画面を開き、指定チャプターを表示する(常に非表示のチャプターでも選択できる想定。要実機確認)。 */
    public static void open(Chapter chapter) {
        ClientQuestFile.openBookToQuestObject(chapter.id);
    }
}
