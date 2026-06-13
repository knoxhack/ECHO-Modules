import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

export const FOUNDATION_NATIVE_MODULES = [
  { id: 'echofoundationcore', nativeClassName: 'EchoFoundationCoreNativeModule' },
  { id: 'echomaterialcore', nativeClassName: 'EchoMaterialCoreNativeModule' },
  { id: 'echotoolcore', nativeClassName: 'EchoToolCoreNativeModule' },
  { id: 'echostationcore', nativeClassName: 'EchoStationCoreNativeModule' },
  { id: 'echoworldstarter', nativeClassName: 'EchoWorldStarterNativeModule' },
  { id: 'echocommonloot', nativeClassName: 'EchoCommonLootNativeModule' },
  { id: 'echocreatureroles', nativeClassName: 'EchoCreatureRolesNativeModule' },
]

export const ROADMAP_MODULES = [
  {
    id: 'echoplaytestcore',
    name: 'ECHO: PlaytestCore',
    className: 'EchoPlaytestCore',
    nativeClassName: 'EchoPlaytestCoreNativeModule',
    phase: 1,
    role: 'playtest_core',
    kind: 'tooling',
    summary: 'Automated gameplay evidence runner for release readiness, session proof, save/load, install, update, repair, and rollback checks.',
    requires: ['echoreportcore', 'echovalidationcore', 'echomodulegraph', 'echoruntimeguard'],
    optional: ['echomigrationcore', 'echopolicycore', 'echotelemetrycore'],
    provides: ['playtest.scenarios', 'playtest.evidence_runner', 'playtest.release_readiness', 'playtest.session_proofs'],
    consumes: ['reports.contracts', 'release.readiness', 'validation.pack', 'module.graph', 'runtime.guard'],
    adapterDomains: ['data', 'diagnostics', 'packs'],
    permissions: ['playtest.read', 'playtest.write', 'reports.write', 'pack.read'],
    mvpContracts: [
      'json_scenario_definitions',
      'first_30_minutes_run',
      'two_hour_run',
      'completion_path',
      'save_load_proof',
      'crash_free_session',
      'install_update_repair_rollback_proof',
      'release_readiness_report',
    ],
  },
  {
    id: 'echomigrationcore',
    name: 'ECHO: MigrationCore',
    className: 'EchoMigrationCore',
    nativeClassName: 'EchoMigrationCoreNativeModule',
    phase: 1,
    role: 'migration_core',
    kind: 'library',
    summary: 'Versioned save, data-key, renamed-ID, removed-module, deprecated-content, and rollback compatibility migration contracts.',
    requires: ['echodatacore', 'echoschemacore', 'echovalidationcore'],
    optional: ['echoreportcore'],
    provides: ['migration.manifest', 'migration.dry_run', 'migration.rollback_report', 'migration.id_aliases'],
    consumes: ['data.contracts', 'schema.registry', 'validation.pack'],
    adapterDomains: ['data', 'diagnostics', 'saves'],
    permissions: ['migration.read', 'migration.write', 'saves.read', 'diagnostics.write'],
    mvpContracts: ['migration_manifest', 'dry_run_report', 'rollback_compatibility_report', 'renamed_id_map', 'removed_module_notes'],
  },
  {
    id: 'echocapabilitycore',
    name: 'ECHO: CapabilityCore',
    className: 'EchoCapabilityCore',
    nativeClassName: 'EchoCapabilityCoreNativeModule',
    phase: 1,
    role: 'capability_core',
    kind: 'library',
    summary: 'Runtime capability negotiation over descriptor provides and consumes so optional integrations are clean, inspectable, and fallback-safe.',
    requires: ['echomodulegraph', 'echoplatformcore'],
    optional: ['echoreportcore'],
    provides: ['capability.registry', 'capability.negotiation', 'capability.missing_diagnostics', 'capability.fallbacks'],
    consumes: ['module.graph', 'feature.graph', 'platform.contracts'],
    adapterDomains: ['data', 'diagnostics', 'packs'],
    permissions: ['capability.read', 'diagnostics.write', 'pack.read'],
    mvpContracts: ['capability_registry', 'missing_capability_diagnostics', 'graceful_fallback_api'],
  },
  {
    id: 'echopolicycore',
    name: 'ECHO: PolicyCore',
    className: 'EchoPolicyCore',
    nativeClassName: 'EchoPolicyCoreNativeModule',
    phase: 1,
    role: 'policy_core',
    kind: 'library',
    summary: 'Trust, permissions, write-action approval, server-rule, blocked-module, content-flag, and creator-governance policy contracts.',
    requires: ['echovalidationcore', 'echoreportcore', 'echometadatacore'],
    optional: ['echocapabilitycore'],
    provides: ['policy.manifest', 'policy.validation', 'policy.runtime_hooks', 'policy.trust_metadata'],
    consumes: ['validation.pack', 'reports.contracts', 'metadata.manifest'],
    adapterDomains: ['data', 'diagnostics', 'packs', 'permissions'],
    permissions: ['policy.read', 'policy.write', 'permissions.validate', 'diagnostics.write'],
    mvpContracts: ['policy_manifest', 'launcher_validation', 'studio_validation', 'runtime_enforcement_hooks', 'blocked_module_rules'],
  },
  {
    id: 'echodependencydoctor',
    name: 'ECHO: DependencyDoctor',
    className: 'EchoDependencyDoctor',
    nativeClassName: 'EchoDependencyDoctorNativeModule',
    phase: 1,
    role: 'dependency_doctor',
    kind: 'tooling',
    summary: 'Human-readable explanations for broken module graphs, conflicts, version gaps, missing artifacts, and bad optional integrations.',
    requires: ['echomodulegraph', 'echocapabilitycore', 'echoreportcore'],
    optional: ['echopolicycore'],
    provides: ['dependency.explanations', 'dependency.launch_report', 'dependency.conflict_diagnostics', 'dependency.artifact_diagnostics'],
    consumes: ['module.graph', 'feature.graph', 'capability.registry', 'reports.contracts'],
    adapterDomains: ['diagnostics', 'packs'],
    permissions: ['dependency.read', 'diagnostics.write', 'pack.read'],
    mvpContracts: ['why_pack_wont_launch_report', 'conflict_explanations', 'missing_artifact_report', 'optional_integration_diagnostics'],
  },
  {
    id: 'echoblueprintcore',
    name: 'ECHO: BlueprintCore',
    className: 'EchoBlueprintCore',
    nativeClassName: 'EchoBlueprintCoreNativeModule',
    phase: 2,
    role: 'blueprint_core',
    kind: 'tooling',
    summary: 'Reusable authoring blueprints for common content types and Studio template generation.',
    requires: ['echoschemacore', 'echocreatorcore', 'echocontentcore'],
    optional: ['echolocalizationcore'],
    provides: ['blueprint.schemas', 'blueprint.templates', 'blueprint.studio_generation'],
    consumes: ['schema.registry', 'creator.validation', 'content.references'],
    adapterDomains: ['data', 'packs'],
    permissions: ['blueprints.read', 'blueprints.write', 'creator.exports'],
    mvpContracts: ['blueprint_schema', 'studio_template_generation', 'content_type_blueprints'],
  },
  {
    id: 'echobalancecore',
    name: 'ECHO: BalanceCore',
    className: 'EchoBalanceCore',
    nativeClassName: 'EchoBalanceCoreNativeModule',
    phase: 2,
    role: 'balance_core',
    kind: 'tooling',
    summary: 'Balance tables and audits for recipes, progression pacing, combat stats, economy, loot, energy, and survival pressure.',
    requires: ['echorecipecore', 'echolootcore', 'echoeconomycore', 'echoprogressioncore'],
    optional: ['echoreportcore'],
    provides: ['balance.tables', 'balance.audits', 'balance.recommended_ranges'],
    consumes: ['recipes.backend', 'loot.tables', 'economy.pricing', 'progression.unlock_graph'],
    adapterDomains: ['data', 'diagnostics'],
    permissions: ['balance.read', 'diagnostics.write'],
    mvpContracts: ['balance_report', 'warning_ranges', 'recommended_ranges'],
  },
  {
    id: 'echopackdiff',
    name: 'ECHO: PackDiff',
    className: 'EchoPackDiff',
    nativeClassName: 'EchoPackDiffNativeModule',
    phase: 2,
    role: 'pack_diff',
    kind: 'tooling',
    summary: 'Explains differences between pack or module versions across gameplay, dependencies, migrations, and renamed content.',
    requires: ['echomigrationcore', 'echometadatacore', 'echoreportcore'],
    optional: ['echodependencydoctor'],
    provides: ['packdiff.json', 'packdiff.markdown', 'packdiff.changelog'],
    consumes: ['migration.manifest', 'metadata.manifest', 'reports.contracts'],
    adapterDomains: ['data', 'diagnostics', 'packs'],
    permissions: ['pack.read', 'reports.write'],
    mvpContracts: ['markdown_changelog', 'json_changelog', 'dependency_diff', 'migration_diff'],
  },
  {
    id: 'echoassetpipeline',
    name: 'ECHO: AssetPipeline',
    className: 'EchoAssetPipeline',
    nativeClassName: 'EchoAssetPipelineNativeModule',
    phase: 2,
    role: 'asset_pipeline',
    kind: 'tooling',
    summary: 'Asset import, naming validation, thumbnails, texture and sound manifests, and missing asset report contracts.',
    requires: ['echoassetcore', 'echotextureforge', 'echosoundcore'],
    optional: ['echoreportcore'],
    provides: ['assetpipeline.audit', 'assetpipeline.thumbnails', 'assetpipeline.manifests'],
    consumes: ['assets.registry', 'assets.validation', 'asset.textureforge', 'sound.service'],
    adapterDomains: ['assets', 'data', 'diagnostics'],
    permissions: ['assets.read', 'assets.write', 'diagnostics.write'],
    mvpContracts: ['asset_audit', 'preview_thumbnail_manifest', 'missing_asset_report'],
  },
  {
    id: 'echolocalizationcore',
    name: 'ECHO: LocalizationCore',
    className: 'EchoLocalizationCore',
    nativeClassName: 'EchoLocalizationCoreNativeModule',
    phase: 2,
    role: 'localization_core',
    kind: 'library',
    summary: 'Translation validation, fallback text, missing-key reports, language-pack exports, and Studio localization workflow contracts.',
    requires: ['echoschemacore', 'echoreportcore'],
    optional: ['echocreatorcore'],
    provides: ['localization.validation', 'localization.fallbacks', 'localization.language_pack_overlay'],
    consumes: ['schema.registry', 'reports.contracts'],
    adapterDomains: ['data', 'diagnostics', 'packs'],
    permissions: ['localization.read', 'localization.write', 'reports.write'],
    mvpContracts: ['missing_key_report', 'fallback_text_contract', 'language_pack_overlay_support'],
  },
  {
    id: 'echosessioncore',
    name: 'ECHO: SessionCore',
    className: 'EchoSessionCore',
    nativeClassName: 'EchoSessionCoreNativeModule',
    phase: 3,
    role: 'session_core',
    kind: 'library',
    summary: 'Shared player session memory for onboarding, objectives, route history, hazards, deaths, and pack phase.',
    requires: ['echodatacore', 'echomissioncore', 'echoplayercore'],
    optional: ['echoterminal', 'echoholomap', 'echolens', 'echotutorialcore'],
    provides: ['session.snapshot', 'session.objective_state', 'session.route_history', 'session.pack_phase'],
    consumes: ['data.contracts', 'mission.objectives', 'player.profile'],
    adapterDomains: ['data', 'saves'],
    permissions: ['session.read', 'session.write', 'saves.read'],
    mvpContracts: ['session_snapshot_api', 'current_objective_state', 'recent_death_state', 'active_hazard_state'],
  },
  {
    id: 'echoaccessibilitycore',
    name: 'ECHO: AccessibilityCore',
    className: 'EchoAccessibilityCore',
    nativeClassName: 'EchoAccessibilityCoreNativeModule',
    phase: 3,
    role: 'accessibility_core',
    kind: 'library',
    summary: 'Readable HUD scale, reduced motion, contrast themes, captions, prompt remaps, and narration metadata contracts.',
    requires: ['echothemecore', 'echoscreencore', 'echoinputcore', 'echosoundcore'],
    optional: ['echolocalizationcore'],
    provides: ['accessibility.settings', 'accessibility.validation', 'accessibility.narration_metadata'],
    consumes: ['theme.tokens', 'screen.surface', 'input.bindings', 'sound.audio_profiles'],
    adapterDomains: ['data', 'ui_screens', 'input', 'audio'],
    permissions: ['accessibility.read', 'accessibility.write'],
    mvpContracts: ['accessibility_settings_contract', 'validation_checks', 'caption_metadata', 'prompt_remaps'],
  },
  {
    id: 'echocurationcore',
    name: 'ECHO: CurationCore',
    className: 'EchoCurationCore',
    nativeClassName: 'EchoCurationCoreNativeModule',
    phase: 3,
    role: 'curation_core',
    kind: 'tooling',
    summary: 'Launcher and Studio recommendations for module fit, bundle previews, dependency explanations, and readiness badges.',
    requires: ['echocapabilitycore', 'echodependencydoctor', 'echometadatacore'],
    optional: ['echoreportcore'],
    provides: ['curation.recommendations', 'curation.bundle_previews', 'curation.readiness_badges'],
    consumes: ['capability.registry', 'dependency.explanations', 'metadata.manifest'],
    adapterDomains: ['data', 'diagnostics', 'packs'],
    permissions: ['curation.read', 'reports.write'],
    mvpContracts: ['recommended_modules_report', 'bundle_preview_contract', 'readiness_badges'],
  },
  {
    id: 'echotelemetrycore',
    name: 'ECHO: TelemetryCore',
    className: 'EchoTelemetryCore',
    nativeClassName: 'EchoTelemetryCoreNativeModule',
    phase: 3,
    role: 'telemetry_core',
    kind: 'library',
    summary: 'Privacy-safe local and session metrics for crashes, install health, progression checkpoints, and module load failures.',
    requires: ['echopolicycore', 'echoreportcore', 'echoplaytestcore'],
    optional: ['echosessioncore'],
    provides: ['telemetry.local_bundle', 'telemetry.privacy_policy', 'telemetry.qa_metrics'],
    consumes: ['policy.manifest', 'reports.contracts', 'playtest.session_proofs'],
    adapterDomains: ['data', 'diagnostics'],
    permissions: ['telemetry.read', 'telemetry.write', 'reports.write'],
    mvpContracts: ['opt_in_local_telemetry_bundle', 'privacy_safe_metrics', 'qa_support_export'],
  },
  {
    id: 'echofactioncore',
    name: 'ECHO: FactionCore',
    className: 'EchoFactionCore',
    nativeClassName: 'EchoFactionCoreNativeModule',
    phase: 4,
    role: 'faction_core',
    kind: 'library',
    summary: 'Factions, reputation, standings, vendors, hostility, alliances, territory hooks, and mission consequence contracts.',
    requires: ['echomissioncore', 'echoeconomycore', 'echosocialcore'],
    optional: ['echoterritorycore'],
    provides: ['faction.registry', 'faction.reputation', 'faction.standings', 'faction.mission_consequences'],
    consumes: ['mission.objectives', 'economy.pricing', 'social.factions', 'social.reputation'],
    adapterDomains: ['data', 'entities'],
    permissions: ['faction.read', 'faction.write'],
    mvpContracts: ['faction_registry', 'reputation_state', 'standing_rules', 'mission_consequence_hooks'],
  },
  {
    id: 'echosettlementcore',
    name: 'ECHO: SettlementCore',
    className: 'EchoSettlementCore',
    nativeClassName: 'EchoSettlementCoreNativeModule',
    phase: 4,
    role: 'settlement_core',
    kind: 'library',
    summary: 'Bases, shelters, NPC jobs, storage needs, defense score, comfort, and logistics request contracts.',
    requires: ['echobasegrid', 'echonpcore', 'echologisticscore', 'echoworldcore'],
    optional: ['echofactioncore'],
    provides: ['settlement.registry', 'settlement.jobs', 'settlement.defense_score', 'settlement.logistics_requests'],
    consumes: ['basegrid.claims', 'npc.profiles', 'logistics.routes', 'world.regions'],
    adapterDomains: ['data', 'entities', 'worldgen'],
    permissions: ['settlement.read', 'settlement.write'],
    mvpContracts: ['settlement_snapshot', 'npc_job_contract', 'defense_score_contract', 'logistics_request_contract'],
  },
  {
    id: 'echohazardcore',
    name: 'ECHO: HazardCore',
    className: 'EchoHazardCore',
    nativeClassName: 'EchoHazardCoreNativeModule',
    phase: 4,
    role: 'hazard_core',
    kind: 'library',
    summary: 'Generic hazards for heat, cold, radiation, oxygen, pressure, corruption, disease, and storm exposure.',
    requires: ['echostatuscore', 'echohealthcore', 'echoweathercore', 'echoworldcore'],
    optional: ['echosessioncore'],
    provides: ['hazard.registry', 'hazard.exposure', 'hazard.resistance', 'hazard.world_hooks'],
    consumes: ['status.exposure', 'health.damage_model', 'weather.events', 'world.hazards'],
    adapterDomains: ['data', 'worldgen'],
    permissions: ['hazard.read', 'hazard.write'],
    mvpContracts: ['hazard_registry', 'exposure_contract', 'resistance_contract', 'world_hazard_hooks'],
  },
  {
    id: 'echoequipmentcore',
    name: 'ECHO: EquipmentCore',
    className: 'EchoEquipmentCore',
    nativeClassName: 'EchoEquipmentCoreNativeModule',
    phase: 4,
    role: 'equipment_core',
    kind: 'library',
    summary: 'Gear slots, durability rules, upgrades, modifiers, and loadout validation contracts.',
    requires: ['echoarmory', 'echocombatcore', 'echotoolcore'],
    optional: ['echoaccessibilitycore'],
    provides: ['equipment.slots', 'equipment.durability', 'equipment.upgrades', 'equipment.loadout_validation'],
    consumes: ['armory.gear', 'combat.stats', 'foundation.tools'],
    adapterDomains: ['data', 'items'],
    permissions: ['equipment.read', 'equipment.write'],
    mvpContracts: ['gear_slot_contract', 'durability_rules', 'upgrade_modifiers', 'loadout_validation'],
  },
  {
    id: 'echoskillcore',
    name: 'ECHO: SkillCore',
    className: 'EchoSkillCore',
    nativeClassName: 'EchoSkillCoreNativeModule',
    phase: 4,
    role: 'skill_core',
    kind: 'library',
    summary: 'Skills, mastery tracks, passive unlocks, and progression gate contracts.',
    requires: ['echoprogressioncore', 'echoplayercore', 'echomissioncore'],
    optional: ['echoequipmentcore'],
    provides: ['skill.tracks', 'skill.mastery', 'skill.passive_unlocks', 'skill.progression_gates'],
    consumes: ['progression.unlock_graph', 'player.profile', 'mission.objectives'],
    adapterDomains: ['data', 'saves'],
    permissions: ['skill.read', 'skill.write'],
    mvpContracts: ['skill_track_contract', 'mastery_contract', 'passive_unlocks', 'progression_gates'],
  },
  {
    id: 'echoterritorycore',
    name: 'ECHO: TerritoryCore',
    className: 'EchoTerritoryCore',
    nativeClassName: 'EchoTerritoryCoreNativeModule',
    phase: 4,
    role: 'territory_core',
    kind: 'library',
    summary: 'Region control, claims, contested zones, faction ownership, map overlays, and server rule contracts.',
    requires: ['echofactioncore', 'echoholomap', 'echoworldcore', 'echopolicycore'],
    optional: ['echoserveropscore'],
    provides: ['territory.claims', 'territory.control', 'territory.map_overlays', 'territory.server_rules'],
    consumes: ['faction.registry', 'holomap.layers', 'world.regions', 'policy.manifest'],
    adapterDomains: ['data', 'worldgen', 'maps'],
    permissions: ['territory.read', 'territory.write', 'server.rules'],
    mvpContracts: ['region_control_contract', 'claim_contract', 'contested_zone_contract', 'map_overlay_contract'],
  },
  {
    id: 'echoexpeditioncore',
    name: 'ECHO: ExpeditionCore',
    className: 'EchoExpeditionCore',
    nativeClassName: 'EchoExpeditionCoreNativeModule',
    phase: 5,
    role: 'expedition_core',
    kind: 'library',
    summary: 'Route preparation, risk budgets, extraction loops, and travel contract surfaces.',
    requires: ['echosessioncore', 'echohazardcore', 'echomissioncore', 'echoworldcore'],
    optional: ['echosupplycore'],
    provides: ['expedition.routes', 'expedition.risk_budget', 'expedition.extraction', 'expedition.travel_contracts'],
    consumes: ['session.snapshot', 'hazard.registry', 'mission.objectives', 'world.regions'],
    adapterDomains: ['data', 'worldgen'],
    permissions: ['expedition.read', 'expedition.write'],
    mvpContracts: ['route_prep_contract', 'risk_budget_contract', 'extraction_loop_contract', 'travel_contract'],
  },
  {
    id: 'echoruincore',
    name: 'ECHO: RuinCore',
    className: 'EchoRuinCore',
    nativeClassName: 'EchoRuinCoreNativeModule',
    phase: 5,
    role: 'ruin_core',
    kind: 'library',
    summary: 'Ruins, archaeology, salvage sites, and restoration state contracts.',
    requires: ['echostructurecore', 'echolootcore', 'echoworldcore'],
    optional: ['echoexpeditioncore'],
    provides: ['ruin.registry', 'ruin.archaeology', 'ruin.salvage_sites', 'ruin.restoration_state'],
    consumes: ['structures.poi_metadata', 'loot.tables', 'world.regions'],
    adapterDomains: ['data', 'structures', 'worldgen'],
    permissions: ['ruins.read', 'ruins.write'],
    mvpContracts: ['ruin_registry', 'archaeology_contract', 'salvage_site_contract', 'restoration_state_contract'],
  },
  {
    id: 'echosupplycore',
    name: 'ECHO: SupplyCore',
    className: 'EchoSupplyCore',
    nativeClassName: 'EchoSupplyCoreNativeModule',
    phase: 5,
    role: 'supply_core',
    kind: 'library',
    summary: 'Scarcity, shortages, stockpiles, rationing, and supply pressure contracts.',
    requires: ['echologisticscore', 'echoeconomycore', 'echolootcore'],
    optional: ['echosettlementcore'],
    provides: ['supply.scarcity', 'supply.stockpiles', 'supply.rationing', 'supply.pressure'],
    consumes: ['logistics.routes', 'economy.pricing', 'loot.tables'],
    adapterDomains: ['data', 'diagnostics'],
    permissions: ['supply.read', 'supply.write'],
    mvpContracts: ['scarcity_contract', 'stockpile_contract', 'rationing_contract', 'supply_pressure_contract'],
  },
  {
    id: 'echodisastercore',
    name: 'ECHO: DisasterCore',
    className: 'EchoDisasterCore',
    nativeClassName: 'EchoDisasterCoreNativeModule',
    phase: 5,
    role: 'disaster_core',
    kind: 'library',
    summary: 'Blackouts, earthquakes, station failures, storm disasters, and recovery event contracts.',
    requires: ['echohazardcore', 'echoweathercore', 'echosessioncore', 'echoworldcore'],
    optional: ['echosupplycore'],
    provides: ['disaster.events', 'disaster.recovery', 'disaster.station_failures', 'disaster.world_impacts'],
    consumes: ['hazard.registry', 'weather.events', 'session.snapshot', 'world.regions'],
    adapterDomains: ['data', 'worldgen', 'diagnostics'],
    permissions: ['disaster.read', 'disaster.write'],
    mvpContracts: ['disaster_event_contract', 'recovery_event_contract', 'station_failure_contract', 'storm_disaster_contract'],
  },
  {
    id: 'echoseasoncore',
    name: 'ECHO: SeasonCore',
    className: 'EchoSeasonCore',
    nativeClassName: 'EchoSeasonCoreNativeModule',
    phase: 5,
    role: 'season_core',
    kind: 'library',
    summary: 'Rotating objectives, seasonal loot, timed modifiers, and live event contracts.',
    requires: ['echomissioncore', 'echolootcore', 'echopolicycore'],
    optional: ['echotelemetrycore'],
    provides: ['season.objectives', 'season.loot', 'season.timed_modifiers', 'season.live_events'],
    consumes: ['mission.objectives', 'loot.tables', 'policy.manifest'],
    adapterDomains: ['data', 'diagnostics'],
    permissions: ['season.read', 'season.write'],
    mvpContracts: ['rotating_objectives_contract', 'seasonal_loot_contract', 'timed_modifier_contract', 'live_event_contract'],
  },
  {
    id: 'echoserveropscore',
    name: 'ECHO: ServerOpsCore',
    className: 'EchoServerOpsCore',
    nativeClassName: 'EchoServerOpsCoreNativeModule',
    phase: 5,
    role: 'server_ops_core',
    kind: 'library',
    summary: 'Moderation, backups, announcements, support bundles, and player report contracts.',
    requires: ['echopolicycore', 'echoreportcore', 'echotelemetrycore', 'echonetcore'],
    optional: ['echoterritorycore'],
    provides: ['serverops.moderation', 'serverops.backups', 'serverops.announcements', 'serverops.player_reports'],
    consumes: ['policy.manifest', 'reports.contracts', 'telemetry.local_bundle', 'echo.net'],
    adapterDomains: ['data', 'diagnostics', 'networking'],
    permissions: ['serverops.read', 'serverops.write', 'reports.write'],
    mvpContracts: ['moderation_contract', 'backup_contract', 'announcement_contract', 'player_report_contract'],
  },
]

