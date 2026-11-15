// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.data;

import com.igoa.issspellresearch.IssSpellResearch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * プレイヤー(UUID)ごとの「研究レポートの再発行が有効になっている魔法」。
 * プレイヤーのNBTには書かず、ワールドの data/ フォルダに1ファイルで保存する。
 * FTBQの「再発行の有効化」報酬を受け取ったときに記録される(報酬の受け取りはプレイヤーごとなので、個人別進行がオフでも個人別になる)。
 */
public class ResearchUnlockData extends SavedData {
    private static final String DATA_NAME = IssSpellResearch.MODID + "_unlocks";

    private final Map<UUID, Set<ResourceLocation>> reissue = new HashMap<>();

    public static ResearchUnlockData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(ResearchUnlockData::load, ResearchUnlockData::new, DATA_NAME);
    }

    public static ResearchUnlockData load(CompoundTag tag) {
        ResearchUnlockData data = new ResearchUnlockData();
        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            if (!entry.hasUUID("id")) continue;
            Set<ResourceLocation> spells = new HashSet<>();
            ListTag list = entry.getList("reissue", Tag.TAG_STRING);
            for (int j = 0; j < list.size(); j++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(j));
                if (id != null) spells.add(id);
            }
            if (!spells.isEmpty()) {
                data.reissue.put(entry.getUUID("id"), spells);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Set<ResourceLocation>> e : reissue.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", e.getKey());
            ListTag list = new ListTag();
            for (ResourceLocation id : e.getValue()) {
                list.add(StringTag.valueOf(id.toString()));
            }
            entry.put("reissue", list);
            players.add(entry);
        }
        tag.put("players", players);
        return tag;
    }

    public boolean isReissueEnabled(UUID player, ResourceLocation spell) {
        Set<ResourceLocation> set = reissue.get(player);
        return set != null && set.contains(spell);
    }

    /** @return 新しく有効になったら true(すでに有効だったら false) */
    public boolean enableReissue(UUID player, ResourceLocation spell) {
        boolean added = reissue.computeIfAbsent(player, k -> new HashSet<>()).add(spell);
        if (added) setDirty();
        return added;
    }

    /** @return 無効にしたら true(もともと有効でなければ false) */
    public boolean disableReissue(UUID player, ResourceLocation spell) {
        Set<ResourceLocation> set = reissue.get(player);
        boolean removed = set != null && set.remove(spell);
        if (removed) {
            if (set.isEmpty()) reissue.remove(player);
            setDirty();
        }
        return removed;
    }

    /** 再発行が有効な魔法ID(文字列順)。 */
    public List<ResourceLocation> getReissueEnabled(UUID player) {
        Set<ResourceLocation> set = reissue.get(player);
        if (set == null) return List.of();
        List<ResourceLocation> list = new ArrayList<>(set);
        list.sort(Comparator.comparing(ResourceLocation::toString));
        return list;
    }
}
