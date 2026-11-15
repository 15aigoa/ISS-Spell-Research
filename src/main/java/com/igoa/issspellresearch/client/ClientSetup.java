// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.client;

import com.igoa.issspellresearch.IssSpellResearch;
import com.igoa.issspellresearch.registry.ModItems;
import com.igoa.issspellresearch.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = IssSpellResearch.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.RESEARCH_TABLE.get(), ResearchTableScreen::new));
    }

    /**
     * 研究レポートの色付け。モデルの layer1 (tintindex 1) だけを、学派の色で染める。
     * layer0 (tintindex 0) は染めない。
     */
    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 1 ? ReportColors.getTint(stack) : 0xFFFFFFFF,
                ModItems.RESEARCH_REPORT.get());
    }
}
