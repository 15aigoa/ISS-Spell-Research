// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.quest;

import com.igoa.issspellresearch.data.ResearchPointsData;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.ISingleLongValueTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 「研究ポイントを value 個消費する」FTB Questsのタスク。
 * FTBQ標準の経験値タスクと同じ分割納品方式:
 * ボタンを押すたびに、所持ポイントの範囲で(残り必要量まで)消費して進行に加算する。
 */
public class ResearchPointsTask extends Task implements ISingleLongValueTask {
    private long value = 1L;

    public ResearchPointsTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return ModQuestTypes.POINTS;
    }

    @Override
    public long getMaxProgress() {
        return value;
    }

    @Override
    public void setValue(long v) {
        this.value = v;
    }

    /** true にすると、ログイン時の自動チェックが走らず、ボタン操作で納品する形になる(経験値タスクと同じ)。 */
    @Override
    public boolean consumesResources() {
        return true;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (!checkTaskSequence(teamData)) {
            return;
        }
        long remaining = value - teamData.getProgress(this);
        if (remaining <= 0L) {
            return;
        }
        long consumed = ResearchPointsData.get(player.server).consume(player.getUUID(), remaining);
        if (consumed <= 0L) {
            player.displayClientMessage(Component.translatable("issspell_research.task.points.insufficient"), true);
            return;
        }
        teamData.addProgress(this, consumed);
    }

    @Override
    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);
        nbt.putLong("value", value);
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        value = nbt.getLong("value");
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeVarLong(value);
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        value = buffer.readVarLong();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addLong("value", value, v -> value = v, 1L, 1L, Long.MAX_VALUE);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getAltTitle() {
        return Component.translatable("issspell_research.task.points.title", formatMaxProgress());
    }
}
