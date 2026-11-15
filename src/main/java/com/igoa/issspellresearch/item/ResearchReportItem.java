// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.item;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 研究レポート。解禁する魔法のID(文字列)をNBTのリストで持つ。耐久度式で、MAX_USES回までスクロール製造に使える。
 * 今は1つだけ使うが、リスト形式で持つので、後から「1レポート=複数魔法」に広げられる。
 * NBT例: {Spells:["irons_spellbooks:fireball"]}
 */
public class ResearchReportItem extends Item {
    public static final String TAG_SPELLS = "Spells";

    /** スクロールを作れる回数(耐久値)。 */
    public static final int MAX_USES = 5;

    public ResearchReportItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Item item, List<ResourceLocation> spells) {
        ItemStack stack = new ItemStack(item);
        ListTag list = new ListTag();
        for (ResourceLocation id : spells) {
            list.add(StringTag.valueOf(id.toString()));
        }
        stack.getOrCreateTag().put(TAG_SPELLS, list);
        return stack;
    }

    public static List<ResourceLocation> getSpells(ItemStack stack) {
        List<ResourceLocation> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SPELLS, Tag.TAG_LIST)) {
            return result;
        }
        ListTag list = tag.getList(TAG_SPELLS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    public static boolean hasSpells(ItemStack stack) {
        return stack.getItem() instanceof ResearchReportItem && !getSpells(stack).isEmpty();
    }

    /** このレポートに、指定した魔法ID(例: "irons_spellbooks:fireball")が記録されているか。 */
    public static boolean containsSpell(ItemStack stack, String spellId) {
        if (!(stack.getItem() instanceof ResearchReportItem)) {
            return false;
        }
        for (ResourceLocation id : getSpells(stack)) {
            if (id.toString().equals(spellId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 使用回数を1回消費する(耐久を1減らし、使い切ったらスタックを消す)。
     * クライアントとサーバーで同じ結果になるよう、乱数(耐久エンチャント等)は使わない。
     */
    public static void consumeUse(ItemStack stack) {
        int damage = stack.getDamageValue() + 1;
        if (damage >= stack.getMaxDamage()) {
            stack.shrink(1);
        } else {
            stack.setDamageValue(damage);
        }
    }

    /** 金床・砥石などでの耐久回復(=使用回数の回復)を防ぐ。 */
    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    /** 本によるエンチャント(修繕など)で回数が回復しないようにする。 */
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        List<ResourceLocation> spells = getSpells(stack);
        if (spells.isEmpty()) {
            tooltip.add(Component.translatable("item.issspell_research.research_report.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        for (ResourceLocation id : spells) {
            // ISSの魔法名の翻訳キーは未確認。見つからなければIDをそのまま表示する
            Component name = Component.translatableWithFallback(
                    "spell." + id.getNamespace() + "." + id.getPath(), id.toString());
            tooltip.add(Component.translatable("item.issspell_research.research_report.spell", name)
                    .withStyle(ChatFormatting.GRAY));
        }
        int max = stack.getMaxDamage();
        tooltip.add(Component.translatable("item.issspell_research.research_report.uses",
                max - stack.getDamageValue(), max).withStyle(ChatFormatting.DARK_AQUA));
    }
}
