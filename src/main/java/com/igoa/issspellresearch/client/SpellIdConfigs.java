// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.client;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * FTB Questsの報酬エディタで、魔法IDを「候補から選ぶ」形にする(クライアント専用)。
 *
 * FTB Libraryの EnumConfig は、候補が17個以上あると、検索ボックス付きの選択画面を開く。
 * ISSの魔法は十分多いので、その画面が使われる。表示は「アイコン + 魔法名 + ID + 学派名」で、
 * 検索は、魔法名・ID・学派名のどれでも当たる。保存される値は、これまでどおり魔法IDの文字列。
 */
public final class SpellIdConfigs {
    /** 未選択を表す値(空文字)。 */
    private static final String NONE = "";

    private SpellIdConfigs() {}

    public static void addSpellChooser(ConfigGroup config, String key, String current, Consumer<String> setter) {
        List<String> values = new ArrayList<>();
        values.add(NONE);

        List<AbstractSpell> spells = new ArrayList<>(SpellRegistry.getEnabledSpells());
        spells.removeIf(spell -> spell.equals(SpellRegistry.none()) || !spell.allowCrafting());
        spells.sort(Comparator.comparing((AbstractSpell s) -> s.getSchoolType().getDisplayName().getString())
                .thenComparing(s -> s.getDisplayName(null).getString()));
        for (AbstractSpell spell : spells) {
            values.add(spell.getSpellResource().toString());
        }
        // 設定済みの値が、候補にない場合(アンインストールされたMODの魔法など)も、そのまま表示・保持できるようにする
        if (current != null && !current.isEmpty() && !values.contains(current)) {
            values.add(current);
        }

        NameMap<String> nameMap = NameMap.of(NONE, values)
                .id(id -> id)
                .name(SpellIdConfigs::label)
                .icon(SpellIdConfigs::icon)
                .create();
        config.addEnum(key, current == null ? NONE : current, setter, nameMap, NONE);
    }

    /** 一覧の表示: 魔法名 (白) + ID (灰色) + 学派名。この文字列全体が、検索の対象になる。 */
    private static Component label(String id) {
        if (id.isEmpty()) {
            return Component.translatable("issspell_research.config.spell.none");
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        AbstractSpell spell = location == null ? null : SpellRegistry.getSpell(location);
        if (spell == null || spell.equals(SpellRegistry.none())) {
            return Component.literal(id).withStyle(ChatFormatting.RED);
        }
        MutableComponent text = spell.getDisplayName(null).copy();
        text.append(Component.literal("  " + id).withStyle(ChatFormatting.GRAY));
        text.append(Component.literal("  ").append(spell.getSchoolType().getDisplayName()));
        return text;
    }

    private static Icon icon(String id) {
        ResourceLocation location = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        AbstractSpell spell = location == null ? null : SpellRegistry.getSpell(location);
        if (spell == null || spell.equals(SpellRegistry.none())) {
            return Icon.empty();
        }
        return new ImageIcon(spell.getSpellIconResource());
    }
}
