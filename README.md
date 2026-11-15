# ISS Spell Research

> **This mod was created with the assistance of [Claude](https://claude.ai) (Anthropic).**
> このModはAnthropic社のAIモデル「[Claude](https://claude.ai)」の支援を受けて作成されました。

**English** | [日本語](#iss-spell-research-日本語)

---

## 1. Overview

ISS Spell Research (`issspell_research`) is a Forge 1.20.1 mod that lets players **unlock Iron's Spells 'n Spellbooks scrolls through research**. Research trees, conditions, and editing are handled by **FTB Quests**; this mod adds research points, research reports, a research table, and a rework of the ISS Scroll Forge. It is designed for modpacks.

## 2. Features

- **Research points** – a per-player currency. Convert enchanted books into points (up to 12 books at once) and spend them through an FTB Quests task.
- **Research reports** – an item that records one spell. It works like a license: each use of the Scroll Forge costs 1 of its 5 durability. Its colour (or texture) follows the spell's school.
- **Research table** – a block with two tabs: convert books to points / reissue reports (searchable by name and school).
- **Scroll Forge integration** – the ISS Scroll Forge only crafts scrolls from paper + ink + a research report. School focuses are no longer used.
- **Optional per-player quest progress** (off by default) – with automatic party sync for non-research quests.

## 3. How Research Works

1. Put enchanted books in the research table and convert them into research points.
2. Open the research chapter (a hidden FTB Quests chapter) from the table and complete a research node (a quest), for example by paying research points.
3. Completing the quest automatically grants a **research report** for the researched spell. A single reward, *Research Complete*, can also enable reissue for it.
4. Put paper, ink, and the report into the ISS Scroll Forge and craft the scroll. The report loses 1 durability per craft.
5. When a report is used up, reissue it at the research table for a material cost (if the quest also enabled reissue for that research).

## 4. Requirements and Installation

| | Version tested |
|---|---|
| Minecraft / Forge / Java | 1.20.1 / 47.x / 17 |
| Iron's Spells 'n Spellbooks | 1.20.1-3.16.3 (and its dependencies) |
| FTB Quests / FTB Library / FTB Teams | 2001.4.22 / 2001.2.13 / 2001.3.2 |
| Architectury | 9.2.14 (Forge) |

Install the mod on **both the client and the server**, together with all required mods above. Do not install the external *FTB Solo Quests* mod alongside this mod's built-in per-player progress (the built-in feature disables itself if it is present).

## 5. Configuration and Compatibility

Config file: `config/issspell_research-common.toml` (created on first launch).

| Key | Default | Description |
|---|---|---|
| `[personalProgress] enabled` | `false` | Make **all** FTB Quests progress per-player instead of per-team. Read at startup – **restart required**. |
| `[autoSync] enabled` | `true` | With per-player progress on, sync completed non-research quests inside a party. |
| `[conversion] levelBase`, `multiplier` | `2.0`, `1.0` | Enchanted book → research point conversion. |

**Compatibility and known limitations**

- Built against ISS 3.16.3 and FTB Quests 2001.4.22; it relies on their internals and may break with other versions.
- Several features are still awaiting verification (see [`DEVELOPMENT.md`](DEVELOPMENT.md#8-compatibility-and-known-limitations)).
- The research table GUI uses placeholder (flat-colour) graphics.

## 6. Documentation

- Detailed specifications → [`DEVELOPMENT.md`](DEVELOPMENT.md#4-feature-implementation)
- Data packs (reissue costs, school textures) → [`DEVELOPMENT.md`](DEVELOPMENT.md#45-reissue-costs-and-data-packs)
- Commands → [`DEVELOPMENT.md`](DEVELOPMENT.md#96-admin-and-debug-commands)
- Development setup, build, and testing → [`DEVELOPMENT.md`](DEVELOPMENT.md#2-build-run-and-test)

## 7. License and Credits

- Source code (and data files under `src/main/resources/data/`): **Mozilla Public License 2.0** – see [`LICENSE`](LICENSE).
- Textures (`src/main/resources/assets/**/textures/`): **standalone redistribution prohibited** – see [`LICENSE-ASSETS.md`](LICENSE-ASSETS.md).
- Everything else (code, data, and the models, blockstates, and language files under `assets/`): **Mozilla Public License 2.0**.
- Part of the built-in per-player progress is derived from [ftb-solo-quests](https://github.com/YamaArashiHZ/ftb-solo-quests) (MIT, © YamaArashi).
- Created with the assistance of Claude (Anthropic). Iron's Spells 'n Spellbooks and FTB mods are the work of their respective authors and are not bundled.

---

# ISS Spell Research (日本語)

[English](#iss-spell-research) | **日本語**

## 1. 概要

ISS Spell Research (`issspell_research`) は、**Iron's Spells 'n Spellbooks のスクロールを、研究によって解禁する** Forge 1.20.1 用MODです。研究ツリー・条件・編集画面は **FTB Quests** に任せ、このMODは、研究ポイント・研究レポート・研究テーブル・ISSのスクロールフォージの改造を担当します。モッドパックでの利用を想定しています。

## 2. 主な機能

- **研究ポイント** – プレイヤーごとの通貨。エンチャント本を(最大12冊まとめて)ポイントに変換し、FTB Questsのタスクで消費します。
- **研究レポート** – 魔法を1つ記録したアイテム。免許のように、スクロールフォージで作るたびに、耐久5のうち1を消費します。見た目は、魔法の学派の色(またはテクスチャ)になります。
- **研究テーブル** – タブが2つあるブロック。本をポイントに変換 / レポートの再発行(名前・学派で検索)。
- **スクロールフォージ連携** – ISSのスクロールフォージは、紙・インク・研究レポートでだけ、スクロールを作れます。学派フォーカスは使いません。
- **個人別のクエスト進行(オプション・既定はオフ)** – 研究以外のクエストを、パーティ内で自動同期します。

## 3. 研究の流れ

1. 研究テーブルにエンチャント本を入れて、研究ポイントに変換します。
2. テーブルから、研究チャプター(非表示のFTB Questsのチャプター)を開き、研究ノード(クエスト)を達成します。例: 研究ポイントを納品します。
3. クエストが完了すると、その魔法の**研究レポート**が、自動で渡されます。報酬「研究完了」を1つ付けるだけで、再発行の有効化も同時に行えます。
4. ISSのスクロールフォージに、紙・インク・レポートを入れて、スクロールを作ります。作るたびに、レポートの耐久が1減ります。
5. レポートを使い切ったら、研究テーブルで、素材を払って再発行します(そのクエストが、再発行を有効にしている場合)。

## 4. 動作要件・導入

| | 検証したバージョン |
|---|---|
| Minecraft / Forge / Java | 1.20.1 / 47.x / 17 |
| Iron's Spells 'n Spellbooks | 1.20.1-3.16.3 (と、その依存MOD) |
| FTB Quests / FTB Library / FTB Teams | 2001.4.22 / 2001.2.13 / 2001.3.2 |
| Architectury | 9.2.14 (Forge) |

上の必須MODと一緒に、**クライアントとサーバーの両方**に導入してください。外部の *FTB Solo Quests* は、このMODの内蔵の個人別進行と併用しないでください(導入されていると、内蔵版は自動で無効になります)。

## 5. 設定・互換性

設定ファイル: `config/issspell_research-common.toml` (初回起動で生成)

| キー | 既定 | 内容 |
|---|---|---|
| `[personalProgress] enabled` | `false` | FTB Questsの**すべての**クエストの進行を、チームではなくプレイヤーごとにします。起動時に読むため、**変更後は再起動が必要**です。 |
| `[autoSync] enabled` | `true` | 個人別進行がオンのとき、研究以外のクエストの完了を、パーティ内で同期します。 |
| `[conversion] levelBase`, `multiplier` | `2.0`, `1.0` | エンチャント本から研究ポイントへの換算。 |

**互換性・既知の制約**

- ISS 3.16.3 と FTB Quests 2001.4.22 を元に作っています。両者の内部構造に依存するため、他のバージョンでは動かない可能性があります。
- 検証が済んでいない機能があります([`DEVELOPMENT.md`](DEVELOPMENT.md#8-compatibility-and-known-limitations) 参照)。
- 研究テーブルの画面は、塗りつぶしの仮デザインです。

## 6. ドキュメント

- 詳細仕様 → [`DEVELOPMENT.md`](DEVELOPMENT.md#4-feature-implementation) (英語)
- データパック(再発行の素材、学派別テクスチャ) → [`DEVELOPMENT.md`](DEVELOPMENT.md#45-reissue-costs-and-data-packs) (英語)
- コマンド → [`DEVELOPMENT.md`](DEVELOPMENT.md#96-admin-and-debug-commands) (英語)
- 開発環境・ビルド・テスト → [`DEVELOPMENT.md`](DEVELOPMENT.md#2-build-run-and-test) (英語)

## 7. ライセンス・クレジット

- ソースコード(および `src/main/resources/data/` のデータファイル): **Mozilla Public License 2.0** → [`LICENSE`](LICENSE)
- テクスチャ(`src/main/resources/assets/**/textures/`): **単体での再配布禁止** → [`LICENSE-ASSETS.md`](LICENSE-ASSETS.md)
- それ以外(ソース、data、assets内のモデル・blockstate・言語ファイル): **Mozilla Public License 2.0**
- 内蔵の個人別進行の一部は、[ftb-solo-quests](https://github.com/YamaArashiHZ/ftb-solo-quests) (MIT, © YamaArashi) を元にしています。
- Claude (Anthropic) の支援を受けて作成されました。Iron's Spells 'n Spellbooks と FTB のMODは、それぞれの作者の著作物で、同梱していません。
