# Development Guide

Technical documentation for `issspell_research` (ISS Spell Research). For an overview, see [`README.md`](README.md).

- [1. Development Environment](#1-development-environment)
- [2. Build, Run, and Debug](#2-build-run-and-debug)
- [3. Architecture and Package Layout](#3-architecture-and-package-layout)
- [4. Feature Implementation](#4-feature-implementation)
- [5. Mixins and External Mod Integration](#5-mixins-and-external-mod-integration)
- [6. Saved Data and Networking](#6-saved-data-and-networking)
- [7. Personal Progress and Auto Sync](#7-personal-progress-and-auto-sync)
- [8. Compatibility and Known Limitations](#8-compatibility-and-known-limitations)
- [9. Testing](#9-testing)
- [10. License and Third-Party Code](#10-license-and-third-party-code)

---

## 1. Development Environment

| Item | Value |
|---|---|
| Minecraft / Forge | 1.20.1 / 47.x (developed on 47.4.28) |
| Java | 17 |
| Build | ForgeGradle 6, official (Mojang) mappings |
| Mixin | Mixin 0.8.5 via MixinGradle 0.7 (annotation processor generates `issspell_research.refmap.json`) |
| Mod ID / base package | `issspell_research` / `com.igoa.issspellresearch` (future mods: `com.igoa.<modname>`) |

### Required mods and verified versions

All of these must be on the compile classpath **and** in the run configuration (they are loaded in `runClient` / `runServer`).

| Mod | Version | Jar file name (as distributed) |
|---|---|---|
| Iron's Spells 'n Spellbooks | 1.20.1-3.16.3 | `irons_spellbooks-1.20.1-3.16.3.jar` |
| irons_lib (ISS dependency) | 1.20.1-2.3.0 | `irons_lib-1.20.1-2.3.0.jar` |
| GeckoLib (ISS dependency) | 4.8.4 | `geckolib-forge-1.20.1-4.8.4.jar` |
| Player Animation Lib (ISS dependency) | 1.0.2-rc1+1.20 | `player-animation-lib-forge-1.0.2-rc1+1.20.jar` |
| Curios (ISS dependency) | 5.14.1+1.20.1 | `curios-forge-5.14.1+1.20.1.jar` |
| FTB Quests | 2001.4.22 | `ftb-quests-forge-2001.4.22.jar` |
| FTB Library | 2001.2.13 | `ftb-library-forge-2001.2.13.jar` |
| FTB Teams | 2001.3.2 | `ftb-teams-forge-2001.3.2.jar` |
| Architectury API | 9.2.14 | `architectury-9.2.14-forge.jar` |

`mods.toml` declares mandatory dependencies on `forge`, `minecraft`, `ftbquests`, `ftbteams`, and `irons_spellbooks`.
(FTB Quests' optional `itemfilters` is not required.)

### Local dependency jars (`libs/`)

The jars above are third-party works (see [section 10](#10-license-and-third-party-code)) and are **not** committed. Copy them from a pack's `mods/` folder into `libs/` at the project root and reference them with a `flatDir` repository and `fg.deobf` (they are SRG-named release jars and must be deobfuscated for the dev environment).

```gradle
repositories {
    flatDir { dir 'libs' }
}

dependencies {
    implementation fg.deobf('local:irons_spellbooks:1.20.1-3.16.3')
    implementation fg.deobf('local:ftb-quests-forge:2001.4.22')
    implementation fg.deobf('local:ftb-library-forge:2001.2.13')
    implementation fg.deobf('local:ftb-teams-forge:2001.3.2')
    implementation fg.deobf('local:architectury:9.2.14:forge')
    // plus irons_lib, geckolib, player-animation-lib, curios
}
```

How flatDir resolves a notation: the file name is read as `<name>-<version>[-<classifier>].jar`. The first element (`local`) is ignored.

- `architectury-9.2.14-forge.jar` → `architectury:9.2.14:forge` (`forge` is a classifier).
- `player-animation-lib-forge-1.0.2-rc1+1.20.jar` → `player-animation-lib-forge:1.0.2-rc1+1.20` (no classifier; `forge` is part of the name).
- Check the **actual** file names in `libs/`; some download sites replace `.` / `+` with `_`.

Add `libs/` to `.gitignore`.

---

## 2. Build, Run, and Debug

```
gradlew build        # compile, run the Mixin annotation processor (refmap), reobfuscate -> build/libs/*.jar
gradlew runClient    # development client (game directory: run/)
gradlew runServer    # development dedicated server
```

- Logs: `run/logs/latest.log`. Generated config: `run/config/issspell_research-common.toml`.
- After changing resources, run `gradlew clean` before `runClient` if the change does not appear.
- Startup lines prefixed with `[issspell_research]` report the personal-progress mode (with the reason), the reissue cost source, and the per-school report models found.
- Multiplayer checks use a dedicated server with `online-mode=false` and two clients under different user names (`--username`, separate game directories). Parties are created with `/ftbteams party create`.

### Debug commands

Permission level 2 (OP). Spell ids are auto-completed.

| Command | Description |
|---|---|
| `/issresearch points get\|add\|set <player> [amount]` | inspect or change research points |
| `/issresearch report give <player> <spell_id>` | give a research report |
| `/issresearch reissue enable\|disable <player> <spell_id>` | toggle reissue for a spell |
| `/issresearch sync` | sync non-research quests in online parties; prints the reason if disabled |

## 3. Architecture and Package Layout

```
com.igoa.issspellresearch
├─ IssSpellResearch        entry point: registries, events, config, reload listener
├─ registry/               ModBlocks, ModItems, ModMenus (DeferredRegister)
├─ block/                  ResearchTableBlock (opens the menu)
├─ menu/                   ResearchTableMenu (slots, point balance, button actions)
├─ item/                   ResearchReportItem (NBT, use limit stored per item)
├─ data/                   ResearchPointsData, ResearchUnlockData (SavedData)
├─ quest/                  FTB Quests task/reward types (incl. combined reward), research-chapter detection
├─ reissue/                reissue costs (data pack), validation, execution
├─ personal/               PersonalMode, TeamAutoSync (per-player progress support)
├─ mixin/                  ISS Scroll Forge mixins, FTB Quests display mixin, mixin plugin
│  └─ personal/            FTB Quests per-player progress mixins (opt-in)
├─ client/                 screens, report models and tint, spell display and chooser, research-chapter opener
├─ command/ config/ util/  admin commands, ForgeConfigSpec, enchant-point calculator
```

- **Entry point and registries** – `IssSpellResearch` registers the deferred registers, the config, commands, the tick listener, and the reload listener. FTB Quests types and `TeamAutoSync` events are registered inside `enqueueWork` (see 4.4).
- **`block/`, `menu/`, `item/`** – the table block holds no state; it builds the menu per player. The menu owns the 12-slot book container. The item class owns report NBT handling and durability.
- **`data/`, `quest/`, `reissue/`** – `data/` stores per-player values; `quest/` exposes them to FTB Quests; `reissue/` reads costs from data packs and performs reissue.
- **`personal/`, `mixin/`, `client/`** – `personal/` decides whether the opt-in progress module is on and syncs quests; `mixin/` changes ISS / FTB Quests behaviour; `client/` holds everything that must not load on a dedicated server.

### Flow

```
books --(table menu)--> ResearchPointsData --(ResearchPointsTask in FTB Quests)--> quest complete
   quest complete --(ResearchReportReward, auto-claimed in hidden chapter)--> research report item
   research report --(Scroll Forge mixins)--> scroll (report durability -1)
   ReissueUnlockReward --> ResearchUnlockData --(table "reissue" tab + ReissueCosts)--> new report
```

---

## 4. Feature Implementation

### 4.1 Research points

- **Storage**: `ResearchPointsData` (SavedData) keeps a `Map<UUID, Long>`. API: `get`, `set`, `add` (saturating), `consume(id, max)` (returns the amount actually taken). Zero balances are not stored.
- **Conversion** (`util/EnchantPointCalculator`): only `minecraft:enchanted_book`. Points = `round(multiplier × Σ levelBase^(min(level, 60) − 1))` over the book's enchantments, capped at 1,000,000,000 per book. Parameters come from `ModConfigs` (`[conversion]`).
- **Spending**: `ResearchPointsTask extends Task implements ISingleLongValueTask` (type `issspell_research:points`).
  - `consumesResources()` is `true` (the quest button submits; no login auto-check), same as FTB Quests' XP task.
  - `submitTask`: `remaining = value − progress`; consumes `min(balance, remaining)` and calls `teamData.addProgress` (partial submission is allowed). An insufficient balance shows an action-bar message.

### 4.2 Research report item

- `issspell_research:research_report` is registered with `durability(DEFAULT_MAX_USES)` (5). That value only enables damage handling; the real limit comes from `getMaxDamage`, which reads the `MaxUses` tag written at creation and falls back to `[research] reportMaxUses`. Storing the limit on the stack keeps the client bar and the server limit in agreement. Changing the config affects only reports created afterwards.
- NBT: `{Spells: ["<spell id>", ...]}`. The data structure is a list, but **the design is one spell per report**.
- `ResearchReportItem.consumeUse(stack)`: `damage + 1 >= maxDamage` → `shrink(1)`; else `setDamageValue`. It uses no randomness so the client and server agree (Unbreaking does not apply).
- `isRepairable` and `isBookEnchantable` return `false` (no durability recovery via anvil, grindstone, or Mending).
- Helpers: `getSpells`, `hasSpells`, `containsSpell(stack, spellId)`.

### 4.3 Research table GUI

- **Recipe** (`data/issspell_research/recipes/research_table.json`, shaped): book on top; `irons_spellbooks:arcane_ingot` – enchanting table – arcane ingot in the middle row; any planks (`#minecraft:planks`) – bookshelf – any planks at the bottom. In ISS 3.16.3 `arcane_ingot` is an **item**, not a tag; if a pack defines a tag with that name, change the key to `{ "tag": ... }`. Packs can override the recipe with a data pack or KubeJS.
- **Block**: `ResearchTableBlock.use` builds the player's reissue entries (`ReissueService.buildEntries`) and opens the menu with `NetworkHooks.openScreen(player, provider, buf -> { pos; entries })`. There is no block entity (like the vanilla enchanting table); the book container belongs to each menu instance, so players never share slots. `removed()` returns leftover books.
- **Menu** (`ResearchTableMenu`):
  - 12 book slots (`BOOK_COLS = 4`, `BOOK_ROWS = 3`, origin `BOOK_X = 12`, `BOOK_Y = 34`), accepting only enchanted books; then the player inventory (main rows at y = 140, hotbar at y = 198).
  - Button ids: `0` = convert; `100 + n` = reissue entry `n` (index into the list sent when the menu opened).
  - Convert sums all convertible books, clears those slots, leaves zero-value books in place, and reports `+points (n books)`.
  - The client-only flag `reissueTab` makes the book slots inactive (`Slot.isActive`) while the reissue tab is shown.
- **Screen** (`ResearchTableScreen`, 176 × 222, no texture files):
  - Tabs are drawn on the **left side** of the main panel like creative-inventory tabs. They sit outside `imageWidth`, so `hasClickedOutside` is overridden (otherwise a tab click would drop the carried item).
  - Tab 1 (enchanted-book icon): book slots, convert button, up to 3 research-chapter buttons (`ResearchChapterFinder`).
  - Tab 2 (evocation research-report icon): search box (space-separated tokens, all must match the spell name, school name, or id), scrollable list sorted by school then name, cost row with item icons (red count = missing), and the reissue button. Slot clicks and shift-clicks are ignored on this tab.
- **Point balance** is sent through four `DataSlot`s (16 bits each) and reassembled on the client.

### 4.4 FTB Quests tasks and rewards

| Type | ID | Class |
|---|---|---|
| Task | `issspell_research:points` | `ResearchPointsTask` |
| Reward | `issspell_research:research_report` | `ResearchReportReward` (`spell`) |
| Reward | `issspell_research:reissue_unlock` | `ReissueUnlockReward` (`spell`) |
| Reward | `issspell_research:research_complete` | `ResearchCompleteReward` (`spell`): research report **and** reissue unlock in one reward |

- Registered with `TaskTypes.register` / `RewardTypes.register` inside `FMLCommonSetupEvent#enqueueWork`. **`FMLCommonSetupEvent` handlers of different mods run in parallel**, and Architectury / FTB Quests registries are not thread-safe; registering off-thread was observed to corrupt Architectury's internal map and break login (`ArrayIndexOutOfBoundsException` in `NetworkManagerImpl.sendSyncPacket`).
- **Combined reward**: `ResearchCompleteReward extends ResearchReportReward`. `claim` gives the report (`super.claim`) and then calls `ResearchUnlockData.enableReissue`. Because it extends `ResearchReportReward`, the `instanceof ResearchReportReward` checks that define a research chapter (`ResearchChapters`, `ResearchChapterFinder`) cover it without changes. Use one `research_complete` reward instead of a `research_report` plus a `reissue_unlock`.
- **Spell id chooser in the quest editor**: `SpellIdConfigs.addSpellChooser` (client only) replaces the free-text field of the three spell rewards. It builds a `NameMap<String>` of the enabled, craftable ISS spells (icon from `AbstractSpell.getSpellIconResource()`, label = name + id + school) and registers it with `ConfigGroup.addEnum`. FTB Library's `EnumConfig` opens a searchable selection screen when there are more than 16 values, so the field is searchable by name, id, or school. The stored value is still the spell id string; an id that is not in the list (for example a mod that was removed) is kept and shown in red.
- **Display of spells in FTB Quests** (`client/SpellDisplay`): the three spell rewards use the spell's own icon (`AbstractSpell.getSpellIconResource()`) and its localized name for `getAltIcon` / `getAltTitle`, falling back to the report item icon / the raw id if the spell is not registered. FTB Quests derives a quest's default title from its **first task** and its default icon from its **tasks**, never from rewards, so `QuestDisplayMixin` (client, section 5) overrides `Quest.getAltTitle` / `getAltIcon` for quests that have a research report reward. A title or icon set by hand in the editor still wins. It can be turned off with `[display] questUsesSpellDisplay = false`.
- Task/reward serialization follows FTB Quests' own pattern (`writeData`/`readData` for NBT, `writeNetData`/`readNetData` for the network, `fillConfigGroup` for the editor).
- **Research chapter** = a chapter containing at least one quest with a `ResearchReportReward` (`ResearchChapters`). No configuration needed. Setting the chapter to "always invisible" hides it from the quest book list and makes FTB Quests auto-claim its rewards.
- The table opens a chapter with `ClientQuestFile.openBookToQuestObject(chapter.id)`. This is unavailable if the pack disables the quest GUI (`disable_gui`).
- Reward icons use `ItemIcon.getItemIcon(report stack)`, so they pick up the tint / school model.

### 4.5 Reissue costs and data packs

**Flow**: `ReissueUnlockReward.claim` → `ResearchUnlockData.enableReissue(uuid, spell)`. Opening the table → `ReissueService.buildEntries` (only spells that exist in ISS, are enabled, and `allowCrafting()`), each with its cost. Pressing the button → `ReissueService.tryReissue` re-checks the unlock, re-reads the **current** cost on the server, verifies the inventory, consumes materials, and gives a fresh report (`ItemHandlerHelper.giveItemToPlayer`).

**Cost data pack** (`ReissueCosts extends SimpleJsonResourceReloadListener`, directory `research_reissue`, reloaded by `/reload`):

| Purpose | Path |
|---|---|
| Default for all spells | `data/issspell_research/research_reissue/default.json` |
| Per spell | `data/<spell namespace>/research_reissue/<spell path>.json` (e.g. `data/irons_spellbooks/research_reissue/fireball.json`) |

Resolution order: per-spell file → `default.json` → built-in default (10 × `irons_spellbooks:arcane_essence` + 3 × `minecraft:paper`).

```json
{
  "ingredients": [
    { "ingredient": { "item": "irons_spellbooks:arcane_essence" }, "count": 10 },
    { "ingredient": { "item": "minecraft:paper" }, "count": 3 }
  ]
}
```

- `ingredient` uses recipe syntax (`Ingredient.fromJson`: `item` or `tag`); max 4 entries; `count` clamped to 1–64; `"ingredients": []` means free. Invalid files are logged and skipped.
- The network format of a requirement is `Ingredient.toNetwork` + `varint count`. KubeJS can supply the same files under `kubejs/data/`.

### 4.6 Report appearance and school models

- **Default (tint)**: `models/item/research_report.json` has `layer0` (untinted base) and `layer1` (overlay in white/grey shades). `ClientSetup` registers an `ItemColor` that tints `tintindex 1` with the school's display colour (`ReportColors`); with no spell data it falls back to purple (`0xA078E6`).
- **Per-school models** (optional, no code needed): place
  `assets/<any namespace>/models/item/research_report_schools/<school namespace>/<school path>.json`
  (e.g. `.../irons_spellbooks/fire.json` for `irons_spellbooks:fire`). `ReportModelSetup` lists these resources in `ModelEvent.RegisterAdditional`, and `ModelEvent.ModifyBakingResult` wraps the report's baked model in `ResearchReportBakedModel` (a `BakedModelWrapper` whose `ItemOverrides` picks the school model by the recorded spell, else the default). Schools without a model keep the tint model.
- The startup log lists the schools that have no dedicated model (`[issspell_research] 専用モデルがなく...`) and warns about school ids that ISS does not register. Use those lines, or the school ids in the ISS sources, to find the ids of the current pack.
- Reload resources with `F3 + T` after adding models.

### 4.7 Scroll Forge changes

ISS `ScrollForgeMenu` has four slots created as anonymous `Slot` subclasses, in source order: ink (`$1`), paper (`$2`), focus (`$3`), result (`$4`).

- **Focus slot** accepts only items with recorded spells (`ResearchReportItem.hasSpells`).
- **Result calculation** (`setupResultSlot` replaced): requires paper, ink (`InkItem`), a report that `containsSpell`, `spell.allowCrafting()`, and `inkRarity >= spell.getMinRarity()`. The scroll is `ItemRegistry.SCROLL` with `ISpellContainer.createScrollContainer(spell, spell.getMinLevelForRarity(inkRarity), stack)`.
- **Taking the result**: ISS calls `Slot.remove(1)` on ink, paper, then focus. The third call is redirected to `ResearchReportItem.consumeUse` + `slot.set(...)` so ISS's change handling recalculates the result.
- **Screen** (client): the spell list is built from the report's spells instead of the focus school, and `menuSlotsChanged` also compares NBT (all reports are the same `Item`).

---

## 5. Mixins and External Mod Integration

Config: `issspell_research.mixins.json`, `"defaultRequire": 1` (a missing target fails at startup instead of silently doing nothing), a plugin (`ResearchMixinPlugin`), common list + `client` list.

| Mixin | Target | Change |
|---|---|---|
| `ScrollForgeFocusSlotMixin` | ISS `ScrollForgeMenu$3.mayPlace` | only research reports can be placed |
| `ScrollForgeResultSlotMixin` | ISS `ScrollForgeMenu$4.onTake` | redirect the 3rd `Slot.remove(I)` (ordinal 2) → report durability |
| `ScrollForgeMenuMixin` | ISS `ScrollForgeMenu.setupResultSlot` | replace result calculation (HEAD, cancel) |
| `ScrollForgeScreenMixin` (client) | ISS `ScrollForgeScreen.generateSpellList`, `menuSlotsChanged` | list from report; redirect `ItemStack.is(TagKey)` and `Stream.toList()` |
| `QuestDisplayMixin` (client) | FTB Quests `Quest.getAltTitle`, `Quest.getAltIcon` | default quest title / icon from the spell of its research report reward; `require = 0`, so a missing target only skips the change |
| `personal.PersonalBaseQuestFileMixin` | FTB Quests `BaseQuestFile.getOrCreateTeamData(Entity)` | key by player UUID (server file only) |
| `personal.PersonalServerQuestFileMixin` | FTB Quests `ServerQuestFile.playerLoggedIn / playerChangedTeam / teamCreated / isPlayerOnTeam` | per-player login, team change, party creation, ownership |
| `personal.PersonalInventoryListenerMixin` | FTB Quests `FTBQuestsInventoryListener.detect` | task detection with per-player data (copy of 2001.4.22's `detect`) |

### `remap`, `@At`, and `defaultRequire`

- Targets that belong to ISS / FTB Quests are **not obfuscated**, so injectors that name them use `remap = false` (otherwise the annotation processor errors: *Unable to locate obfuscation mapping*).
- **An `@At` inherits its default `remap` from the enclosing injector.** With `@Redirect(remap = false)`, an `@At` that targets a *vanilla* method (e.g. `ItemStack.is(TagKey)`) is silently left unmapped, and it works in dev but fails in production (SRG names). Set `@At(..., remap = true)` explicitly for vanilla targets and `remap = false` for JDK targets (`Stream.toList()`).
- Overrides of vanilla methods inside ISS anonymous classes (`mayPlace`, `onTake`) are mapped through the superclass and need no special handling.
- `ItemOverrides` has a `protected` no-arg constructor (Forge access transformer), so `ResearchReportBakedModel` can subclass it.
- Classes inside the Mixin `package` (`com.igoa.issspellresearch.mixin`) cannot be referenced from regular code. `PersonalMode` therefore lives in `personal/`, not `mixin/`.
- Mixin classes in `mixin.personal` are listed in the **common** list (not `server`), so they also apply to the integrated server (single player / LAN). The `server` list only applies to dedicated servers.

### What can break when other mods update

| Dependency | Fragile points |
|---|---|
| ISS | anonymous class numbering (`$3`, `$4`); private methods `setupResultSlot`, `generateSpellList`, `menuSlotsChanged`; private fields (`inkSlot`, `blankScrollSlot`, `focusSlot`, `resultSlot`, `spellRecipeSelection`, `oldMenuSlots`); the order of `Slot.remove` calls in `onTake`; the `INVOKE` targets inside `generateSpellList`; spell/school registries and `AbstractSpell` accessors |
| FTB Quests | all `personal.*` targets above; `Quest.getAltTitle` / `getAltIcon` (cosmetic, non-fatal); `detect` body equivalence; `ObjectCompletedEvent` (an `EventActor`); `TaskTypes`/`RewardTypes` registration; `ClientQuestFile.openBookToQuestObject`; `TeamData` methods used by `TeamAutoSync` |
| FTB Teams | `FTBTeamsAPI`, `Team.getMembers`, team events used in the mixins |

---

## 6. Saved Data and Networking

### Saved data

Stored in the overworld's `data/` folder. **Player NBT is never used.**

| File | Class | Content |
|---|---|---|
| `issspell_research_points.dat` | `ResearchPointsData` | `players: [{ id: <UUID>, points: <long> }]` (only balances > 0) |
| `issspell_research_unlocks.dat` | `ResearchUnlockData` | `players: [{ id: <UUID>, reissue: [<spell id string>] }]` |

- Keys are player UUIDs. Reward claims are per player in FTB Quests, so reissue unlocks are per player **even when per-player progress is off**.

### Networking

The mod defines **no packets of its own.**

- **Menu open**: extra data = `BlockPos`, then `varint count`, then each entry: `ResourceLocation spell`, `varint n`, `n × (Ingredient, varint count)`. The client rebuilds the same ordered list; button ids refer to its indices.
- **Point balance**: four `DataSlot`s carrying 16 bits each (slot values are sent as shorts and masked with `0xFFFF` on receipt).
- **Menu buttons**: vanilla `handleInventoryButtonClick` → `clickMenuButton` on the server.
- **FTB Quests state** (progress, completion, reward claims) relies on FTB Quests' own synchronisation. `TeamAutoSync` sends FTB Quests' `SyncTeamDataMessage` to update clients after it changes progress.
- The reissue list is a snapshot from the time the menu opened; the server re-validates everything when a button is pressed.

---

## 7. Personal Progress and Auto Sync

An **opt-in** module derived from [ftb-solo-quests](https://github.com/YamaArashiHZ/ftb-solo-quests) (MIT). When on, it changes **all** FTB Quests progress from per-team to per-player.

### Enabling (`PersonalMode`)

Mixins are applied before Forge loads its config, so `ResearchMixinPlugin.onLoad` decides, in this order:

1. external `ftb_solo_quests` present (checked with `LoadingModList`) → **off**;
2. JVM flag `-Dissspell_research.personalProgress=true` → on;
3. `[personalProgress] enabled = true` in `config/issspell_research-common.toml` → on (the plugin reads the TOML file directly with a small line parser: section `personalProgress`, key `enabled`; if the file is missing it is off);
4. otherwise → **off** (default).

The result is stored in `PersonalMode` (a plain class with no game references) and is also what `TeamAutoSync` checks at runtime. **Changing the setting needs a restart.** `shouldApplyMixin` returns `PersonalMode.isEnabled()` for classes under `mixin.personal`.

### Per-player keying

`PersonalBaseQuestFileMixin` makes `getOrCreateTeamData(Entity)` return the data keyed by the player UUID, but **only** for `ServerQuestFile` (the client file keeps FTB Quests' behaviour). `PersonalServerQuestFileMixin` adjusts login, party change (no merging of progress), party creation (no empty party data), and ownership checks; `PersonalInventoryListenerMixin` keeps item-based task detection consistent.

### Auto sync (`TeamAutoSync`)

- Only **completed quests** are synced; partial progress (e.g. kills 5/10) is not.
- **Research chapters are never synced.** All other chapters are.
- A member receives a quest only if its dependencies are complete for that member; the loop repeats until nothing changes, so chained quests sync in one pass. Receiving a quest sets every task's progress to its maximum (an inconsistent "full but incomplete" task is reset to 0 first so completion events fire).
- Triggers: login (before FTB Quests' own login handling, so no extra message is needed), party change, **the tick after** a non-research quest completes (completions are queued in a concurrent set and processed at `ServerTickEvent` END, never while the completion event is still running), and `/issresearch sync`.
- `autoSync.enabled` and `autoSync.dedicatedServerOnly` can disable it; `disabledReason` explains why it is off. Parties of one player are skipped immediately, so the cost when unused is negligible.
- Registered in `enqueueWork` (see 4.4).

### Conflict with FTB Solo Quests

If `ftb_solo_quests` is installed, the embedded mixins and auto sync stay off. Be aware that SoloQuests' own sync buttons also sync research quests (the research chapter is not protected in that case), and that its network registration runs inside `FMLCommonSetupEvent`, which was the cause of the login crash described in 4.4.

### Switching on an existing world

There is **no migration**. Enabling the module on a world with team-keyed progress leaves party progress behind (new per-player data starts empty). Do not toggle it on a live world.

---

## 8. Compatibility and Known Limitations

### Version-dependent areas

- ISS 3.16.3 (Scroll Forge internals) and FTB Quests 2001.4.22 (the `detect` copy and the `personal` targets). Re-check the tables in [section 5](#5-mixins-and-external-mod-integration) after any update.
- Anonymous classes and private members of ISS are used directly.
- The reissue list is a snapshot from menu-open time.
- Only 3 research-chapter buttons are shown in the table.
- JEI still shows the original Scroll Forge recipes.
- GUI graphics are placeholders (flat fills).
- The table cannot open a chapter if the pack disables the FTB Quests GUI.

### Not yet verified in game

- Per-player progress and party sync (needs a multiplayer test).

### Dev vs. production

Mixins resolve names differently (Mojang names in dev, SRG in production). The `@At` `remap` pitfall in section 5 was found only in a real pack, so test a built jar in a real modpack after any Mixin change.

## 9. Testing

There is no automated test suite yet. Verify changes with the debug commands in section 2, a dedicated server, and two clients for per-player progress and party sync.

## 10. License and Third-Party Code

| What | License |
|---|---|
| Source code | **Mozilla Public License 2.0** ([`LICENSE`](LICENSE)) |
| `src/main/resources/data/**` (recipes, loot tables, tags, default reissue cost) | Mozilla Public License 2.0 |
| `src/main/resources/assets/**/textures/**` (texture images) | **Standalone redistribution prohibited** ([`LICENSE-ASSETS.md`](LICENSE-ASSETS.md)) |
| `src/main/resources/assets/**` except textures (models, blockstates, language files) | Mozilla Public License 2.0 |

### Derived code (MIT)

The following files are derived from **ftb-solo-quests** (© 2026 YamaArashi, MIT License). The original portions remain under the MIT License; keep the copyright notice and the license text (`src/main/resources/licenses/ftb-solo-quests-LICENSE.txt`) with any distribution.

- `mixin/personal/PersonalBaseQuestFileMixin.java`
- `mixin/personal/PersonalServerQuestFileMixin.java`
- `mixin/personal/PersonalInventoryListenerMixin.java`
- `personal/TeamAutoSync.java` (sync logic)

Mixing MIT-derived code into an MPL-2.0 project is permitted; those portions keep their MIT terms.

### Dependencies

- Iron's Spells 'n Spellbooks, FTB Quests, FTB Library, FTB Teams, and the other required mods are the work of their authors. **They are not bundled or redistributed.** Do not commit the jars in `libs/`.
- Before using or republishing anything derived from their internals (for example the `detect` copy), check each mod's current license and the distribution terms of its download page.
- Set `mod_license` in `gradle.properties` to match the above (the MDK default is `MIT`).
- Optionally add the MPL-2.0 per-file notice (Exhibit A) to each source file; files derived from ftb-solo-quests keep their MIT attribution.