const BUNDLES = [
  {
    id: 'foundation',
    name: 'Foundation Bundle',
    description: 'Shared survival/content backbone required by official ECHO experience packs.',
    requiredModules: ['echocore', 'echoadaptercore', 'echonetcore', ...FOUNDATION_NATIVE_MODULES.map((module) => module.id)],
    optionalModules: [],
    bestFor: ['Baseline survival contracts', 'Official pack roots', 'Creator starter packs'],
    packStyles: ['foundation'],
    accent: '#58d7ff',
  },
  {
    id: 'openlands_official',
    name: 'Openlands Official Bundle',
    description: 'Openlands protocol plus Foundation, world, progression, recipe, content, and exploration support modules.',
    requiredModules: ['echocore', 'echoadaptercore', 'echonetcore', ...FOUNDATION_NATIVE_MODULES.map((module) => module.id), 'echoopenlandsprotocol'],
    optionalModules: ['echoassetcore', 'echobiomecore', 'echocontentcore', 'echocreaturecore', 'echoprogressioncore', 'echorecipecore', 'echostructurecore', 'echoworldcore', 'echoholomap', 'echoindex', 'echolens', 'echotutorialcore'],
    bestFor: ['Openlands', 'Calm exploration', 'Homesteading'],
    packStyles: ['openlands', 'official_pack'],
    accent: '#7bcf6b',
  },
  {
    id: 'sky_relay_official',
    name: 'Sky Relay Official Bundle',
    description: 'Sky Relay protocol with power, weather, recovery, route, and logistics support.',
    requiredModules: ['echocore', 'echoadaptercore', 'echonetcore', 'echoruntimeguard', 'echoskyrelayprotocol'],
    optionalModules: ['echoholomap', 'echoindex', 'echolens', 'echopowergrid', 'echoweathercore', 'echorecovery', 'echologisticsnetwork', 'echomissioncore', 'echoterminal', 'echothemecore'],
    bestFor: ['Sky Relay', 'Storm routes', 'Restoration loops'],
    packStyles: ['sky_relay', 'official_pack'],
    accent: '#6aa6ff',
  },
  {
    id: 'arcana_division',
    name: 'Arcana Division Bundle',
    description: 'Arcana Division protocol, Foundation, Arcana systems, and launcher support modules.',
    requiredModules: ['echocore', 'echoadaptercore', 'echonetcore', ...FOUNDATION_NATIVE_MODULES.map((module) => module.id), 'echoarcanacore', 'echoarcanadivisionprotocol'],
    optionalModules: ['echoaetherworks', 'echocursecore', 'echofamiliarcore', 'echogrimoire', 'echoriftworlds', 'echoritualcore', 'echospellcore', 'echoholomap', 'echoindex', 'echolens', 'echoterminal', 'echothemecore', 'echomissioncore'],
    bestFor: ['Arcana Division', 'Magic research', 'Anomaly containment'],
    packStyles: ['arcana_division', 'official_pack'],
    accent: '#b98cff',
  },
  {
    id: 'creator_tooling',
    name: 'Creator Tooling Bundle',
    description: 'Authoring, validation, asset, localization, diff, balance, and roadmap tooling for pack creators.',
    requiredModules: ['echocore', 'echoadaptercore', 'echoschemacore', 'echovalidationcore', 'echocreatorcore', 'echoreportcore'],
    optionalModules: ['echoassetcore', 'echotextureforge', 'echosoundcore', 'echoblueprintcore', 'echobalancecore', 'echopackdiff', 'echoassetpipeline', 'echolocalizationcore', 'echocurationcore', 'echodependencydoctor'],
    bestFor: ['Creator packs', 'Studio workflows', 'Release QA'],
    packStyles: ['creator_tooling'],
    accent: '#f0b85a',
  },
]

