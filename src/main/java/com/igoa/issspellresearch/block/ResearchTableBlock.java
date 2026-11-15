// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.block;

import com.igoa.issspellresearch.menu.ResearchTableMenu;
import com.igoa.issspellresearch.reissue.ReissueEntry;
import com.igoa.issspellresearch.reissue.ReissueService;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * 研究テーブル。今はバニラのエンチャントテーブルと同様、ブロックエンティティを持たない。
 * (後でブロックエンティティ化する場合は、EntityBlock化してメニューの生成元をBEに移す)
 */
public class ResearchTableBlock extends Block {
    public ResearchTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            // 開いた時点の「再発行できる研究」を、サーバーが作って、メニューとクライアントに渡す
            List<ReissueEntry> entries = ReissueService.buildEntries(serverPlayer);
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inv, p) -> new ResearchTableMenu(id, inv, ContainerLevelAccess.create(level, pos), entries),
                    Component.translatable("block.issspell_research.research_table")),
                    buf -> {
                        buf.writeBlockPos(pos);
                        ReissueEntry.writeList(buf, entries);
                    });
        }
        return InteractionResult.CONSUME;
    }
}
