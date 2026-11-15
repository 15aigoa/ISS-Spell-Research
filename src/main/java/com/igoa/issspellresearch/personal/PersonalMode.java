// SPDX-License-Identifier: MPL-2.0
package com.igoa.issspellresearch.personal;

/**
 * 「個人別進行(内蔵)」を有効にするかの判定結果。Mixinプラグインが、Mixinの読み込み時に1度だけ決めて、ここに入れる。
 * Mixinの適用は、Forgeのコンフィグが読み込まれるより前に行われるため、プラグインが設定ファイルを直接読んで決めている。
 * このクラスは、プラグインから早い段階で読み込まれるので、ゲームのクラスを参照しないこと。
 */
public final class PersonalMode {
    private static volatile boolean enabled;
    private static volatile String reason = "未判定";

    private PersonalMode() {}

    public static void set(boolean enabled, String reason) {
        PersonalMode.enabled = enabled;
        PersonalMode.reason = reason;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String reason() {
        return reason;
    }
}
