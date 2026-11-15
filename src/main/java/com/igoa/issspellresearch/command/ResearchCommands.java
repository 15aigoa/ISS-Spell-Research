// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.command;

import com.igoa.issspellresearch.data.ResearchPointsData;
import com.igoa.issspellresearch.data.ResearchUnlockData;
import com.igoa.issspellresearch.personal.TeamAutoSync;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import com.igoa.issspellresearch.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TextColor;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 検証用コマンド(OP権限レベル2以上)。
 *  /issresearch points get|add|set <player> [amount]
 *  /issresearch report give <player> <spell_id>
 *  /issresearch sync  (オンラインのパーティで、研究以外のクエストの完了を同期する)
 *  /issresearch schools  (登録されている魔法の学派の一覧。他MODが追加した学派も含む)
 *  /issresearch reissue enable|disable <player> <spell_id>  (再発行の有効化を、コマンドで切り替える。検証用)
 */
public final class ResearchCommands {
    private ResearchCommands() {}

    /** 魔法IDの候補: ISSに登録されていて有効な、スクロール製造ができる魔法。 */
    private static final SuggestionProvider<CommandSourceStack> SPELL_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(
                    SpellRegistry.getEnabledSpells().stream()
                            .filter(spell -> !spell.equals(SpellRegistry.none()) && spell.allowCrafting())
                            .map(spell -> spell.getSpellResource()),
                    builder);

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("issresearch")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("points")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> get(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                                .executes(ctx -> add(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        LongArgumentType.getLong(ctx, "amount"))))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                                .executes(ctx -> set(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        LongArgumentType.getLong(ctx, "amount")))))))
                .then(Commands.literal("report")
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("spell", ResourceLocationArgument.id())
                                                .suggests(SPELL_SUGGESTIONS)
                                                .executes(ctx -> giveReport(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        ResourceLocationArgument.getId(ctx, "spell")))))))
                .then(Commands.literal("reissue")
                        .then(Commands.literal("enable")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("spell", ResourceLocationArgument.id())
                                                .suggests(SPELL_SUGGESTIONS)
                                                .executes(ctx -> setReissue(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        ResourceLocationArgument.getId(ctx, "spell"), true)))))
                        .then(Commands.literal("disable")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("spell", ResourceLocationArgument.id())
                                                .suggests(SPELL_SUGGESTIONS)
                                                .executes(ctx -> setReissue(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        ResourceLocationArgument.getId(ctx, "spell"), false))))))
                .then(Commands.literal("schools")
                        .executes(ctx -> schools(ctx.getSource())))
                .then(Commands.literal("sync")
                        .executes(ctx -> sync(ctx.getSource()))));
    }

    /** 登録されている学派の一覧(ID・表示色・魔法の数)。研究レポートの学派別テクスチャの対象を、実行時の正確な情報で確認するために使う。 */
    private static int schools(CommandSourceStack src) {
        var registry = SchoolRegistry.REGISTRY.get();
        List<ResourceLocation> ids = new ArrayList<>(registry.getKeys());
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        src.sendSuccess(() -> Component.literal("登録されている学派: " + ids.size() + " 件"), false);
        for (ResourceLocation id : ids) {
            SchoolType school = registry.getValue(id);
            TextColor color = school.getDisplayName().getStyle().getColor();
            String hex = color == null ? "-" : String.format("#%06X", color.getValue());
            int spells = SpellRegistry.getSpellsForSchool(school).size();
            src.sendSuccess(() -> Component.literal(" - " + id + "   色 " + hex + "   魔法 " + spells + " 個"), false);
        }
        src.sendSuccess(() -> Component.literal("モデルの置き場: assets/<任意の名前空間>/models/item/research_report_schools/<学派の名前空間>/<学派のパス>.json"), false);
        return ids.size();
    }

    private static int setReissue(CommandSourceStack src, ServerPlayer target, ResourceLocation spell, boolean enable) {
        ResearchUnlockData data = ResearchUnlockData.get(src.getServer());
        boolean changed = enable ? data.enableReissue(target.getUUID(), spell) : data.disableReissue(target.getUUID(), spell);
        src.sendSuccess(() -> Component.translatable(
                enable ? "issspell_research.command.reissue.enable" : "issspell_research.command.reissue.disable",
                spell.toString(), target.getDisplayName(), changed ? "" : " (変更なし)"), true);
        return changed ? 1 : 0;
    }

    private static int sync(CommandSourceStack src) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        String reason = file == null ? "クエストのデータがまだ読み込まれていません" : TeamAutoSync.disabledReason(file);
        if (reason != null) {
            src.sendFailure(Component.translatable("issspell_research.command.sync.disabled", reason));
            return 0;
        }
        int teams = TeamAutoSync.syncAllOnline(src.getServer());
        src.sendSuccess(() -> Component.translatable("issspell_research.command.sync", teams), true);
        return teams;
    }

    private static int get(CommandSourceStack src, ServerPlayer target) {
        long v = ResearchPointsData.get(src.getServer()).get(target.getUUID());
        src.sendSuccess(() -> Component.translatable("issspell_research.command.points.get",
                target.getDisplayName(), v), false);
        return clamp(v);
    }

    private static int add(CommandSourceStack src, ServerPlayer target, long amount) {
        long v = ResearchPointsData.get(src.getServer()).add(target.getUUID(), amount);
        src.sendSuccess(() -> Component.translatable("issspell_research.command.points.add",
                amount, target.getDisplayName(), v), true);
        return clamp(v);
    }

    private static int set(CommandSourceStack src, ServerPlayer target, long amount) {
        ResearchPointsData data = ResearchPointsData.get(src.getServer());
        data.set(target.getUUID(), amount);
        long v = data.get(target.getUUID());
        src.sendSuccess(() -> Component.translatable("issspell_research.command.points.set",
                target.getDisplayName(), v), true);
        return clamp(v);
    }

    private static int giveReport(CommandSourceStack src, ServerPlayer target, ResourceLocation spell) {
        ItemStack stack = ModItems.createReport(spell);
        ItemHandlerHelper.giveItemToPlayer(target, stack);
        src.sendSuccess(() -> Component.translatable("issspell_research.command.report.give",
                spell.toString(), target.getDisplayName()), true);
        return 1;
    }

    private static int clamp(long v) {
        return (int) Math.min(v, Integer.MAX_VALUE);
    }
}