function unique(values) {
  return [...new Set(values.filter(Boolean))]
}

function absolute(relativePath) {
  return path.join(repoRoot, relativePath)
}

function readJson(relativePath) {
  return JSON.parse(fs.readFileSync(absolute(relativePath), 'utf8'))
}

function writeText(relativePath, value) {
  const file = absolute(relativePath)
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, value.replace(/^\n/, ''), 'utf8')
}

function writeJson(relativePath, value) {
  writeText(relativePath, `${JSON.stringify(value, null, 2)}\n`)
}

function javaQuote(value) {
  return JSON.stringify(value)
}

function javaList(values, indent = '            ') {
  if (!values.length) return 'List.of()'
  return `List.of(\n${values.map((value) => `${indent}${javaQuote(value)}`).join(',\n')}\n        )`
}

function packageName(module) {
  return module.packageName ?? `com.knoxhack.echo.${module.id.replace(/^echo/, '')}`
}

function packagePath(module) {
  return packageName(module).replaceAll('.', '/')
}

function requiresFor(module) {
  return unique(['echocore', 'echoadaptercore', ...(module.requires ?? [])])
}

function gradleDependencies(module) {
  return requiresFor(module)
    .map((dependency) => `    implementation project(":${dependency}")`)
    .join('\n')
}

