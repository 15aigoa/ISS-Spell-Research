// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch;

import com.igoa.issspellresearch.command.ResearchCommands;
import com.igoa.issspellresearch.config.ModConfigs;
import com.igoa.issspellresearch.personal.TeamAutoSync;
import com.igoa.issspellresearch.quest.ModQuestTypes;
import com.igoa.issspellresearch.registry.ModBlocks;
import com.igoa.issspellresearch.registry.ModItems;
import com.igoa.issspellresearch.registry.ModMenus;
import com.igoa.issspellresearch.reissue.ReissueCosts;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(IssSpellResearch.MODID)
public class IssSpellResearch {
    public static final String MODID = "issspell_research";

    public IssSpellResearch() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModMenus.register(modBus);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onBuildCreativeTab);
        MinecraftForge.EVENT_BUS.addListener(ResearchCommands::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(TeamAutoSync::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfigs.SPEC);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // FMLCommonSetupEvent は、各MODのハンドラが別スレッドで「並列」に呼ばれる。
        // ArchitecturyやFTBQのイベント登録はスレッドセーフではないので、enqueueWork でメインスレッドに回す。
        event.enqueueWork(() -> {
            ModQuestTypes.register();
            TeamAutoSync.registerEvents();
        });
    }

    /** 再発行の必要素材(データパック)を、サーバーの読み込み・/reload のたびに読み込む。 */
    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ReissueCosts());
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.RESEARCH_TABLE);
        }
    }
}
