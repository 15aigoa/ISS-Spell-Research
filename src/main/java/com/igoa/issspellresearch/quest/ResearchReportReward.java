// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.quest;

import com.igoa.issspellresearch.IssSpellResearch;
import com.igoa.issspellresearch.client.SpellIdConfigs;
import com.igoa.issspellresearch.registry.ModItems;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.net.DisplayItemRewardToastMessage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemHandlerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 「研究レポートを受け取る」FTB Questsの報酬。設定した魔法IDを持つ研究レポートを渡す。
 * 非表示チャプター内のクエストでは、FTBQの仕様上この報酬は自動で受け取られる。
 */
public class ResearchReportReward extends Reward {
    private static final Logger LOGGER = LoggerFactory.getLogger(IssSpellResearch.MODID);

    /** 解禁する魔法のID(例: irons_spellbooks:fireball)。今は1つだけ。 */
    protected String spell = "";

    public ResearchReportReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return ModQuestTypes.REPORT_REWARD;
    }

    @Override
    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);
        nbt.putString("spell", spell);
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        spell = nbt.getString("spell");
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(spell);
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        spell = buffer.readUtf();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        SpellIdConfigs.addSpellChooser(config, "spell", spell, v -> spell = v);
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        ResourceLocation id = ResourceLocation.tryParse(spell);
        if (id == null) {
            LOGGER.warn("Research report reward {} has an invalid spell id: '{}'", this.id, spell);
            return;
        }
        ItemStack stack = ModItems.createReport(id);
        ItemHandlerHelper.giveItemToPlayer(player, stack.copy());
        if (notify) {
            new DisplayItemRewardToastMessage(stack, 1).sendTo(player);
        }
    }

    /** クエスト画面の報酬アイコンを、学派の色で染まった研究レポートにする。 */
    @Override
    @OnlyIn(Dist.CLIENT)
    public Icon getAltIcon() {
        ResourceLocation id = ResourceLocation.tryParse(spell);
        if (id == null) {
            return super.getAltIcon();
        }
        return ItemIcon.getItemIcon(ModItems.createReport(id));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getAltTitle() {
        return Component.translatable("issspell_research.reward.research_report.title",
                spell.isEmpty() ? "-" : spell);
    }
}