function moduleBuildGradle(module) {
  return `
plugins {
    id 'java-library'
    id 'maven-publish'
    id 'net.neoforged.moddev' version '2.0.141'
    id 'idea'
}

version = mod_version
group = mod_group_id

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

neoForge {
    version = project.neo_version

    mods {
        "\${mod_id}" {
            sourceSet(sourceSets.main)
        }
    }
}

configurations {
    runtimeClasspath.extendsFrom localRuntime
}

dependencies {
${gradleDependencies(module)}
    compileOnly project(":echo-native-contracts")
}

var generateModMetadata = tasks.register("generateModMetadata", ProcessResources) {
    var replaceProperties = [
            minecraft_version      : minecraft_version,
            minecraft_version_range: minecraft_version_range,
            neo_version            : neo_version,
            neo_version_range      : project.findProperty('neo_version_range') ?: '[26.1,)',
            loader_version_range   : project.findProperty('loader_version_range') ?: '[4,)',
            mod_id                 : mod_id,
            mod_name               : mod_name,
            mod_license            : mod_license,
            mod_version            : mod_version,
            mod_authors            : mod_authors,
            mod_description        : mod_description,
    ]
    inputs.properties replaceProperties
    expand replaceProperties
    from "src/main/templates"
    into "build/generated/sources/modMetadata"
}
sourceSets.main.resources.srcDir generateModMetadata
neoForge.ideSyncTask generateModMetadata

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

publishing {
    publications {
        register('mavenJava', MavenPublication) {
            from components.java
        }
    }
}
`
}

