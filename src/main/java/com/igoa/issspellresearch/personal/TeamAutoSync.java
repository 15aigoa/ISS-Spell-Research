// SPDX-License-Identifier: MPL-2.0 AND MIT
package com.igoa.issspellresearch.personal;

import com.igoa.issspellresearch.IssSpellResearch;
import com.igoa.issspellresearch.config.ModConfigs;
import com.igoa.issspellresearch.quest.ResearchChapters;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.net.SyncTeamDataMessage;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 進行がプレイヤーごとになった状態で、<b>研究以外のクエストだけ</b>をパーティ内で自動同期する。
 * <ul>
 *   <li>同期の対象: 研究以外のチャプターで「パーティの誰かが完了したクエスト」</li>
 *   <li>同期の方法: 前提クエストが完了している人だけ、そのクエストのタスクを完了状態にする(SoloQuestsの同期ボタンと同じ処理)</li>
 *   <li>研究チャプター(研究レポート報酬を含むチャプター)は、常に同期しない</li>
 *   <li>同期するのは「完了したクエスト」のみ。途中の進行(例: 討伐 5/10)は同期しない</li>
 * </ul>
 * 同期のタイミング: ログイン時 / パーティの変更時 / クエスト完了の次のtick / コマンド /issresearch sync
 * 派生元: ftb-solo-quests (c) 2026 YamaArashi, MIT License (TeamSyncHandler の同期処理)
 */
