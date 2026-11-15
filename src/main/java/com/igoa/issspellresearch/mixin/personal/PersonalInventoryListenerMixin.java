// SPDX-License-Identifier: MPL-2.0 AND MIT
package com.igoa.issspellresearch.mixin.personal;

import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * インベントリの変化によるタスクの検出を、プレイヤーUUIDごとの進行データで行う。
 * FTB Quests 2001.4.22 の detect() と同じ処理で、進行データの取り出し方だけを変えている。
 * (FTBQの更新で detect() が変わった場合は、ここも見直すこと)
 * 派生元: ftb-solo-quests (c) 2026 YamaArashi, MIT License
 */
@Mixin(value = FTBQuestsInventoryListener.class, remap = false)
public abstract class PersonalInventoryListenerMixin {

    @Shadow(remap = false)
    private static Map<Item, List<ItemStack>> inventorySummaryCache;
    @Shadow(remap = false)
    private static List<ItemStack> nonEmptyStacks;

    @Shadow(remap = false)
    private static void buildInventorySummary(ServerPlayer player) {
    }

    @Inject(method = "detect", at = @At("HEAD"), cancellable = true, remap = false)
    private static void issspell_research$detect(ServerPlayer player, ItemStack craftedItem, long sourceTask, CallbackInfo ci) {
        ci.cancel();
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || PlayerHooks.isFake(player)) {
            return;
        }
        List<Task> tasksToCheck = craftedItem.isEmpty() ? file.getSubmitTasks() : file.getCraftingTasks();
        if (!tasksToCheck.isEmpty()) {
            TeamData data = file.getOrCreateTeamData(player);
            if (data != null && !data.isLocked()) {
                file.withPlayerContext(player, () -> {
                    buildInventorySummary(player);
                    for (Task task : tasksToCheck) {
                        if (task.id != sourceTask && data.canStartTasks(task.getQuest())) {
                            task.submitTask(data, player, craftedItem);
                        }
                    }
                    inventorySummaryCache.clear();
                    nonEmptyStacks.clear();
                });
            }
        }
    }
}