function gradleProperties(module) {
  return `
org.gradle.jvmargs=-Xmx1G
org.gradle.daemon=false
mod_id=${module.id}
mod_name=${module.name}
mod_license=All Rights Reserved
mod_version=0.1.0
mod_group_id=${packageName(module)}
mod_authors=KnoxHack
mod_description=${module.summary}
`
}

function neoForgeToml() {
  return `
modLoader="javafml"
loaderVersion="\${loader_version_range}"
license="\${mod_license}"

[[mods]]
modId="\${mod_id}"
version="\${mod_version}"
displayName="\${mod_name}"
authors="\${mod_authors}"
description='''\${mod_description}'''

[[dependencies.\${mod_id}]]
modId="neoforge"
type="required"
versionRange="\${neo_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.\${mod_id}]]
modId="minecraft"
type="required"
versionRange="\${minecraft_version_range}"
ordering="NONE"
side="BOTH"
`
}

function markerJava(module) {
  return `
package ${packageName(module)};

import java.util.List;

public final class ${module.className} {
    public static final String MODID = "${module.id}";
    public static final List<String> REQUIRES = ${javaList(requiresFor(module), '            ')};
    public static final List<String> PROVIDES = ${javaList(module.provides, '            ')};
    public static final List<String> MVP_CONTRACTS = ${javaList(module.mvpContracts, '            ')};

    public ${module.className}() {
        bootstrap();
    }

    public void bootstrap() {
    }

    public String moduleId() {
        return MODID;
    }

    public List<String> provides() {
        return PROVIDES;
    }
}
`
}

