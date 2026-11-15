// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.reissue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 研究テーブルの「再発行」タブに出す1件(魔法 + 必要素材)。メニューを開くときに、サーバーからクライアントへ送る。 */
public record ReissueEntry(ResourceLocation spell, List<ReissueRequirement> requirements) {

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(spell);
        buf.writeVarInt(requirements.size());
        for (ReissueRequirement requirement : requirements) {
            requirement.write(buf);
        }
    }

    public static ReissueEntry read(FriendlyByteBuf buf) {
        ResourceLocation spell = buf.readResourceLocation();
        int size = buf.readVarInt();
        List<ReissueRequirement> requirements = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            requirements.add(ReissueRequirement.read(buf));
        }
        return new ReissueEntry(spell, requirements);
    }

    public static void writeList(FriendlyByteBuf buf, List<ReissueEntry> entries) {
        buf.writeVarInt(entries.size());
        for (ReissueEntry entry : entries) {
            entry.write(buf);
        }
    }

    public static List<ReissueEntry> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ReissueEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(read(buf));
        }
        return entries;
    }
}
