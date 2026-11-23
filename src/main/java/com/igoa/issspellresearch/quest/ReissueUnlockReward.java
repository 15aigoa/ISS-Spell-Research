// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.quest;

import com.igoa.issspellresearch.IssSpellResearch;
import com.igoa.issspellresearch.client.SpellDisplay;
import com.igoa.issspellresearch.client.SpellIdConfigs;
import com.igoa.issspellresearch.data.ResearchUnlockData;
import com.igoa.issspellresearch.registry.ModItems;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 「研究レポートの再発行を有効にする」FTB Questsの報酬。設定した魔法について、そのプレイヤーが
 * 研究テーブルの「再発行」タブから、研究レポートを作り直せるようになる。
 * 研究レポート報酬と同じクエストに付けるのが基本(非表示チャプターでは自動で受け取られる)。
 */
public class ReissueUnlockReward extends Reward {
    private static final Logger LOGGER = LoggerFactory.getLogger(IssSpellResearch.MODID);

    /** 再発行を有効にする魔法のID(例: irons_spellbooks:fireball)。 */
    private String spell = "";

    public ReissueUnlockReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return ModQuestTypes.REISSUE_REWARD;
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
            LOGGER.warn("Reissue unlock reward {} has an invalid spell id: '{}'", this.id, spell);
            return;
        }
        boolean added = ResearchUnlockData.get(player.server).enableReissue(player.getUUID(), id);
        if (notify && added) {
            AbstractSpell abstractSpell = SpellRegistry.getSpell(id);
            Component name = abstractSpell == null ? Component.literal(spell) : abstractSpell.getDisplayName(player);
            player.displayClientMessage(Component.translatable("issspell_research.reissue.unlocked", name), true);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Icon getAltIcon() {
        Icon spellIcon = SpellDisplay.iconOf(spell);
        if (spellIcon != null) {
            return spellIcon;
        }
        ResourceLocation id = ResourceLocation.tryParse(spell);
        if (id == null) {
            return super.getAltIcon();
        }
        return ItemIcon.getItemIcon(ModItems.createReport(id));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getAltTitle() {
        return Component.translatable("issspell_research.reward.reissue_unlock.title", SpellDisplay.nameOf(spell));
    }
}