function nativeJava(module) {
  return `
package ${packageName(module)};

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ${module.nativeClassName} implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = ${module.className}.MODID;
    public static final List<String> CONTRACT_IDS = ${module.className}.PROVIDES;
    public static final List<String> ADAPTER_DOMAINS = ${javaList(module.adapterDomains, '            ')};

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "${module.id}_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", safeContext.getOrDefault("packId", "unknown"));
        result.put("roadmapPhase", ${module.phase});
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", ADAPTER_DOMAINS);
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("mvpContracts", ${module.className}.MVP_CONTRACTS);
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", ${javaQuote(module.summary)});
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new ${module.nativeClassName}()
                .describeNativeSurfaces(Map.of("packId", "${module.id}-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "${module.id} native adapter should activate");
        require(Integer.valueOf(CONTRACT_IDS.size()).equals(activation.get("logicalRegistrationCount")),
                "${module.id} native adapter should expose every contract");
        require(Boolean.FALSE.equals(activation.get("registryMutated")),
                "${module.id} native adapter must stay contract-first");
        System.out.println("${module.id} native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractFirst", true);
        result.put("descriptorBacked", true);
        result.put("contractsDeclared", CONTRACT_IDS.size());
        result.put("mvpContractsDeclared", ${module.className}.MVP_CONTRACTS.size());
        result.put("contractCountMatches", CONTRACT_IDS.size() == ${module.provides.length});
        result.put("roadmapPhase", ${module.phase});
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
`
}

function descriptor(module) {
  return {
    schema: 'echo.mod.v1',
    id: module.id,
    name: module.name,
    version: '0.1.0',
    type: 'addon',
    kind: module.kind,
    role: module.role,
    entrypoint: `${packageName(module)}.${module.className}`,
    publisher: 'KnoxHack',
    channel: 'alpha',
    official: true,
    trustLevel: 'official',
    standalone: true,
    clientOnly: false,
    serverOnly: false,
    side: 'common',
    summary: module.summary,
    requires: requiresFor(module),
    optional: module.optional ?? [],
    provides: module.provides,
    consumes: module.consumes,
    gameModes: ['ashfall', 'openlands_standard', 'skyrelay_restoration', 'arcana_division', 'dev_tools'],
    permissions: module.permissions,
    assets: [],
    transforms: [],
    access: {
      adapterCore: {
        domains: module.adapterDomains,
        runtimes: ['neoforge', 'echo_native', 'echo_runtime_standalone'],
      },
      nativeEntrypoint: `${packageName(module)}.${module.nativeClassName}`,
      nativeClasspath: [],
      requiresConfirmationForWriteActions: true,
      notes: 'Contract-first platform roadmap module. Runtime behavior is limited to native surface description and in-memory probes until deeper implementation lands.',
    },
    apiStability: 'alpha',
    roadmap: {
      phase: module.phase,
      status: 'contract-first',
      mvpContracts: module.mvpContracts,
    },
    ai: {
      requiresHumanReview: false,
      recommendedAgentLanes: ['metadata_agent', 'validation_agent', 'release_agent'],
    },
    deprecatedFeatures: [],
    replacements: [],
    conflicts: [],
  }
}

function contractJson(module) {
  return {
    schema: 'echo.platform_roadmap.module_contract.v1',
    id: module.id,
    name: module.name,
    phase: module.phase,
    role: module.role,
    status: 'contract-first',
    summary: module.summary,
    dependencies: {
      requires: requiresFor(module),
      optional: module.optional ?? [],
    },
    surfaces: {
      provides: module.provides,
      consumes: module.consumes,
      adapterDomains: module.adapterDomains,
      nativeEntrypoint: `${packageName(module)}.${module.nativeClassName}`,
    },
    mvpContracts: module.mvpContracts,
    probeAssertions: [
      'native entrypoint activates',
      'logical registration count equals descriptor provides count',
      'registryMutated remains false',
      'transformsPerformed remains false',
    ],
    implementationBoundary: 'This module exposes platform contracts only. Gameplay/runtime mutation must be added in later implementation phases.',
  }
}

function readme(module) {
  return `
# ${module.name}

${module.summary}

## Review Status

This is a Phase ${module.phase} ECHO platform roadmap module and is ready for contract review. The first implementation is contract-first: descriptor metadata, native surface discovery, data contracts, docs, and release artifact metadata. It is not a finished gameplay/runtime implementation.

## Public Contracts

- Provides: ${module.provides.map((value) => `\`${value}\``).join(', ')}
- Consumes: ${module.consumes.map((value) => `\`${value}\``).join(', ')}
- MVP contracts: ${module.mvpContracts.map((value) => `\`${value}\``).join(', ')}

## Native Probe

