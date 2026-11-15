// SPDX-License-Identifier: MPL-2.0 AND MIT
package com.igoa.issspellresearch.mixin.personal;

import com.igoa.issspellresearch.personal.TeamAutoSync;
import dev.ftb.mods.ftbquests.net.SyncTeamDataMessage;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerChangedTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerLoggedInAfterTeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamCreatedEvent;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ログイン・パーティ変更・パーティ作成・所属判定を、プレイヤーUUIDごとの進行データに合わせる。
 * ログイン時とパーティ変更時には、研究以外のクエストの同期(TeamAutoSync)を行う。
 * 派生元: ftb-solo-quests (c) 2026 YamaArashi, MIT License
 */
@Mixin(value = ServerQuestFile.class, remap = false)
public abstract class PersonalServerQuestFileMixin {

    /** playerLoggedIn の中で、チームではなくログインしたプレイヤーのデータを使うために覚えておく。 */
    @Unique
    private ServerPlayer issspell_research$loggedInPlayer;

    @Inject(method = "playerLoggedIn", at = @At("HEAD"), remap = false)
    private void issspell_research$onLogin(PlayerLoggedInAfterTeamEvent event, CallbackInfo ci) {
        this.issspell_research$loggedInPlayer = event.getPlayer();
        TeamAutoSync.syncOnLogin(event.getPlayer());
    }

    // 本来の処理の先頭の getOrCreateTeamData(event.getTeam()) は、パーティ用の空データを作ってしまうので、プレイヤーUUIDの方に差し替える。
    // @Redirect の target の owner は、バイトコード上の ServerQuestFile (this の型) と一致させる必要がある。
    @Redirect(method = "playerLoggedIn", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/ServerQuestFile;getOrCreateTeamData(Ldev/ftb/mods/ftbteams/api/Team;)Ldev/ftb/mods/ftbquests/quest/TeamData;"),
            remap = false)
    private TeamData issspell_research$teamDataForPlayer(ServerQuestFile self, Team team) {
        return self.getOrCreateTeamData(this.issspell_research$loggedInPlayer.getUUID());
    }

    // パーティを変えても、進行データの合成はしない。パーティ内で、研究以外のクエストの完了を同期する。
    @Inject(method = "playerChangedTeam", at = @At("HEAD"), cancellable = true, remap = false)
    private void issspell_research$playerChangedTeam(PlayerChangedTeamEvent event, CallbackInfo ci) {
        ServerQuestFile file = (ServerQuestFile) (Object) this;
        ServerPlayer player = event.getPlayer();
        TeamData data = file.getOrCreateTeamData(player);
        data.getCanEdit(player);
        TeamAutoSync.syncTeam(file, event.getTeam());
        new SyncTeamDataMessage(data, true).sendTo(player);
        ci.cancel();
    }

    // パーティ作成時に、パーティ用の空データを作らない(個人用のチームは、本来の動作のまま)。
    @Inject(method = "teamCreated", at = @At("HEAD"), cancellable = true, remap = false)
    private void issspell_research$teamCreated(TeamCreatedEvent event, CallbackInfo ci) {
        if (event.getTeam() instanceof PartyTeam) {
            ci.cancel();
        }
    }

    // 「このデータはこのプレイヤーのもの」かどうかを、プレイヤーUUIDで判定する。
    @Inject(method = "isPlayerOnTeam", at = @At("HEAD"), cancellable = true, remap = false)
    private void issspell_research$isPlayerOnTeamByUuid(Player player, TeamData teamData, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(player.getUUID().equals(teamData.getTeamId()));
    }
}
