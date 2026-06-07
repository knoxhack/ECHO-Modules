<!-- CURSEFORGE_README_START -->
# TutorialCore by ECHO Labs

![TutorialCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echotutorialcore/brand-sheet.png)

**Make Ashfall deep, but not confusing.**

![TutorialCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echotutorialcore/features-portrait.png)

![TutorialCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echotutorialcore/features-landscape.png)

## CurseForge Summary

Make Ashfall deep, but not confusing.

## Main Features

- Tutorial cards.
- Guided onboarding paths.
- First-steps presentation.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echotutorialcore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echotutorialcore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echotutorialcore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: TutorialCore

**Make Ashfall deep, but not confusing.**

ECHO: TutorialCore is a first-party ECHO addon that provides shared onboarding, guided tutorials, contextual hints, first-time popups, mistake detection, Terminal tutorial cards, beginner guide mode, and codex-style help for the entire ECHO ecosystem.

TutorialCore does not make Ashfall simpler. It makes Ashfall **readable**.

---

## What Problem It Solves

Ashfall is a complex ruined-Earth expedition survival RPG. New players face deep systems, hidden dependencies, and lethal hazards without clear feedback. TutorialCore exists to:

- Surface critical information exactly when it is needed.
- Explain mistakes without punishing the player.
- Provide a searchable, persistent guide system through the Terminal.
- Respect expert players by being ignorable and non-spammy.

---

## Guide Modes

Per-player guide modes control how much help the system gives:

- **OFF**: No automatic popups or hints. Manual Guide cards still exist.
- **MINIMAL**: Only major first-time tips and critical danger warnings.
- **NORMAL**: Default balanced guidance.
- **ASSISTED**: More active guidance, missing item hints, stuck detection, and next-step suggestions.

Default: **NORMAL**

Guide modes are persisted per player and can be changed via command or (if present) Terminal UI.

---

## Tutorial Cards

Data-driven guide cards organized by category:

- Start Here, Survival, Terminal, Scanner, HoloMap, Lens, Power, Machines, Water, Hazards, Factions, Research, Drones, Combat, Nexus, Route Chapters, Troubleshooting, Advanced, Addons.

Cards support:
- Title, summary, body paragraphs, steps, common mistakes
- Related cards, items, blocks, missions
- Unlock triggers, visibility states
- Addon ownership for extensibility

Cards are readable in the Terminal Guide page when Terminal is present. Without Terminal, they are accessible via commands or chat fallback.

---

## Contextual Hints

Hints trigger based on player state and are evaluated periodically (not every tick). They respect:
- Guide mode restrictions
- Cooldowns and dismissals
- Caps per minute / per session
- Danger overrides (critical warnings bypass normal cooldowns)

Hint types: INFO, WARNING, DANGER, BLOCKED, MISSING_ITEM, PROGRESSION, SYSTEM_HELP, MISSION_HELP, RECIPE_HELP, HAZARD_HELP, COMBAT_HELP, MACHINE_HELP, POWER_HELP, ROUTE_HELP.

---

## Mistake Detection

TutorialMistakeDetector tracks common errors and produces useful hints after repeated occurrences:

- No power connected to machine
- Missing filter in scrubber / gas mask
- Dirty water overuse
- Hazard unprepared entry
- Recipe locked attempt
- Unclaimed reward
- No active mission
- Scanner / HoloMap ignored
- Repeated failure pattern

All detection is non-spammy, server-safe, and configurable.

---

## Requirement Hints

When a player lacks items, research, missions, or faction standing for an action, TutorialCore can explain what is missing. The resolver supports:
- Item, tag, block, research, mission, and faction requirements
- Safe optional lookups (no hard crash if other modules are absent)
- Guide card links for deeper reading

---

## Recipe Lock Explanations

TutorialCore provides API hooks and a structured lock explanation model for "why can't I craft this?" systems. Requirement hints can report missing items, tags, research progress, mission progress, and faction contact, then link back to the relevant Guide cards.

---

## First-Hour Onboarding Flow

Optional first-hour flow tracks:
- Terminal opened
- Water found
- Resources gathered
- First power loop
- Clean water produced
- Signal lead followed
- Hazard preparation

Flows unlock cards and provide a gentle, non-intrusive introduction.

---

## Terminal Integration

When ECHO Terminal is present, TutorialCore:
- Registers a Guide page/hub
- Provides card lists by category
- Supplies "What Now" recommendations
- Shows unread/new card markers
- Offers guide mode controls

If Terminal is absent, all functionality falls back to commands and chat.

---

## Lens Integration

When ECHO Lens is present, TutorialCore can supply assist rows:
- Machine offline: suggest power connection
- Missing filter: suggest cartridge insertion
- Unknown block: suggest deep scan
- Route, power, filter, and Guide context from real TutorialCore progress

---

## HoloMap Integration

When ECHO HoloMap is present, TutorialCore can supply:
- Route prep warnings (radiation, toxic air, difficulty)
- Signal lead context (survivor cache, guardian site)
- Preparation checklists before dangerous routes
- Active reminder markers from real tutorial progress and hazard context

---

## PowerGrid Integration

When ECHO PowerGrid is present, TutorialCore:
- Receives power events (no power, breaker trip, brownout, overload)
- Shows PowerGrid-specific tutorial hints
- Unlocks Power Basics cards

If direct integration is not available, PowerGrid can call:
- `TutorialCoreApi.reportNoPower(player, blockPos)`
- `TutorialCoreApi.reportBreakerTripped(player, blockPos)`
- `TutorialCoreApi.reportBrownout(player, blockPos)`
- `TutorialCoreApi.reportOverload(player, blockPos)`

---

## MissionCore Integration

When ECHO MissionCore is present, TutorialCore registers tutorial side-op guidance for the first Terminal, clean water, power loop, scanner lead, route prep, and first hazard survival.