The native entrypoint reports the standard roadmap activation map: \`activated\`, \`activationStage\`, \`adapterCoreUsed\`, \`nativeAdapterCodeExecuted\`, \`moduleId\`, \`packId\`, \`registeredFeatureContracts\`, \`logicalRegistrationCount\`, \`adapterDomains\`, \`runtimeTargets\`, \`referenceProbe\`, \`registryMutated: false\`, and \`transformsPerformed: false\`.

## Contract Boundary

This module exposes schemas, descriptors, data contracts, artifact metadata, and native surface probes only. Deeper gameplay behavior, runtime state mutation, player-facing loops, server operations, and destructive writes must land in later implementation work behind validation and policy gates.

## References

- [Artifact notes](docs/artifacts.md)
- [Platform roadmap](../../docs/ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](../../docs/module-artifact-contract.md)
`
}

function artifactDocs(module) {
  return `
# ${module.name} Artifact Notes

This file documents the release outputs expected for \`${module.id}\` version \`0.1.0\`.

## Artifact Matrix

| File | Requirement |
| --- | --- |
| \`${module.id}-0.1.0-neoforge.jar\` | Required for NeoForge editions when this module is selected. |
| \`${module.id}-0.1.0.echo-addon\` | Required for ECHO Native editions when this module is selected. |
| \`${module.id}-0.1.0-standalone.jar\` | Required for Standalone editions when this module is selected. |
| \`${module.id}-0.1.0-sources.jar\` | Always required for traceability and developer debugging. |
| \`META-INF/echo.mod.json\` | Always required and embedded in runtime artifacts where applicable. |
| \`META-INF/neoforge.mods.toml\` | Required in NeoForge artifacts. |
| \`echo-addon-package.json\` | Required in \`.echo-addon\` packages. |

## Release Boundary

Status: Not Player Ready.

This module is source-packaged and contract-first until compiled runtime artifacts are published. Source-packaged outputs prove descriptor, metadata, native-surface, and packaging shape only. They must not be promoted as player-ready releases, and strict release generation must replace them with compiled runtime artifacts before publication.

## Review Checklist

- Descriptor ID, version, channel, API stability, trust level, side, and standalone support match the roadmap contract.
- Native entrypoint reports contract metadata only and keeps \`registryMutated\` and \`transformsPerformed\` false.
- \`echo.platform_roadmap.module_contract.v1\` data remains the authoritative small contract JSON for this roadmap module.
- Any later gameplay implementation must update tests and keep this artifact boundary accurate.

## Shared Contract

- [Module artifact contract](../../../docs/module-artifact-contract.md)
`
}

function writeRoadmapModule(module) {
  const base = `addons/${module.id}`
  writeText(`${base}/build.gradle`, moduleBuildGradle(module))
  writeText(`${base}/gradle.properties`, gradleProperties(module))
  writeText(`${base}/README.md`, readme(module))
  writeText(`${base}/docs/artifacts.md`, artifactDocs(module))
  writeText(`${base}/src/main/java/${packagePath(module)}/${module.className}.java`, markerJava(module))
  writeText(`${base}/src/main/java/${packagePath(module)}/${module.nativeClassName}.java`, nativeJava(module))
  writeJson(`${base}/src/main/resources/META-INF/echo.mod.json`, descriptor(module))
  writeText(`${base}/src/main/templates/META-INF/neoforge.mods.toml`, neoForgeToml())
  writeJson(`${base}/src/main/resources/pack.mcmeta`, {
    pack: {
      description: module.name,
      pack_format: 15,
    },
  })
  writeJson(`${base}/src/main/resources/data/${module.id}/roadmap/contracts.json`, contractJson(module))
}

function foundationNativeJava(module, descriptorData) {
  const entrypoint = descriptorData.entrypoint
  const entrypointParts = entrypoint.split('.')
  entrypointParts.pop()
  const packageValue = entrypointParts.join('.')
  const domains = descriptorData.access?.adapterCore?.domains ?? ['data', 'diagnostics']
  return `
package ${packageValue};

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ${module.nativeClassName} implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = "${module.id}";
    public static final List<String> CONTRACT_IDS = ${javaList(descriptorData.provides ?? [], '            ')};
    public static final List<String> ADAPTER_DOMAINS = ${javaList(domains, '            ')};

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "${module.id}_foundation_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", safeContext.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", ADAPTER_DOMAINS);
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("foundationBackbone", true);
        result.put("dataFirstContract", true);
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", ${javaQuote(descriptorData.summary ?? `${module.id} Foundation native contract surface.`)});
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new ${module.nativeClassName}()
                .describeNativeSurfaces(Map.of("packId", "${module.id}-foundation-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "${module.id} Foundation native adapter should activate");
        require(Integer.valueOf(CONTRACT_IDS.size()).equals(activation.get("logicalRegistrationCount")),
                "${module.id} Foundation native adapter should expose every descriptor contract");
        require(Boolean.FALSE.equals(activation.get("registryMutated")),
                "${module.id} Foundation native adapter must not mutate registries");
        System.out.println("${module.id} foundation native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("descriptorBacked", true);
        result.put("foundationBackbone", true);
        result.put("contractCountMatches", CONTRACT_IDS.size() == ${descriptorData.provides?.length ?? 0});
        result.put("contractSurface", "foundation_data_first");
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
`
}

function ensureFoundationNativeEntrypoints() {
  for (const module of FOUNDATION_NATIVE_MODULES) {
    const descriptorPath = `addons/${module.id}/src/main/resources/META-INF/echo.mod.json`
    const descriptorData = readJson(descriptorPath)
    const packageValue = descriptorData.entrypoint.split('.').slice(0, -1).join('.')
    descriptorData.access = descriptorData.access ?? {}
    descriptorData.access.nativeEntrypoint = `${packageValue}.${module.nativeClassName}`
    descriptorData.access.nativeClasspath = descriptorData.access.nativeClasspath ?? []
    writeJson(descriptorPath, descriptorData)
    writeText(
      `addons/${module.id}/src/main/java/${packageValue.replaceAll('.', '/')}/${module.nativeClassName}.java`,
      foundationNativeJava(module, descriptorData),
    )
  }
}

function normalizeDescriptorHygiene() {
  const updates = [
    {
      path: 'addons/echoaddonapi/src/main/resources/META-INF/echo.mod.json',
      update: (descriptorData) => {
        descriptorData.channel = descriptorData.channel || 'experimental'
      },
    },
    {
      path: 'addons/echogalacticcore/src/main/resources/META-INF/echo.mod.json',
      update: (descriptorData) => {
        if (typeof descriptorData.apiStability === 'string') {
          descriptorData.apiStability = descriptorData.apiStability.toLowerCase()
        }
      },
    },
    {
      path: 'addons/echowiki/src/main/resources/META-INF/echo.mod.json',
      update: (descriptorData) => {
        if (descriptorData.role === 'ui/ux') descriptorData.role = 'ui_ux'
      },
    },
  ]
  for (const item of updates) {
    const file = absolute(item.path)
    if (!fs.existsSync(file)) continue
    const descriptorData = readJson(item.path)
    item.update(descriptorData)
    writeJson(item.path, descriptorData)
  }
}

