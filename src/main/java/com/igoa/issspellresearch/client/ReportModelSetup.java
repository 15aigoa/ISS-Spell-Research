// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.client;

import com.igoa.issspellresearch.IssSpellResearch;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 研究レポートの「学派ごとの専用モデル(テクスチャ)」を、リソースから自動で見つけて登録する。
 *
 * 置き場所(どのMODの名前空間でも、リソースパックでもよい):
 *   assets/<任意の名前空間>/models/item/research_report_schools/<学派の名前空間>/<学派のパス>.json
 *   例: assets/issspell_research/models/item/research_report_schools/irons_spellbooks/fire.json  → 学派 irons_spellbooks:fire
 * 専用モデルがない学派は、従来の「色付け」の研究レポートのまま表示される。
 */
@Mod.EventBusSubscriber(modid = IssSpellResearch.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ReportModelSetup {
    private static final Logger LOGGER = LoggerFactory.getLogger(IssSpellResearch.MODID);

    /** models/ 以下の、学派別モデルの置き場。 */
    public static final String SCHOOL_MODEL_DIR = "item/research_report_schools";

    /** 学派ID → モデルの場所。リソースの再読み込みのたびに作り直す。 */
    private static final Map<ResourceLocation, ResourceLocation> SCHOOL_MODELS = new HashMap<>();

    private ReportModelSetup() {}

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        SCHOOL_MODELS.clear();
        String listDir = "models/" + SCHOOL_MODEL_DIR;
        Map<ResourceLocation, Resource> found = Minecraft.getInstance().getResourceManager()
                .listResources(listDir, id -> id.getPath().endsWith(".json"));
        for (ResourceLocation file : found.keySet()) {
            // 例: models/item/research_report_schools/irons_spellbooks/fire.json → relative = irons_spellbooks/fire
            String relative = file.getPath().substring(listDir.length() + 1, file.getPath().length() - ".json".length());
            int slash = relative.indexOf('/');
            if (slash <= 0 || slash == relative.length() - 1) {
                LOGGER.warn("[issspell_research] 学派別モデルの置き場が不正です(<学派の名前空間>/<学派のパス>.json の形にしてください): {}", file);
                continue;
            }
            ResourceLocation schoolId = ResourceLocation.tryParse(relative.substring(0, slash) + ":" + relative.substring(slash + 1));
            if (schoolId == null) {
                continue;
            }
            ResourceLocation modelLocation = new ResourceLocation(file.getNamespace(), SCHOOL_MODEL_DIR + "/" + relative);
            SCHOOL_MODELS.put(schoolId, modelLocation);
            event.register(modelLocation);
        }
        LOGGER.info("[issspell_research] 学派別の研究レポートモデル: {} 件 {}", SCHOOL_MODELS.size(), SCHOOL_MODELS.keySet());
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation key = new ModelResourceLocation(new ResourceLocation(IssSpellResearch.MODID, "research_report"), "inventory");
        BakedModel base = event.getModels().get(key);
        if (base == null) {
            return;
        }
        Map<ResourceLocation, BakedModel> bySchool = new HashMap<>();
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : SCHOOL_MODELS.entrySet()) {
            BakedModel model = event.getModels().get(entry.getValue());
            if (model != null) {
                bySchool.put(entry.getKey(), model);
            }
        }
        event.getModels().put(key, new ResearchReportBakedModel(base, bySchool));
        logSchoolCoverage();
    }

    /**
     * 確認用のログ: 学派別モデルのIDが、ISSに登録されていない(綴りの間違いなど)ものを警告し、
     * 専用モデルがなく、色付け方式になる学派を一覧にする。
     */
    private static void logSchoolCoverage() {
        var registry = SchoolRegistry.REGISTRY.get();
        for (ResourceLocation schoolId : SCHOOL_MODELS.keySet()) {
            if (!registry.containsKey(schoolId)) {
                LOGGER.warn("[issspell_research] 学派別モデル {} の学派IDは、登録されていません。綴りが違うか、そのMODが入っていません。"
                        + " (/issresearch schools で確認してください)", schoolId);
            }
        }
        List<ResourceLocation> tinted = new ArrayList<>();
        for (ResourceLocation schoolId : registry.getKeys()) {
            if (!SCHOOL_MODELS.containsKey(schoolId)) {
                tinted.add(schoolId);
            }
        }
        LOGGER.info("[issspell_research] 専用モデルがなく、色付け方式になる学派: {}", tinted);
    }
}
