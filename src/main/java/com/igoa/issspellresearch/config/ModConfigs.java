// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** コンフィグ(config/issspell_research-common.toml)。換算レートや自動同期の設定はここで調整する。 */
public final class ModConfigs {
    private ModConfigs() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue LEVEL_BASE;
    public static final ForgeConfigSpec.DoubleValue MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue PERSONAL_PROGRESS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue AUTO_SYNC_ENABLED;
    public static final ForgeConfigSpec.BooleanValue AUTO_SYNC_DEDICATED_ONLY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Enchanted book -> research points conversion").push("conversion");
        LEVEL_BASE = b.comment("Points per enchantment = levelBase ^ (level - 1) * multiplier (summed over all enchantments on the book)")
                .defineInRange("levelBase", 2.0D, 1.0D, 10.0D);
        MULTIPLIER = b.comment("Global multiplier applied to the converted points")
                .defineInRange("multiplier", 1.0D, 0.0D, 1_000_000.0D);
        b.pop();

        b.comment("Per-player FTB Quests progress (derived from ftb-solo-quests, MIT).",
                        "WARNING: when enabled, progress of ALL FTB Quests quests becomes per-player instead of per-team.",
                        "Off by default. This value is read directly from this file by the mixin plugin at startup, so a restart is required.",
                        "It is always disabled when the external 'ftb_solo_quests' mod is installed. JVM flag alternative: -Dissspell_research.personalProgress=true")
                .push("personalProgress");
        PERSONAL_PROGRESS_ENABLED = b.comment("Enable per-player progress (and the party auto sync below).")
                .define("enabled", false);
        b.pop();

        b.comment("Automatic party sync of NON-research quests (completed quests only). Research chapters are never synced.")
                .push("autoSync");
        AUTO_SYNC_ENABLED = b.comment("Enable the automatic party sync. Takes effect after a restart.")
                .define("enabled", true);
        AUTO_SYNC_DEDICATED_ONLY = b.comment("If true, the sync only runs on dedicated servers (it is off in single player / LAN worlds).")
                .define("dedicatedServerOnly", false);
        b.pop();

        SPEC = b.build();
    }
}
