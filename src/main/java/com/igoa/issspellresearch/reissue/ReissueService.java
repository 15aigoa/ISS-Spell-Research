// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.reissue;

import com.igoa.issspellresearch.data.ResearchUnlockData;
import com.igoa.issspellresearch.registry.ModItems;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.items.ItemHandlerHelper;

/** 研究レポートの再発行(サーバー側)。 */
public final class ReissueService {
    private ReissueService() {}

    /** そのプレイヤーが再発行できる魔法の一覧(必要素材つき)。存在しない・スクロール製造できない魔法は除く。 */
    public static List<ReissueEntry> buildEntries(ServerPlayer player) {
        List<ReissueEntry> entries = new ArrayList<>();
        for (ResourceLocation id : ResearchUnlockData.get(player.server).getReissueEnabled(player.getUUID())) {
            AbstractSpell spell = SpellRegistry.getSpell(id);
            if (spell == null || spell.equals(SpellRegistry.none()) || !spell.allowCrafting()) {
                continue;
            }
            entries.add(new ReissueEntry(id, ReissueCosts.costFor(id)));
        }
        return entries;
    }

    /** 再発行する。有効化済みで、素材が足りているときだけ、素材を消費して研究レポートを渡す。 */
    public static boolean tryReissue(ServerPlayer player, ResourceLocation spellId) {
        if (!ResearchUnlockData.get(player.server).isReissueEnabled(player.getUUID(), spellId)) {
            player.displayClientMessage(Component.translatable("issspell_research.reissue.not_unlocked"), true);
            return false;
        }
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        if (spell == null || spell.equals(SpellRegistry.none()) || !spell.allowCrafting()) {
            return false;
        }
        // サーバー側で、その時点の素材を取り直す(画面を開いたあとに設定が変わっていても、最新の設定で判定する)
        List<ReissueRequirement> cost = ReissueCosts.costFor(spellId);
        for (ReissueRequirement requirement : cost) {
            if (requirement.countIn(player.getInventory()) < requirement.count()) {
                player.displayClientMessage(Component.translatable("issspell_research.reissue.insufficient"), true);
                return false;
            }
        }
        for (ReissueRequirement requirement : cost) {
            requirement.consumeFrom(player.getInventory());
        }
        player.getInventory().setChanged();
        ItemHandlerHelper.giveItemToPlayer(player, ModItems.createReport(spellId));
        player.displayClientMessage(Component.translatable("issspell_research.reissue.done", spell.getDisplayName(player)), true);
        return true;
    }
}