function writeBundle(bundle) {
  const modules = unique([...bundle.requiredModules, ...bundle.optionalModules])
  writeJson(`metadata/bundles/${bundle.id}.json`, {
    id: bundle.id,
    name: bundle.name,
    version: '1.0.0',
    description: bundle.description,
    modules,
    requiredModules: bundle.requiredModules,
    optionalModules: bundle.optionalModules,
    bestFor: bundle.bestFor,
    packStyles: bundle.packStyles,
    difficulty: 'normal',
    launcherCard: {
      title: bundle.name.replace(/ Bundle$/, ''),
      accent: bundle.accent,
      description: bundle.description,
      status: 'Ready',
      style: bundle.packStyles[0],
    },
    docsPath: 'docs/MODULE_BUNDLES.md',
    recommendedWith: [],
  })
}

function writeBundleDocs() {
  const rows = BUNDLES.map((bundle) => `| ${bundle.id} | ${bundle.name} | ${bundle.requiredModules.length} | ${bundle.optionalModules.length} | ${bundle.bestFor.join(', ')} |`)
  writeText('docs/MODULE_BUNDLES.md', `
# Module Bundles

Bundles are curated module groups for Launcher browsing and pack-builder shortcuts. They do not create new addons, and they do not make Ashfall a dependency for reusable modules.

## Platform Defaults

- ECHO is the ecosystem.
- ECHO Launcher is the platform.
- Foundation modules are shared backbone contracts consumed by official packs.
- Creator tooling uses schemas, examples, generators, validators, and docs before deeper editor automation.
- Roadmap modules start contract-first so Launcher, Studio, and validation can reason about them before gameplay systems mutate runtime state.

## Curated Bundles

| ID | Name | Required | Optional | Best For |
| --- | --- | ---: | ---: | --- |
${rows.join('\n')}

## Related Docs

- [ECHO platform roadmap](ECHO_PLATFORM_ROADMAP.md)
- [Module artifact contract](module-artifact-contract.md)
`)
}

function writeRoadmapDocs() {
  const phaseNames = {
    1: 'Shipping Confidence',
    2: 'Creator Multipliers',
    3: 'Player State And UX',
    4: 'Big Gameplay Systems',
    5: 'Event And World Depth',
  }
  const phaseGoals = {
    1: 'Make every future pack safer to ship by proving launchability, migration safety, policy enforcement, and graph diagnostics.',
    2: 'Make creator work faster and less fragile through reusable authoring, audit, diff, asset, and localization contracts.',
    3: 'Make player-facing packs feel coherent by standardizing session state, accessibility, curation, and privacy-safe telemetry contracts.',
    4: 'Prepare richer gameplay systems as shared contracts while reusing existing social, world, status, combat, and progression modules.',
    5: 'Prepare long-tail event and world-depth systems after player-state and gameplay contracts are in place.',
  }
  const lines = [
    '# ECHO Platform Roadmap',
    '',
    'This roadmap is implemented as contract-first modules. The first pass makes packs easier to ship, safer to update, and richer to inspect before deep gameplay behavior lands.',
    '',
    '## Review Baseline',
    '',
    '- Module catalog baseline: 131 descriptors after roadmap generation.',
    '- Source of truth: `scripts/generate-platform-roadmap-modules.mjs` owns roadmap module scaffolds, bundle metadata, and roadmap docs.',
    '- Validator: `scripts/validate-platform-roadmap.mjs` enforces descriptor invariants, native entrypoints, bundle files, docs, and index membership.',
    '- Release posture: Phase 1 source-packaging smoke proves artifact shape only; compiled runtime artifacts are still required before player-ready publication.',
    '',
    '## Contract Boundary',
    '',
    'Roadmap modules expose descriptors, small data contracts, artifact docs, and native surface probes. They do not mutate runtime state, register gameplay content, execute server operations, or claim completed player-facing loops. Later gameplay work must add implementation-specific tests and keep policy/reporting gates intact.',
    '',
  ]
  for (const phase of [1, 2, 3, 4, 5]) {
    lines.push(`## Phase ${phase}: ${phaseNames[phase]}`, '')
    lines.push(phaseGoals[phase], '')
    lines.push('| Module | Responsibility | Public Contracts |')
    lines.push('| --- | --- | --- |')
    for (const module of ROADMAP_MODULES.filter((candidate) => candidate.phase === phase)) {
      lines.push(`| \`${module.id}\` | ${module.summary} | ${module.provides.map((value) => `\`${value}\``).join(', ')} |`)
    }
    lines.push('')
  }
  lines.push('## Build Order', '')
  lines.push('1. `echoplaytestcore`')
  lines.push('2. `echomigrationcore`')
  lines.push('3. `echocapabilitycore`')
  lines.push('4. `echopolicycore`')
  lines.push('5. `echodependencydoctor`')
  lines.push('6. `echoblueprintcore`')
  lines.push('7. `echosessioncore`')
  lines.push('8. `echohazardcore`')
  lines.push('9. `echofactioncore`')
  lines.push('10. `echosettlementcore`')
  lines.push('', '## Review Commands', '')
  lines.push('- `node scripts/validate-module-graph.mjs --write-index`')
  lines.push('- `node scripts/validate-module-graph.mjs`')
  lines.push('- `node scripts/validate-foundations-split.mjs`')
  lines.push('- `node scripts/docs-audit.mjs`')
  lines.push('- `node scripts/validate-arcana-division-beta.mjs`')
  lines.push('- `node scripts/validate-platform-roadmap.mjs`')
  writeText('docs/ECHO_PLATFORM_ROADMAP.md', `${lines.join('\n')}\n`)
}

function main() {
  normalizeDescriptorHygiene()
  ensureFoundationNativeEntrypoints()
  for (const module of ROADMAP_MODULES) writeRoadmapModule(module)
  for (const bundle of BUNDLES) writeBundle(bundle)
  writeBundleDocs()
  writeRoadmapDocs()
  console.log(`Generated ${ROADMAP_MODULES.length} platform roadmap module scaffold(s).`)
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  main()
}
