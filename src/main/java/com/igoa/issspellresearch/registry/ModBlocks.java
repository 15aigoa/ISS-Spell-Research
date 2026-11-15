// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.registry;

import com.igoa.issspellresearch.IssSpellResearch;
import com.igoa.issspellresearch.block.ResearchTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, IssSpellResearch.MODID);

    public static final RegistryObject<Block> RESEARCH_TABLE = BLOCKS.register("research_table",
            () -> new ResearchTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
