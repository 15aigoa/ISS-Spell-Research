// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.reissue;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** 再発行に必要な素材1種類(素材の指定 + 個数)。 */
public record ReissueRequirement(Ingredient ingredient, int count) {

    public void write(FriendlyByteBuf buf) {
        ingredient.toNetwork(buf);
        buf.writeVarInt(count);
    }

    public static ReissueRequirement read(FriendlyByteBuf buf) {
        Ingredient ingredient = Ingredient.fromNetwork(buf);
        int count = buf.readVarInt();
        return new ReissueRequirement(ingredient, count);
    }

    /** インベントリ(メイン・ホットバー・オフハンド)にある、この素材の合計個数。 */
    public int countIn(Inventory inventory) {
        int total = 0;
        for (ItemStack stack : inventory.items) {
            if (!stack.isEmpty() && ingredient.test(stack)) total += stack.getCount();
        }
        for (ItemStack stack : inventory.offhand) {
            if (!stack.isEmpty() && ingredient.test(stack)) total += stack.getCount();
        }
        return total;
    }

    /** インベントリから、この素材を count 個消費する(足りることを確認してから呼ぶこと)。 */
    public void consumeFrom(Inventory inventory) {
        int remaining = count;
        for (ItemStack stack : inventory.items) {
            if (remaining <= 0) return;
            if (!stack.isEmpty() && ingredient.test(stack)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (remaining <= 0) return;
            if (!stack.isEmpty() && ingredient.test(stack)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }
}
