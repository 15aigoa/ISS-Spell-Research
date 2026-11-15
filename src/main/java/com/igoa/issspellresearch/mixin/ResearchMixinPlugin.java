// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.mixin;

import com.igoa.issspellresearch.personal.PersonalMode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * 内蔵の「個人別進行」のMixin(mixin.personal.*)を、適用するかどうかを決める。
 * 既定はオフ(FTB Questsのすべてのクエストを書き換える機能のため)。次のいずれかでオンになる。
 *   1. JVM引数  -Dissspell_research.personalProgress=true
 *   2. config/issspell_research-common.toml の [personalProgress] enabled = true
 * ただし、外部の SoloQuests (ftb_solo_quests) が導入されているときは、競合を避けるため、常にオフ。
 *
 * このクラスは Mixin の読み込み時(ゲームのクラスが使える前)に動くので、ゲームのクラスを参照しないこと。
 * (Forgeのコンフィグはまだ読み込まれていないため、設定ファイルを直接読む)
 */
public class ResearchMixinPlugin implements IMixinConfigPlugin {
    private static final String PERSONAL_PREFIX = "com.igoa.issspellresearch.mixin.personal.";
    private static final String CONFIG_FILE = "issspell_research-common.toml";
    private static final String CONFIG_SECTION = "personalProgress";
    private static final String CONFIG_KEY = "enabled";

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList list = LoadingModList.get();
        boolean externalSoloQuests = list != null && list.getModFileById("ftb_solo_quests") != null;
        if (externalSoloQuests) {
            PersonalMode.set(false, "外部の SoloQuests (ftb_solo_quests) が導入されているため");
        } else if (Boolean.getBoolean("issspell_research.personalProgress")) {
            PersonalMode.set(true, "JVM引数 -Dissspell_research.personalProgress=true");
        } else if (readConfigFlag()) {
            PersonalMode.set(true, "config: [personalProgress] enabled = true");
        } else {
            PersonalMode.set(false, "既定のオフ (有効にするには config の [personalProgress] enabled = true にして再起動)");
        }
    }

    /** コンフィグファイルから [personalProgress] enabled の値を読む。ファイルがない・読めない場合は false。 */
    private static boolean readConfigFlag() {
        try {
            Path file = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
            if (!Files.isRegularFile(file)) {
                return false;
            }
            String section = "";
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.substring(1, line.length() - 1).trim();
                    continue;
                }
                if (!section.equals(CONFIG_SECTION)) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0 && line.substring(0, eq).trim().equals(CONFIG_KEY)) {
                    String value = line.substring(eq + 1).trim();
                    int comment = value.indexOf('#');
                    if (comment >= 0) {
                        value = value.substring(0, comment).trim();
                    }
                    return value.equalsIgnoreCase("true");
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // 読めない場合はオフのまま
        }
        return false;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(PERSONAL_PREFIX)) {
            return PersonalMode.isEnabled();
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
