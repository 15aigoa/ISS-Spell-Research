// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.util;

import com.igoa.issspellresearch.config.ModConfigs;
import java.util.Map;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** エンチャント本1冊ぶんの研究ポイントを計算する。 */
public final class EnchantPointCalculator {
    private EnchantPointCalculator() {}

    /** 1冊あたりの上限(オーバーフロー防止)。 */
    private static final long MAX_PER_BOOK = 1_000_000_000L;

    public static long calculate(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return 0L;
        }
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        double base = ModConfigs.LEVEL_BASE.get();
        double multiplier = ModConfigs.MULTIPLIER.get();
        double total = 0.0D;
        for (int level : enchantments.values()) {
            if (level <= 0) continue;
            total += Math.pow(base, Math.min(level, 60) - 1);
        }
        long result = Math.round(total * multiplier);
        return Math.max(0L, Math.min(result, MAX_PER_BOOK));
    }
}
