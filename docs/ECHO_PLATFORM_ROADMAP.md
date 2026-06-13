# ECHO Platform Roadmap

This roadmap is implemented as contract-first modules. The first pass makes packs easier to ship, safer to update, and richer to inspect before deep gameplay behavior lands.

## Review Baseline

- Module catalog baseline: 131 descriptors after roadmap generation.
- Source of truth: `scripts/generate-platform-roadmap-modules.mjs` owns roadmap module scaffolds, bundle metadata, and roadmap docs.
- Validator: `scripts/validate-platform-roadmap.mjs` enforces descriptor invariants, native entrypoints, bundle files, docs, and index membership.
- Release posture: Phase 1 source-packaging smoke proves artifact shape only; compiled runtime artifacts are still required before player-ready publication.

## Contract Boundary

Roadmap modules expose descriptors, small data contracts, artifact docs, and native surface probes. They do not mutate runtime state, register gameplay content, execute server operations, or claim completed player-facing loops. Later gameplay work must add implementation-specific tests and keep policy/reporting gates intact.

## Phase 1: Shipping Confidence

Make every future pack safer to ship by proving launchability, migration safety, policy enforcement, and graph diagnostics.

| Module | Responsibility | Public Contracts |
| --- | --- | --- |
| `echoplaytestcore` | Automated gameplay evidence runner for release readiness, session proof, save/load, install, update, repair, and rollback checks. | `playtest.scenarios`, `playtest.evidence_runner`, `playtest.release_readiness`, `playtest.session_proofs` |
| `echomigrationcore` | Versioned save, data-key, renamed-ID, removed-module, deprecated-content, and rollback compatibility migration contracts. | `migration.manifest`, `migration.dry_run`, `migration.rollback_report`, `migration.id_aliases` |
| `echocapabilitycore` | Runtime capability negotiation over descriptor provides and consumes so optional integrations are clean, inspectable, and fallback-safe. | `capability.registry`, `capability.negotiation`, `capability.missing_diagnostics`, `capability.fallbacks` |
| `echopolicycore` | Trust, permissions, write-action approval, server-rule, blocked-module, content-flag, and creator-governance policy contracts. | `policy.manifest`, `policy.validation`, `policy.runtime_hooks`, `policy.trust_metadata` |
| `echodependencydoctor` | Human-readable explanations for broken module graphs, conflicts, version gaps, missing artifacts, and bad optional integrations. | `dependency.explanations`, `dependency.launch_report`, `dependency.conflict_diagnostics`, `dependency.artifact_diagnostics` |

## Phase 2: Creator Multipliers

Make creator work faster and less fragile through reusable authoring, audit, diff, asset, and localization contracts.

| Module | Responsibility | Public Contracts |
| --- | --- | --- |
| `echoblueprintcore` | Reusable authoring blueprints for common content types and Studio template generation. | `blueprint.schemas`, `blueprint.templates`, `blueprint.studio_generation` |
| `echobalancecore` | Balance tables and audits for recipes, progression pacing, combat stats, economy, loot, energy, and survival pressure. | `balance.tables`, `balance.audits`, `balance.recommended_ranges` |
| `echopackdiff` | Explains differences between pack or module versions across gameplay, dependencies, migrations, and renamed content. | `packdiff.json`, `packdiff.markdown`, `packdiff.changelog` |
| `echoassetpipeline` | Asset import, naming validation, thumbnails, texture and sound manifests, and missing asset report contracts. | `assetpipeline.audit`, `assetpipeline.thumbnails`, `assetpipeline.manifests` |
| `echolocalizationcore` | Translation validation, fallback text, missing-key reports, language-pack exports, and Studio localization workflow contracts. | `localization.validation`, `localization.fallbacks`, `localization.language_pack_overlay` |

## Phase 3: Player State And UX

Make player-facing packs feel coherent by standardizing session state, accessibility, curation, and privacy-safe telemetry contracts.

