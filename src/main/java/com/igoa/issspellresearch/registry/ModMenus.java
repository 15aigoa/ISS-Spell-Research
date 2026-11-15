// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.registry;

import com.igoa.issspellresearch.IssSpellResearch;
import com.igoa.issspellresearch.menu.ResearchTableMenu;
import com.igoa.issspellresearch.reissue.ReissueEntry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, IssSpellResearch.MODID);

    public static final RegistryObject<MenuType<ResearchTableMenu>> RESEARCH_TABLE = MENUS.register("research_table",
            () -> IForgeMenuType.create((windowId, inv, buf) -> {
                BlockPos pos = buf.readBlockPos();
                List<ReissueEntry> entries = ReissueEntry.readList(buf);
                return new ResearchTableMenu(windowId, inv, ContainerLevelAccess.create(inv.player.level(), pos), entries);
            }));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
