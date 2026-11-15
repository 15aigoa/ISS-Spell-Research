// SPDX-License-Identifier: MPL-2.0 AND MIT
package com.igoa.issspellresearch.mixin.personal;

import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 進行データの取り出し口を、チームIDではなくプレイヤーUUIDにする(サーバー側のみ)。
 * 派生元: ftb-solo-quests (c) 2026 YamaArashi, MIT License
 */
@Mixin(value = BaseQuestFile.class, remap = false)
public abstract class PersonalBaseQuestFileMixin {
    @Inject(method = "getOrCreateTeamData(Lnet/minecraft/world/entity/Entity;)Ldev/ftb/mods/ftbquests/quest/TeamData;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void issspell_research$usePlayerId(Entity player, CallbackInfoReturnable<TeamData> cir) {
        BaseQuestFile self = (BaseQuestFile) (Object) this;
        // クライアント側のクエストファイル(ClientQuestFile)は、FTBQ本来の動作のままにする
        if (self instanceof ServerQuestFile) {
            cir.setReturnValue(self.getOrCreateTeamData(player.getUUID()));
        }
    }
}
