// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.client;

import com.igoa.issspellresearch.item.ResearchReportItem;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.BakedModelWrapper;

/**
 * 研究レポートのモデル。記録された魔法の学派に、専用モデルがあればそれを使い、なければ元のモデル(色付け方式)を使う。
 * ItemOverrides の引数なしコンストラクタは、Forgeのアクセストランスフォーマーで protected になっているので、継承して使える。
 */
public class ResearchReportBakedModel extends BakedModelWrapper<BakedModel> {
    private final ItemOverrides overrides;

    public ResearchReportBakedModel(BakedModel original, Map<ResourceLocation, BakedModel> bySchool) {
        super(original);
        this.overrides = new SchoolOverrides(original.getOverrides(), bySchool);
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overrides;
    }

    private static final class SchoolOverrides extends ItemOverrides {
        private final ItemOverrides delegate;
        private final Map<ResourceLocation, BakedModel> bySchool;

        private SchoolOverrides(ItemOverrides delegate, Map<ResourceLocation, BakedModel> bySchool) {
            super();
            this.delegate = delegate;
            this.bySchool = bySchool;
        }

        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            if (!this.bySchool.isEmpty()) {
                BakedModel schoolModel = findSchoolModel(stack);
                if (schoolModel != null) {
                    return schoolModel;
                }
            }
            return this.delegate.resolve(model, stack, level, entity, seed);
        }

        @Nullable
        private BakedModel findSchoolModel(ItemStack stack) {
            List<ResourceLocation> spells = ResearchReportItem.getSpells(stack);
            if (spells.isEmpty()) {
                return null;
            }
            AbstractSpell spell = SpellRegistry.getSpell(spells.get(0));
            if (spell == null || spell.equals(SpellRegistry.none())) {
                return null;
            }
            return this.bySchool.get(spell.getSchoolType().getId());
        }
    }
}