---

## Index Integration

When ECHO Index is present, TutorialCore publishes Guide cards and troubleshooting references through the shared Index content provider service.

---

## WorldCore Integration

When ECHO WorldCore is present, TutorialCore reads hazard/region context and triggers first-time hazard hints with accurate region names.

---

## SoundCore Integration

When ECHO SoundCore is present, TutorialCore:
- Plays subtle tutorial notification sounds
- Plays warning stingers for danger hints
- Plays guide card unlock sounds

If SoundCore is absent, all sound calls are no-op.

---

## Data-Driven JSON Formats

All cards, hints, and flows are loaded from JSON under:
- `data/<namespace>/tutorial_cards/*.json`
- `data/<namespace>/tutorial_hints/*.json`
- `data/<namespace>/tutorial_flows/*.json`
- `data/<namespace>/tutorial_tooltips/*.json`

Addons can add their own content by placing JSON in their own namespace.

Invalid JSON is logged and skipped. The game does not crash.

`schemaVersion: 1` is supported. Legacy string hint conditions still load, while new condition objects support `type`, `target`, `count`, `invert`, and `addon`.

---

## Public API

```java
TutorialCoreApi.registerCard(...)
TutorialCoreApi.registerHint(...)
TutorialCoreApi.registerFlow(...)
TutorialCoreApi.registerTooltip(...)
TutorialCoreApi.unlockCard(ServerPlayer player, Identifier cardId)
TutorialCoreApi.showCard(ServerPlayer player, Identifier cardId)
TutorialCoreApi.dismissCard(ServerPlayer player, Identifier cardId)
TutorialCoreApi.showHint(ServerPlayer player, Identifier hintId)
TutorialCoreApi.showHint(ServerPlayer player, TutorialHint hint)
TutorialCoreApi.dismissHint(ServerPlayer player, Identifier hintId)
TutorialCoreApi.markProgress(ServerPlayer player, Identifier progressId)
TutorialCoreApi.hasProgress(ServerPlayer player, Identifier progressId)
TutorialCoreApi.recordTrigger(ServerPlayer player, TutorialTriggerType type, Identifier target, Map<String, String> context)
TutorialCoreApi.completeFlow(ServerPlayer player, Identifier flowId)
TutorialCoreApi.setGuideMode(ServerPlayer player, TutorialGuideMode mode)
TutorialCoreApi.getGuideMode(ServerPlayer player)
TutorialCoreApi.reportMistake(ServerPlayer player, Identifier mistakeId)
TutorialCoreApi.reportMissingRequirement(ServerPlayer player, Identifier requirementId)
TutorialCoreApi.getVisibleCards(Player player, TutorialCategory category)
TutorialCoreApi.getRecommendedCards(Player player, int limit)
TutorialCoreApi.reportPowerEvent(Player player, BlockPos pos, TutorialPowerEventType type)
TutorialCoreApi.reportHazardContext(Player player, Identifier hazardId, Identifier regionId)
TutorialCoreApi.getRecommendedNextSteps(Player player)
```

Convenience calls:
```java
TutorialCoreApi.reportNoPower(player, pos)
TutorialCoreApi.reportMissingFilter(player)
TutorialCoreApi.reportRecipeLocked(player)
TutorialCoreApi.reportHazardUnprepared(player)
TutorialCoreApi.reportRewardAvailable(player)
TutorialCoreApi.reportSignalDetected(player)
TutorialCoreApi.reportGuardianLocated(player)
TutorialCoreApi.reportFactionContact(player)
```

All methods are null-safe, side-safe, and optional-integration-safe.

---

## Commands

```
/echotutorialcore guide_mode <off|minimal|normal|assisted>
/echotutorialcore progress [player]
/echotutorialcore reset <player>
/echotutorialcore unlock_card <player> <cardId>
/echotutorialcore show_card <player> <cardId>
/echotutorialcore hint <player> <hintId>
/echotutorialcore list_cards
/echotutorialcore list_hints
/echotutorialcore debug
/echotutorialcore simulate_stuck <player>
/echotutorialcore reload
```

Normal players can set their own guide mode and view progress. Admin permission is required for reset, unlock, show to others, simulate_stuck, and debug.

---

## Config Options

**Server (`echotutorialcore-common.toml`)**
- `allowAssistedGuideMode`
- `forceGuideMode`
- `enableMistakeDetection`
- `enableStuckDetection`
- `enableRecipeLockExplanations`
- `enableHazardWarnings`
- `enableFirstHourFlow`
- `enableTooltipHelp`
- `maxHintsPerMinute`
- `maxPopupsPerSession`
- `stuckDetectionMinutes`
- `repeatedDeathThreshold`
- `noCleanWaterWarningDay`

**Client (`echotutorialcore-client.toml`)**
- `showTutorialPopups`
- `showContextualHints`
- `showDangerWarnings`
- `showTooltipHelp`
- `showTerminalGuideCards`
- `showToastHints`
- `playTutorialSounds`
- `guideModeDefault`
- `hintScale`
- `hintDurationTicks`

---

## Known Limitations

- TutorialCore remains service-only in 1.0.0: no blocks, items, mobs, recipes, loot, or worldgen are added.
- Optional addon surfaces are guarded and no-op when their addon is absent.
- Stack-wide validation still depends on unrelated upstream modules compiling cleanly.

---

## Future Roadmap

- Search/filter refinements for the Terminal Guide page
- More addon-authored tutorial card packs
- Deeper stuck detection heuristics with automated What Now updates
- Addon registration API for third-party tutorial content

---

## License

All Rights Reserved.

Built as part of the ECHO / Ashfall ecosystem by KnoxHack.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echotutorialcore.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echotutorialcore.md`.
