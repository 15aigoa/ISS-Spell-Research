// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.registry;

import com.igoa.issspellresearch.IssSpellResearch;
import com.igoa.issspellresearch.item.ResearchReportItem;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, IssSpellResearch.MODID);

    public static final RegistryObject<Item> RESEARCH_REPORT = ITEMS.register("research_report",
            () -> new ResearchReportItem(new Item.Properties().durability(ResearchReportItem.MAX_USES)));

    public static final RegistryObject<Item> RESEARCH_TABLE = ITEMS.register("research_table",
            () -> new BlockItem(ModBlocks.RESEARCH_TABLE.get(), new Item.Properties()));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** 指定した魔法を持つ研究レポートを作る(1つだけ指定する形)。 */
    public static ItemStack createReport(ResourceLocation spell) {
        return ResearchReportItem.create(RESEARCH_REPORT.get(), List.of(spell));
    }
}
