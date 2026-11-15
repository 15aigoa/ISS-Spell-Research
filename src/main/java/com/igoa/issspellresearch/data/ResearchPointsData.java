// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.data;

import com.igoa.issspellresearch.IssSpellResearch;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * プレイヤー(UUID)ごとの研究ポイント残高。
 * プレイヤーのNBTには一切書かず、ワールドの data/ フォルダに1ファイルで保存する。
 * 保存されるのは「ポイントが1以上のプレイヤー」だけ(1人あたり UUID + long 1個)。
 */
public class ResearchPointsData extends SavedData {
    private static final String DATA_NAME = IssSpellResearch.MODID + "_points";

    private final Map<UUID, Long> points = new HashMap<>();

    public static ResearchPointsData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(ResearchPointsData::load, ResearchPointsData::new, DATA_NAME);
    }

    public static ResearchPointsData load(CompoundTag tag) {
        ResearchPointsData data = new ResearchPointsData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("id")) continue;
            long value = entry.getLong("points");
            if (value > 0L) {
                data.points.put(entry.getUUID("id"), value);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Long> e : points.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", e.getKey());
            entry.putLong("points", e.getValue());
            list.add(entry);
        }
        tag.put("players", list);
        return tag;
    }

    public long get(UUID id) {
        return points.getOrDefault(id, 0L);
    }

    /** 残高を直接設定する(0未満は0)。 */
    public void set(UUID id, long value) {
        long v = Math.max(0L, value);
        if (v == 0L) {
            points.remove(id);
        } else {
            points.put(id, v);
        }
        setDirty();
    }

    /** ポイントを加算し、加算後の残高を返す(オーバーフローしないよう上限で止める)。 */
    public long add(UUID id, long amount) {
        if (amount <= 0L) return get(id);
        long cur = get(id);
        long next = cur > Long.MAX_VALUE - amount ? Long.MAX_VALUE : cur + amount;
        set(id, next);
        return next;
    }

    /** 最大 max までを消費し、実際に消費した量を返す(残高が足りなければ残高ぶんだけ)。 */
    public long consume(UUID id, long max) {
        if (max <= 0L) return 0L;
        long cur = get(id);
        long used = Math.min(cur, max);
        if (used > 0L) {
            set(id, cur - used);
        }
        return used;
    }
}
