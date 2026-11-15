// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.reissue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.igoa.issspellresearch.IssSpellResearch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 再発行の必要素材。データパックで、魔法ごとに変えられる。
 *
 * 置き場所: data/<魔法の名前空間>/research_reissue/<魔法のパス>.json
 *   例: data/irons_spellbooks/research_reissue/fireball.json  → 魔法 irons_spellbooks:fireball だけの素材
 * 全魔法の既定: data/issspell_research/research_reissue/default.json (魔法ごとのファイルがなければ、これを使う)
 *
 * 形式:
 *   { "ingredients": [
 *       { "ingredient": { "item": "irons_spellbooks:arcane_essence" }, "count": 10 },
 *       { "ingredient": { "item": "minecraft:paper" }, "count": 3 } ] }
 * "ingredient" はレシピと同じ書き方(item / tag)。素材を空にすると、無料で再発行できる。
 */
public class ReissueCosts extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(IssSpellResearch.MODID);

    public static final String DIRECTORY = "research_reissue";
    public static final ResourceLocation DEFAULT_ID = new ResourceLocation(IssSpellResearch.MODID, "default");
    private static final int MAX_REQUIREMENTS = 4;

    private static volatile List<ReissueRequirement> defaultCost = null;
    private static volatile Map<ResourceLocation, List<ReissueRequirement>> perSpell = Map.of();

    public ReissueCosts() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, List<ReissueRequirement>> loaded = new HashMap<>();
        List<ReissueRequirement> newDefault = null;
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                List<ReissueRequirement> parsed = parse(entry.getValue());
                if (entry.getKey().equals(DEFAULT_ID)) {
                    newDefault = parsed;
                } else {
                    loaded.put(entry.getKey(), parsed);
                }
            } catch (JsonParseException | IllegalStateException e) {
                LOGGER.error("[issspell_research] 再発行の素材の読み込みに失敗しました ({}): {}", entry.getKey(), e.getMessage());
            }
        }
        perSpell = loaded;
        defaultCost = newDefault;
        LOGGER.info("[issspell_research] 再発行の素材: 魔法ごとの設定 {} 件, 既定 {}", loaded.size(),
                newDefault == null ? "組み込み(秘術のエッセンス10 + 紙3)" : "データパックの default.json");
    }

    private static List<ReissueRequirement> parse(JsonElement element) {
        JsonObject root = GsonHelper.convertToJsonObject(element, "research_reissue");
        JsonArray array = GsonHelper.getAsJsonArray(root, "ingredients", new JsonArray());
        List<ReissueRequirement> list = new ArrayList<>();
        for (JsonElement item : array) {
            if (list.size() >= MAX_REQUIREMENTS) {
                break;
            }
            JsonObject object = GsonHelper.convertToJsonObject(item, "ingredients[]");
            Ingredient ingredient = Ingredient.fromJson(object.get("ingredient"));
            int count = Math.max(1, Math.min(64, GsonHelper.getAsInt(object, "count", 1)));
            list.add(new ReissueRequirement(ingredient, count));
        }
        return list;
    }

    /** 魔法の再発行に必要な素材。魔法ごとの設定 → 既定(default.json) → 組み込みの既定、の順に探す。 */
    public static List<ReissueRequirement> costFor(ResourceLocation spell) {
        List<ReissueRequirement> specific = perSpell.get(spell);
        if (specific != null) {
            return specific;
        }
        List<ReissueRequirement> fallback = defaultCost;
        return fallback != null ? fallback : builtinDefault();
    }

    /** データパックに default.json がないときの、組み込みの既定: 秘術のエッセンス10個 + 紙3枚。 */
    private static List<ReissueRequirement> builtinDefault() {
        List<ReissueRequirement> list = new ArrayList<>();
        Item essence = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks", "arcane_essence"));
        if (essence != null && essence != Items.AIR) {
            list.add(new ReissueRequirement(Ingredient.of(essence), 10));
        }
        list.add(new ReissueRequirement(Ingredient.of(Items.PAPER), 3));
        return list;
    }
}