public final class TeamAutoSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(IssSpellResearch.MODID);

    /** 完了イベントで受け取った「完了者のUUID」。次のtickでまとめて処理する(イベント処理中に進行を書き換えないため)。 */
    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

    private static boolean registered;
    private static boolean syncing;

    private TeamAutoSync() {}

    /**
     * 自動同期が有効か。個人別進行がオフ(既定・外部SoloQuests導入中を含む)・コンフィグで自動同期が無効・
     * (dedicatedServerOnlyのとき)専用サーバーでない場合は、無効。
     * 条件はどれも軽い判定(コンフィグ値の参照のみ)なので、毎回呼んでよい。
     */
    public static boolean isEnabled(ServerQuestFile file) {
        return disabledReason(file) == null;
    }

    /** 自動同期が無効な理由。有効なら null。コマンドの案内やログ用。 */
    public static String disabledReason(ServerQuestFile file) {
        if (!PersonalMode.isEnabled()) {
            return PersonalMode.reason();
        }
        if (!ModConfigs.AUTO_SYNC_ENABLED.get()) {
            return "config: [autoSync] enabled = false";
        }
        if (ModConfigs.AUTO_SYNC_DEDICATED_ONLY.get() && !file.server.isDedicatedServer()) {
            return "config: [autoSync] dedicatedServerOnly = true で、専用サーバーではないため";
        }
        return null;
    }

    /** メインスレッドで1回だけ呼ぶ(FMLCommonSetupEvent の enqueueWork の中)。 */
    public static void registerEvents() {
        if (registered) {
            return;
        }
        if (!PersonalMode.isEnabled()) {
            LOGGER.info("[issspell_research] 個人別進行・研究以外の自動同期: 無効 ({})", PersonalMode.reason());
            return;
        }
        registered = true;
        ObjectCompletedEvent.QUEST.register(event -> {
            Quest quest = event.getQuest();
            if (!(quest.getQuestFile() instanceof ServerQuestFile serverFile) || !isEnabled(serverFile)) {
                return EventResult.pass();
            }
            if (!syncing && !ResearchChapters.isResearchChapter(quest.getChapter())) {
                PENDING.add(event.getData().getTeamId());
            }
            return EventResult.pass();
        });
        LOGGER.info("[issspell_research] 個人別進行 + 研究以外のクエストの自動同期: 有効 ({})", PersonalMode.reason());
    }

    /** サーバーのtick終了時に、保留中の同期をまとめて処理する。 */
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            PENDING.clear();
            return;
        }
        List<UUID> ids = new ArrayList<>(PENDING);
        PENDING.removeAll(ids);
        Set<UUID> doneTeams = new HashSet<>();
        for (UUID id : ids) {
            FTBTeamsAPI.api().getManager().getTeamForPlayerID(id).ifPresent(team -> {
                if (doneTeams.add(team.getId())) {
                    syncTeam(file, team);
                }
            });
        }
    }

    /**
     * ログイン時: そのプレイヤーだけ、パーティの完了済みクエストを取り込む。
     * FTBQ本来のログイン処理(クライアントへのデータ送信)より前に呼ばれるので、ここでは送信しない。
     */
    public static void syncOnLogin(ServerPlayer player) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || !isEnabled(file)) {
            return;
        }
        FTBTeamsAPI.api().getManager().getTeamForPlayerID(player.getUUID()).ifPresent(team -> {
            if (team.getMembers().size() < 2) {
                return;
            }
            Set<Long> research = ResearchChapters.researchChapterIds(file);
            Set<Long> completed = computeTeamCompleted(file, team.getMembers(), research);
            boolean was = syncing;
            syncing = true;
            try {
                syncPlayer(file, player, completed);
            } finally {
                syncing = was;
            }
        });
    }

    /** パーティの、オンラインの全員を同期する。同期が起きた人にはクライアントへデータを送る。戻り値は同期が起きた人数。 */
    public static int syncTeam(ServerQuestFile file, Team team) {
        Set<UUID> members = team.getMembers();
        if (members.size() < 2 || !isEnabled(file)) {
            return 0;
        }
        Set<Long> research = ResearchChapters.researchChapterIds(file);
        Set<Long> completed = computeTeamCompleted(file, members, research);
        if (completed.isEmpty()) {
            return 0;
        }
        int changed = 0;
        boolean was = syncing;
        syncing = true;
        try {
            for (UUID id : members) {
                ServerPlayer player = file.server.getPlayerList().getPlayer(id);
                if (player != null && syncPlayer(file, player, completed)) {
                    changed++;
                    new SyncTeamDataMessage(file.getOrCreateTeamData(player), true).sendTo(player);
                }
            }
        } finally {
            syncing = was;
        }
        return changed;
    }

    /** コマンド用: オンラインの全プレイヤーのパーティを同期する。戻り値は同期したパーティ数。 */
    public static int syncAllOnline(MinecraftServer server) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            return 0;
        }
        Set<UUID> doneTeams = new HashSet<>();
        int teams = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var team = FTBTeamsAPI.api().getManager().getTeamForPlayerID(player.getUUID());
            if (team.isPresent() && doneTeams.add(team.get().getId())) {
                syncTeam(file, team.get());
                teams++;
            }
        }
        return teams;
    }

    /**
     * 1人ぶんの同期。前提が満たされたクエストから順に取り込み、変化がなくなるまで繰り返す
     * (A→B→C のように連なるクエストも、1回で全部同期される)。
     * @return 1つでも新しくクエストを完了させたら true
     */
    private static boolean syncPlayer(ServerQuestFile file, ServerPlayer player, Set<Long> teamCompleted) {
        TeamData data = file.getOrCreateTeamData(player);
        if (data == null || data.isLocked()) {
            return false;
        }
        Set<Long> handled = new HashSet<>();
        boolean[] changed = {false};
        boolean progressed;
        do {
            boolean[] round = {false};
            file.forAllQuests(quest -> {
                if (handled.contains(quest.id) || !teamCompleted.contains(quest.id)) {
                    return;
                }
                if (data.isCompleted(quest)) {
                    handled.add(quest.id);
                    return;
                }
                if (!data.areDependenciesComplete(quest)) {
                    return;
                }
                syncQuest(file, player, data, quest);
                handled.add(quest.id);
                round[0] = true;
                changed[0] = true;
            });
            progressed = round[0];
        } while (progressed);
        return changed[0];
    }

    /** 1つのクエストの全タスクを完了状態にする(前提未達の場合は呼ばない)。 */
    private static void syncQuest(ServerQuestFile file, ServerPlayer player, TeamData data, Quest quest) {
        file.withPlayerContext(player, () -> {
            for (Task task : quest.getTasks()) {
                if (!data.isCompleted(task)) {
                    if (data.getProgress(task) >= task.getMaxProgress()) {
                        // 進行は満タンなのに未完了(不整合): 0に戻してから満タンにして、完了処理を走らせる
                        data.setProgress(task, 0L);
                    }
                    data.setProgress(task, task.getMaxProgress());
                }
            }
        });
    }

    /** パーティの誰かが完了済みの、研究以外のクエストID一覧。まだデータのない(一度もログインしていない)人のデータは作らない。 */
    private static Set<Long> computeTeamCompleted(ServerQuestFile file, Set<UUID> members, Set<Long> researchChapterIds) {
        List<TeamData> memberData = members.stream()
                .map(file::getNullableTeamData)
                .filter(Objects::nonNull)
                .toList();
        Set<Long> result = new HashSet<>();
        file.forAllQuests(quest -> {
            Chapter chapter = quest.getChapter();
            if (chapter != null && researchChapterIds.contains(chapter.id)) {
                return;
            }
            for (TeamData data : memberData) {
                if (data.getCompletedTime(quest.id).isPresent()) {
                    result.add(quest.id);
                    return;
                }
            }
        });
        return result;
    }
}