| Module | Responsibility | Public Contracts |
| --- | --- | --- |
| `echosessioncore` | Shared player session memory for onboarding, objectives, route history, hazards, deaths, and pack phase. | `session.snapshot`, `session.objective_state`, `session.route_history`, `session.pack_phase` |
| `echoaccessibilitycore` | Readable HUD scale, reduced motion, contrast themes, captions, prompt remaps, and narration metadata contracts. | `accessibility.settings`, `accessibility.validation`, `accessibility.narration_metadata` |
| `echocurationcore` | Launcher and Studio recommendations for module fit, bundle previews, dependency explanations, and readiness badges. | `curation.recommendations`, `curation.bundle_previews`, `curation.readiness_badges` |
| `echotelemetrycore` | Privacy-safe local and session metrics for crashes, install health, progression checkpoints, and module load failures. | `telemetry.local_bundle`, `telemetry.privacy_policy`, `telemetry.qa_metrics` |

## Phase 4: Big Gameplay Systems

Prepare richer gameplay systems as shared contracts while reusing existing social, world, status, combat, and progression modules.

| Module | Responsibility | Public Contracts |
| --- | --- | --- |
| `echofactioncore` | Factions, reputation, standings, vendors, hostility, alliances, territory hooks, and mission consequence contracts. | `faction.registry`, `faction.reputation`, `faction.standings`, `faction.mission_consequences` |
| `echosettlementcore` | Bases, shelters, NPC jobs, storage needs, defense score, comfort, and logistics request contracts. | `settlement.registry`, `settlement.jobs`, `settlement.defense_score`, `settlement.logistics_requests` |
| `echohazardcore` | Generic hazards for heat, cold, radiation, oxygen, pressure, corruption, disease, and storm exposure. | `hazard.registry`, `hazard.exposure`, `hazard.resistance`, `hazard.world_hooks` |
| `echoequipmentcore` | Gear slots, durability rules, upgrades, modifiers, and loadout validation contracts. | `equipment.slots`, `equipment.durability`, `equipment.upgrades`, `equipment.loadout_validation` |
| `echoskillcore` | Skills, mastery tracks, passive unlocks, and progression gate contracts. | `skill.tracks`, `skill.mastery`, `skill.passive_unlocks`, `skill.progression_gates` |
| `echoterritorycore` | Region control, claims, contested zones, faction ownership, map overlays, and server rule contracts. | `territory.claims`, `territory.control`, `territory.map_overlays`, `territory.server_rules` |

## Phase 5: Event And World Depth

Prepare long-tail event and world-depth systems after player-state and gameplay contracts are in place.

| Module | Responsibility | Public Contracts |
| --- | --- | --- |
| `echoexpeditioncore` | Route preparation, risk budgets, extraction loops, and travel contract surfaces. | `expedition.routes`, `expedition.risk_budget`, `expedition.extraction`, `expedition.travel_contracts` |
| `echoruincore` | Ruins, archaeology, salvage sites, and restoration state contracts. | `ruin.registry`, `ruin.archaeology`, `ruin.salvage_sites`, `ruin.restoration_state` |
| `echosupplycore` | Scarcity, shortages, stockpiles, rationing, and supply pressure contracts. | `supply.scarcity`, `supply.stockpiles`, `supply.rationing`, `supply.pressure` |
| `echodisastercore` | Blackouts, earthquakes, station failures, storm disasters, and recovery event contracts. | `disaster.events`, `disaster.recovery`, `disaster.station_failures`, `disaster.world_impacts` |
| `echoseasoncore` | Rotating objectives, seasonal loot, timed modifiers, and live event contracts. | `season.objectives`, `season.loot`, `season.timed_modifiers`, `season.live_events` |
| `echoserveropscore` | Moderation, backups, announcements, support bundles, and player report contracts. | `serverops.moderation`, `serverops.backups`, `serverops.announcements`, `serverops.player_reports` |

## Build Order

1. `echoplaytestcore`
2. `echomigrationcore`
3. `echocapabilitycore`
4. `echopolicycore`
5. `echodependencydoctor`
6. `echoblueprintcore`
7. `echosessioncore`
8. `echohazardcore`
9. `echofactioncore`
10. `echosettlementcore`

## Review Commands

- `node scripts/validate-module-graph.mjs --write-index`
- `node scripts/validate-module-graph.mjs`
- `node scripts/validate-foundations-split.mjs`
- `node scripts/docs-audit.mjs`
- `node scripts/validate-arcana-division-beta.mjs`
- `node scripts/validate-platform-roadmap.mjs`
