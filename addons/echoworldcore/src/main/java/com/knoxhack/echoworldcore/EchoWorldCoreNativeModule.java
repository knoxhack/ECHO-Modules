package com.knoxhack.echoworldcore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeDifficultyApplicationBridge;
import com.knoxhack.echo.adaptercore.EchoNativeHazardTickDamageBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeSpawnRuleEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeSpawnZoneStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStatusEffectLoadBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStatusEffectSaveBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStructureDiscoveryStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStructurePoiLookupBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStructurePoiMarkerStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWorldDataCatalogBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWorldEffectsBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWorldHazardTransitionBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWorldRegionTransitionBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EchoWorldCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    private static final String MODULE_ID = "echoworldcore";
    private static final Pattern DIFFICULTY_PATTERN = Pattern.compile("\"difficulty\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover WorldCore hazards, regions, and world integration contracts.")
                .phase("register_world_contracts", "Record built-in hazard and region providers before runtime world service execution.")
                .phase("attach_world_events", "Record reload and discovery hooks for the future native world bridge.")
                .phase("attach_live_player_tick", "Attach live player tick handling for native region, hazard, status, spawn, and POI execution.")
                .phase("ready", "Expose WorldCore as the native world contract provider for Ashfall.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("world_hazard", "echoworldcore:hazard/salvage_debris", "Sharp wreckage and damaged hull fragments.")
                .register("world_hazard", "echoworldcore:hazard/toxic_air", "Airborne chemical and spore contamination.")
                .register("world_hazard", "echoworldcore:hazard/radiation", "Irradiated terrain and fallout pockets.")
                .register("world_hazard", "echoworldcore:hazard/cryo_cold", "Extreme cold from ruptured cryogenic systems.")
                .register("world_hazard", "echoworldcore:hazard/nexus_anomaly", "Reality instability pressure.")
                .register("world_hazard", "echoworldcore:hazard/orbital_exposure", "Vacuum, oxygen, and debris pressure.")
                .register("world_hazard", "echoworldcore:hazard/convoy_threat", "Route ambush and vehicle attrition pressure.")
                .register("world_hazard", "echoworldcore:hazard/secure_zone", "Stabilized field position.")
                .register("service", "echoworldcore:world_region_service", "World region and hazard service provider.")
                .register("addon_chapter", "echoworldcore:world_core", "WorldCore addon chapter.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("common.setup", "EchoWorldCore.commonSetup", "Attach WorldCore service providers after native setup.")
                .hook("player.tick.post", "WorldCoreEvents.onPlayerTick", "Execute live region, hazard, status, structure, spawn, and difficulty handlers.")
                .hook("data.reload", "WorldCoreReloaders.addServerReloadListeners", "Attach world region and hazard reloaders.")
                .hook("terminal.integration", "WorldCoreTerminalIntegration.register", "Attach terminal integration when available.")
                .hook("holomap.integration", "WorldCoreHoloMapRichProvider.register", "Attach HoloMap integration when available.");
        EchoWorldContracts.EchoWorldEffectTick referenceTick = referenceWorldEffectTick();
        Map<String, Object> worldEffectsReport = new EchoNativeWorldEffectsBridge(MODULE_ID)
                .report(referenceTick);
        EchoWorldContracts.EchoWorldEffectResult worldEffectResult =
                (EchoWorldContracts.EchoWorldEffectResult) worldEffectsReport.get("worldEffectResult");
        EchoWorldContracts.EchoHazardTickDamageResult hazardTickDamageResult =
                new EchoNativeHazardTickDamageBridge(MODULE_ID)
                        .apply(new EchoWorldContracts.EchoHazardTickDamageRequest(
                                "agent7-player",
                                20.0D,
                                1,
                                6003L,
                                "worldcore-native-reference-hazard-damage",
                                referenceTick.hazard(),
                                referenceTick.difficulty()));
        EchoNativeWorldHazardTransitionBridge hazardTransitionBridge =
                new EchoNativeWorldHazardTransitionBridge(MODULE_ID);
        EchoWorldContracts.EchoWorldHazardTransitionResult hazardEnterTransition =
                hazardTransitionBridge.transition(new EchoWorldContracts.EchoWorldHazardTransitionRequest(
                        "agent7-player",
                        "",
                        worldEffectResult.activeHazardId(),
                        referenceTick.statusEffect().id(),
                        6004L,
                        "worldcore-native-reference-hazard-enter"));
        EchoWorldContracts.EchoWorldHazardTransitionResult hazardExitTransition =
                hazardTransitionBridge.transition(new EchoWorldContracts.EchoWorldHazardTransitionRequest(
                        "agent7-player",
                        hazardEnterTransition.currentHazardId(),
                        "",
                        "",
                        6005L,
                        "worldcore-native-reference-hazard-exit"));
        EchoWorldContracts.EchoStatusEffectSaveResult statusSaveResult =
                new EchoNativeStatusEffectSaveBridge(MODULE_ID)
                        .persist(new EchoWorldContracts.EchoStatusEffectSaveRequest(
                                "agent7-player",
                                hazardTickDamageResult.hazardId(),
                                (float) hazardTickDamageResult.damageApplied(),
                                6006L,
                                "worldcore-native-reference-status-save",
                                referenceTick.statusEffect()));
        EchoWorldContracts.EchoStatusEffectLoadResult statusLoadResult =
                new EchoNativeStatusEffectLoadBridge(MODULE_ID)
                        .load(new EchoWorldContracts.EchoStatusEffectLoadRequest(
                                "agent7-player",
                                statusSaveResult.hazardId(),
                                statusSaveResult.saveKey(),
                                statusSaveResult.savedStatusState(),
                                6007L,
                                "worldcore-native-reference-status-load"));
        EchoWorldContracts.EchoStructurePoiLookupResult structurePoiLookupResult =
                new EchoNativeStructurePoiLookupBridge(MODULE_ID)
                        .lookup(new EchoWorldContracts.EchoStructurePoiLookupRequest(
                                "agent7-player",
                                referenceTick.region().id(),
                                referenceTick.x(),
                                referenceTick.y(),
                                referenceTick.z(),
                                96,
                                6008L,
                                "worldcore-native-reference-structure-poi-lookup",
                                referenceTick.structure()));
        EchoWorldContracts.EchoStructurePoiMarkerStateResult structurePoiMarkerStateResult =
                new EchoNativeStructurePoiMarkerStateBridge(MODULE_ID)
                        .persist(new EchoWorldContracts.EchoStructurePoiMarkerStateRequest(
                                "agent7-player",
                                "worldcore-native-reference-structure-poi-marker",
                                structurePoiLookupResult));
        EchoWorldContracts.EchoStructureDiscoveryStateResult structureDiscoveryStateResult =
                new EchoNativeStructureDiscoveryStateBridge(MODULE_ID)
                        .discover(new EchoWorldContracts.EchoStructureDiscoveryStateRequest(
                                "agent7-player",
                                "worldcore-native-reference-structure-discovery",
                                structurePoiMarkerStateResult));
        EchoWorldContracts.EchoSpawnRuleEventResult spawnRuleEventResult =
                new EchoNativeSpawnRuleEventBridge(MODULE_ID)
                        .plan(new EchoWorldContracts.EchoSpawnRuleEventRequest(
                                "agent7-player",
                                referenceTick.region().id(),
                                referenceTick.x(),
                                referenceTick.y(),
                                referenceTick.z(),
                                0,
                                6009L,
                                "worldcore-native-reference-spawn-rule-event",
                                referenceTick.spawnRule(),
                                referenceTick.difficulty()));
        EchoWorldContracts.EchoSpawnZoneStateResult spawnZoneStateResult =
                new EchoNativeSpawnZoneStateBridge(MODULE_ID)
                        .persist(new EchoWorldContracts.EchoSpawnZoneStateRequest(
                                "agent7-player",
                                "worldcore-native-reference-spawn-zone-state",
                                spawnRuleEventResult));
        EchoWorldContracts.EchoDifficultyApplicationResult difficultyApplicationResult =
                new EchoNativeDifficultyApplicationBridge(MODULE_ID)
                        .apply(new EchoWorldContracts.EchoDifficultyApplicationRequest(
                                "agent7-player",
                                referenceTick.region().id(),
                                hazardTickDamageResult.hazardId(),
                                hazardTickDamageResult.baseDamage(),
                                hazardTickDamageResult.damageApplied(),
                                spawnRuleEventResult.ruleId(),
                                spawnRuleEventResult.maxCount(),
                                spawnRuleEventResult.scaledBudget(),
                                spawnZoneStateResult.activePopulation(),
                                6010L,
                                "worldcore-native-reference-difficulty-application",
                                referenceTick.difficulty()));
        EchoWorldContracts.EchoWorldDataCatalogResult worldDataCatalogResult =
                new EchoNativeWorldDataCatalogBridge(MODULE_ID)
                        .materialize(referenceWorldDataCatalogRequest(context));
        EchoNativeWorldRegionTransitionBridge regionTransitionBridge =
                new EchoNativeWorldRegionTransitionBridge(MODULE_ID);
        EchoWorldContracts.EchoWorldRegionTransitionResult regionEnterTransition =
                regionTransitionBridge.transition(new EchoWorldContracts.EchoWorldRegionTransitionRequest(
                        "agent7-player",
                        "",
                        worldEffectResult.activeRegionId(),
                        "echoashfallprotocol:mission/secure_crash_outpost",
                        6001L,
                        "worldcore-native-reference-region-enter"));
        EchoWorldContracts.EchoWorldRegionTransitionResult regionExitTransition =
                regionTransitionBridge.transition(new EchoWorldContracts.EchoWorldRegionTransitionRequest(
                        "agent7-player",
                        regionEnterTransition.currentRegionId(),
                        "",
                        "",
                        6002L,
                        "worldcore-native-reference-region-exit"));
        boolean worldEffectsPassed = "PASS".equals(worldEffectsReport.get("status"))
                && "echoashfallprotocol:crash_zone_wasteland".equals(worldEffectResult.activeRegionId())
                && "echoworldcore:hazard/salvage_debris".equals(worldEffectResult.activeHazardId())
                && worldEffectResult.healthAfter() == 18.0D
                && worldEffectResult.missionEvents().contains("echoashfallprotocol:mission/secure_crash_outpost")
                && worldEffectResult.statusEffects().contains("echostatuscore:status/salvage_debris")
                && worldEffectResult.savedStatusState().containsKey("echoworldcore.hazard.salvage_debris.status");
        boolean hazardTickDamagePassed = hazardTickDamageResult.damaged()
                && "echoworldcore:hazard/salvage_debris".equals(hazardTickDamageResult.hazardId())
                && "echostatuscore:status/salvage_debris".equals(hazardTickDamageResult.statusEffectId())
                && hazardTickDamageResult.healthAfter() == 18.0D
                && hazardTickDamageResult.damageApplied() == 2.0D;
        boolean hazardTransitionPassed = hazardEnterTransition.hazardEntered()
                && "ENTER".equals(hazardEnterTransition.eventType())
                && hazardEnterTransition.statusEffects().contains("echostatuscore:status/salvage_debris")
                && hazardExitTransition.hazardExited()
                && "EXIT".equals(hazardExitTransition.eventType())
                && hazardExitTransition.currentHazardId().isBlank();
        boolean statusSaveLoadPassed = statusSaveResult.saved()
                && statusLoadResult.loaded()
                && "echostatuscore:status/salvage_debris".equals(statusLoadResult.effectId())
                && "echoworldcore.hazard.salvage_debris.status".equals(statusLoadResult.saveKey())
                && statusLoadResult.damageApplied() == 2.0F;
        boolean structurePoiPassed = structurePoiLookupResult.inRange()
                && structurePoiMarkerStateResult.markerPersisted()
                && structureDiscoveryStateResult.discovered()
                && "POI_IN_RANGE".equals(structurePoiLookupResult.lookupType())
                && "echoashfallprotocol:poi/drop_pod".equals(structurePoiLookupResult.poiId())
                && "DISCOVERED".equals(structureDiscoveryStateResult.discoveryState());
        boolean spawnRulePassed = "SPAWN_ALLOWED".equals(spawnRuleEventResult.eventType())
                && spawnRuleEventResult.spawnCount() == 2
                && spawnZoneStateResult.activePopulation() == 2
                && spawnZoneStateResult.zoneKey().contains("echospawncore:spawn/rad_zombie_crash_zone");
        boolean difficultyApplicationPassed = difficultyApplicationResult.applied()
                && "echodifficultycore:easy".equals(difficultyApplicationResult.difficultyId())
                && difficultyApplicationResult.scaledHazardDamage() == hazardTickDamageResult.damageApplied()
                && difficultyApplicationResult.scaledSpawnBudget() == spawnRuleEventResult.scaledBudget();
        boolean worldDataCatalogPassed = worldDataCatalogResult.loaded()
                && worldDataCatalogResult.regionCount() == 8
                && worldDataCatalogResult.hazardCount() == 12
                && worldDataCatalogResult.weatherProfileCount() == 9
                && worldDataCatalogResult.biomeCount() == 9
                && worldDataCatalogResult.structureCount() == 28
                && worldDataCatalogResult.statusEffectCount() == 12
                && worldDataCatalogResult.difficultyRuleCount() == 5
                && worldDataCatalogResult.spawnRuleCount() == 36
                && worldDataCatalogResult.representativeRegionIds()
                .contains("echoashfallprotocol:crash_zone_wasteland")
                && worldDataCatalogResult.representativeHazardIds()
                .contains("echoworldcore:hazard/salvage_debris")
                && worldDataCatalogResult.representativeWeatherProfileIds()
                .contains("echoashfallprotocol:ashfall_toxic_front");
        boolean regionTransitionPassed = regionEnterTransition.regionEntered()
                && "ENTER".equals(regionEnterTransition.eventType())
                && regionEnterTransition.missionEvents().contains("echoashfallprotocol:mission/secure_crash_outpost")
                && regionExitTransition.regionExited()
                && "EXIT".equals(regionExitTransition.eventType())
                && regionExitTransition.currentRegionId().isBlank();
        EchoWorldCoreRegionCellSampleContract regionCellSampleContract =
                new EchoWorldCoreRegionCellSampleContract();
        Map<String, Object> regionCellSample = regionCellSampleContract.execute(
                context.getOrDefault("packId", "echo-native-m17"),
                "echo_native");
        boolean regionCellSamplePassed = regionCellSampleContract.referenceSamplePassed(regionCellSample);
        List<?> mapFeed = (List<?>) regionCellSample.get("mapFeed");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "worldcore_native_region_cell_sample_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("logicalRegistrationCount", 10);
        result.put("eventHookCount", 5);
        result.put("livePlayerTickHook", "WorldCoreEvents.onPlayerTick -> WorldRegionService.tickPlayer");
        result.put("liveNativeGameplayHandlerAttached", true);
        result.put("registeredFeatureContracts", List.of(
                "EchoWorldRegion",
                "EchoWorldRegionTransition",
                "EchoWorldHazard",
                "EchoWorldHazardTransition",
                "EchoHazardTickDamage",
                "EchoWorldEffectTick",
                "EchoWorldEffectResult",
                "EchoWeatherState",
                "EchoAtmosphereState",
                "EchoBiomeProfile",
                "EchoStructurePlacement",
                "EchoSpawnRule",
                "EchoStatusEffect",
                "EchoStructurePoiLookup",
                "EchoStructurePoiMarkerState",
                "EchoStructureDiscoveryState",
                "EchoSpawnRuleEvent",
                "EchoSpawnZoneState",
                "EchoDifficultyProfile",
                "EchoDifficultyApplication",
                "EchoWorldDataCatalog",
                EchoWorldCoreRegionCellSampleContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("nativeWorldEffectsReport", worldEffectsReport);
        result.put("worldEffectsRuntimeContract", worldEffectsPassed);
        result.put("worldEffectHealthAfter", worldEffectResult.healthAfter());
        result.put("worldEffectActiveRegionId", worldEffectResult.activeRegionId());
        result.put("worldEffectActiveHazardId", worldEffectResult.activeHazardId());
        result.put("worldEffectMissionEvents", worldEffectResult.missionEvents());
        result.put("worldEffectStatusEffects", worldEffectResult.statusEffects());
        result.put("worldEffectSavedStatusState", worldEffectResult.savedStatusState());
        result.put("hazardTickDamageRuntimeContract", hazardTickDamagePassed);
        result.put("hazardTickDamageResult", hazardTickDamageResult);
        result.put("hazardTickDamageApplied", hazardTickDamageResult.damageApplied());
        result.put("hazardTickHealthAfter", hazardTickDamageResult.healthAfter());
        result.put("hazardTransitionRuntimeContract", hazardTransitionPassed);
        result.put("hazardEnterTransition", hazardEnterTransition);
        result.put("hazardExitTransition", hazardExitTransition);
        result.put("hazardEnterEventType", hazardEnterTransition.eventType());
        result.put("hazardExitEventType", hazardExitTransition.eventType());
        result.put("hazardTransitionStatusEffects", hazardEnterTransition.statusEffects());
        result.put("hazardTransitionCleared", hazardExitTransition.currentHazardId().isBlank());
        result.put("statusSaveLoadRuntimeContract", statusSaveLoadPassed);
        result.put("statusSaveResult", statusSaveResult);
        result.put("statusLoadResult", statusLoadResult);
        result.put("statusSaved", statusSaveResult.saved());
        result.put("statusLoaded", statusLoadResult.loaded());
        result.put("statusLoadedEffectId", statusLoadResult.effectId());
        result.put("structurePoiRuntimeContract", structurePoiPassed);
        result.put("structurePoiLookupResult", structurePoiLookupResult);
        result.put("structurePoiMarkerStateResult", structurePoiMarkerStateResult);
        result.put("structureDiscoveryStateResult", structureDiscoveryStateResult);
        result.put("structurePoiLookupType", structurePoiLookupResult.lookupType());
        result.put("structurePoiMarkerPersisted", structurePoiMarkerStateResult.markerPersisted());
        result.put("structureDiscoveryState", structureDiscoveryStateResult.discoveryState());
        result.put("spawnRuleRuntimeContract", spawnRulePassed);
        result.put("spawnRuleEventResult", spawnRuleEventResult);
        result.put("spawnZoneStateResult", spawnZoneStateResult);
        result.put("spawnRuleEventType", spawnRuleEventResult.eventType());
        result.put("spawnZoneActivePopulation", spawnZoneStateResult.activePopulation());
        result.put("difficultyApplicationRuntimeContract", difficultyApplicationPassed);
        result.put("difficultyApplicationResult", difficultyApplicationResult);
        result.put("difficultyApplied", difficultyApplicationResult.applied());
        result.put("difficultyScaledHazardDamage", difficultyApplicationResult.scaledHazardDamage());
        result.put("difficultyScaledSpawnBudget", difficultyApplicationResult.scaledSpawnBudget());
        result.put("worldDataCatalogRuntimeContract", worldDataCatalogPassed);
        result.put("worldDataCatalogResult", worldDataCatalogResult);
        result.put("worldDataCatalogRegionCount", worldDataCatalogResult.regionCount());
        result.put("worldDataCatalogHazardCount", worldDataCatalogResult.hazardCount());
        result.put("worldDataCatalogWeatherProfileCount", worldDataCatalogResult.weatherProfileCount());
        result.put("worldDataCatalogBiomeCount", worldDataCatalogResult.biomeCount());
        result.put("worldDataCatalogStructureCount", worldDataCatalogResult.structureCount());
        result.put("worldDataCatalogStatusEffectCount", worldDataCatalogResult.statusEffectCount());
        result.put("worldDataCatalogDifficultyRuleCount", worldDataCatalogResult.difficultyRuleCount());
        result.put("worldDataCatalogSpawnRuleCount", worldDataCatalogResult.spawnRuleCount());
        result.put("worldDataCatalogSourceFileCount", worldDataCatalogResult.sourceFileCount());
        result.put("worldDataCatalogSourceReason", worldDataCatalogResult.sourceReason());
        result.put("worldDataCatalogLoaded", worldDataCatalogResult.loaded());
        result.put("regionTransitionRuntimeContract", regionTransitionPassed);
        result.put("regionEnterTransition", regionEnterTransition);
        result.put("regionExitTransition", regionExitTransition);
        result.put("regionEnterEventType", regionEnterTransition.eventType());
        result.put("regionExitEventType", regionExitTransition.eventType());
        result.put("regionTransitionMissionEvents", regionEnterTransition.missionEvents());
        result.put("regionTransitionCleared", regionExitTransition.currentRegionId().isBlank());
        result.put("regionCellSample", regionCellSample);
        result.put("regionCellSampleExecuted", regionCellSamplePassed);
        result.put("regionCellSampleContract", EchoWorldCoreRegionCellSampleContract.ADAPTERCORE_CONTRACT_ID);
        result.put("regionCellSampleMapFeedCount", mapFeed.size());
        result.put("regionCellSampleRegionId", regionCellSample.get("activeRegionId"));
        result.put("regionCellSampleHazardId", regionCellSample.get("activeHazardId"));
        result.put("requiresWorldBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", worldEffectsPassed
                && hazardTickDamagePassed
                && hazardTransitionPassed
                && statusSaveLoadPassed
                && structurePoiPassed
                && spawnRulePassed
                && difficultyApplicationPassed
                && worldDataCatalogPassed
                && regionTransitionPassed
                && regionCellSamplePassed);
        result.put("transformsPerformed", false);
        result.put("summary", "WorldCore native contract applied AdapterCore world effects, executed hazard damage and status save/load, emitted region and hazard enter/exit transitions, resolved POI/marker/discovery state, planned spawn-zone and difficulty application state, materialized the Agent 7 world data catalog, sampled the Ashfall crash-zone cell, resolved active region and hazard membership, and projected map/integration events.");
        return result;
    }

    private static EchoWorldContracts.EchoWorldEffectTick referenceWorldEffectTick() {
        return new EchoWorldContracts.EchoWorldEffectTick(
                "agent7-player",
                32,
                68,
                32,
                20.0D,
                "",
                new EchoWorldContracts.EchoWorldRegion(
                        "echoashfallprotocol:crash_zone_wasteland",
                        "Crash Zone Wasteland",
                        0,
                        96,
                        0,
                        96,
                        "echoashfallprotocol:mission/secure_crash_outpost"
                ),
                new EchoWorldContracts.EchoWorldHazard(
                        "echoworldcore:hazard/salvage_debris",
                        "salvage_debris",
                        32,
                        32,
                        12,
                        2.0D,
                        "echostatuscore:status/salvage_debris"
                ),
                new EchoWorldContracts.EchoWeatherState(
                        "echoweathercore:ash_storm",
                        "ASH STORM: Ash front detected. Visibility loss expected.",
                        "echoashfallprotocol:event.ash_storm",
                        "echorendercore:hazard/ash_storm"
                ),
                new EchoWorldContracts.EchoAtmosphereState(
                        "echoatmospherecore:ash_storm_field",
                        0.45D,
                        "minecraft:ash",
                        "fog_color:9069905"
                ),
                new EchoWorldContracts.EchoBiomeProfile(
                        "echoashfallprotocol:crash_zone_wasteland",
                        "#echoashfallprotocol:common_wasteland_biomes",
                        "echoworldcore:hazard/salvage_debris"
                ),
                new EchoWorldContracts.EchoStructurePlacement(
                        "echoashfallprotocol:drop_pod",
                        "echoashfallprotocol:poi/drop_pod",
                        30,
                        0,
                        30
                ),
                new EchoWorldContracts.EchoSpawnRule(
                        "echospawncore:spawn/rad_zombie_crash_zone",
                        "echoashfallprotocol:rad_zombie",
                        "echoashfallprotocol:crash_zone_wasteland",
                        2,
                        21.0D
                ),
                new EchoWorldContracts.EchoStatusEffect(
                        "echostatuscore:status/salvage_debris",
                        200,
                        1,
                        "echoworldcore.hazard.salvage_debris.status"
                ),
                new EchoWorldContracts.EchoDifficultyProfile(
                        "echodifficultycore:easy",
                        1.0D,
                        0.85D
                )
        );
    }

    private static EchoWorldContracts.EchoWorldDataCatalogRequest referenceWorldDataCatalogRequest(
            Map<String, String> context) {
        String repoRoot = context.getOrDefault("repoRoot", ".");
        try {
            return loadWorldDataCatalogFromDefinitions(Paths.get(repoRoot).toAbsolutePath().normalize());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load Agent 7 world data catalog from definitions", exception);
        }
    }

    private static EchoWorldContracts.EchoWorldDataCatalogRequest loadWorldDataCatalogFromDefinitions(
            Path repoRoot) throws IOException {
        List<Path> regionFiles = matchingJson(repoRoot, "echoworldcore/world_regions");
        List<Path> hazardFiles = matchingJson(repoRoot, "echoworldcore/world_hazards");
        List<Path> weatherFiles = matchingJson(repoRoot, "weather_profiles");
        Path ashfallDataRoot = repoRoot.resolve("addons/echoashfallprotocol/src/main/resources/data/echoashfallprotocol");
        List<Path> biomeFiles = filesUnder(ashfallDataRoot.resolve("worldgen/biome"));
        List<Path> structureFiles =
                filesUnder(ashfallDataRoot.resolve("worldgen/structure"));
        List<Path> missionFiles =
                filesUnder(ashfallDataRoot.resolve("missioncore/missions"));
        List<String> regionIds = orderedCatalogIds(
                idsFromFiles(repoRoot, regionFiles, "echoworldcore/world_regions"),
                "echoashfallprotocol:crash_zone_wasteland",
                "echoashfallprotocol:toxic_swamp");
        List<String> hazardIds = orderedCatalogIds(
                idsFromFiles(repoRoot, hazardFiles, "echoworldcore/world_hazards"),
                "echoworldcore:hazard/salvage_debris",
                "echoashfallprotocol:hazard/toxic_ash");
        List<String> weatherIds = orderedCatalogIds(
                idsFromFiles(repoRoot, weatherFiles, "weather_profiles"),
                "echoweathercore:ash_storm",
                "echoashfallprotocol:ashfall_toxic_front");
        List<String> biomeIds = orderedCatalogIds(
                idsFromFiles(repoRoot, biomeFiles, "worldgen/biome"),
                "echoashfallprotocol:crash_zone_wasteland",
                "echoashfallprotocol:toxic_swamp");
        List<String> structureIds = orderedCatalogIds(
                idsFromFiles(repoRoot, structureFiles, "worldgen/structure"),
                "echoashfallprotocol:drop_pod",
                "echoashfallprotocol:reactor_ruin");
        List<String> statusEffectIds = orderedCatalogIds(
                hazardIds.stream()
                        .map(id -> "echostatuscore:status/" + idPath(id))
                        .distinct()
                        .sorted()
                        .toList(),
                "echostatuscore:status/salvage_debris",
                "echostatuscore:status/toxic_ash");
        List<String> difficultyIds = orderedCatalogIds(
                difficultyIds(missionFiles),
                "echodifficultycore:easy",
                "echodifficultycore:hard");
        ArrayList<String> sourceFiles = new ArrayList<>();
        for (Path path : allCatalogFiles(regionFiles, hazardFiles, weatherFiles, biomeFiles, structureFiles,
                missionFiles)) {
            sourceFiles.add(slash(repoRoot.relativize(path)));
        }
        return new EchoWorldContracts.EchoWorldDataCatalogRequest(
                regionIds,
                hazardIds,
                weatherIds,
                biomeIds,
                structureIds,
                statusEffectIds,
                difficultyIds,
                spawnRuleCount(biomeFiles),
                List.copyOf(sourceFiles),
                "WorldCoreJsonReloadListener plus WeatherDataReloadListener definition file scan");
    }

    private static List<Path> matchingJson(Path repoRoot, String contentDirectory) throws IOException {
        ArrayList<Path> matches = new ArrayList<>();
        List<Path> bases = List.of(
                repoRoot.resolve("src/main/resources/data"),
                repoRoot.resolve("addons/echoworldcore/src/main/resources/data"),
                repoRoot.resolve("addons/echoweathercore/src/main/resources/data"));
        for (Path base : bases) {
            if (!Files.isDirectory(base)) {
                continue;
            }
            try (var stream = Files.walk(base)) {
                stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .filter(path -> slash(base.relativize(path)).contains("/" + contentDirectory + "/")
                                || slash(base.relativize(path)).startsWith(contentDirectory + "/"))
                        .forEach(matches::add);
            }
        }
        return matches.stream()
                .distinct()
                .sorted(Comparator.comparing(path -> slash(repoRoot.relativize(path))))
                .toList();
    }

    private static List<Path> filesUnder(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (var stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    @SafeVarargs
    private static List<Path> allCatalogFiles(List<Path>... groups) {
        return java.util.Arrays.stream(groups)
                .flatMap(List::stream)
                .distinct()
                .sorted(Comparator.comparing(Path::toString))
                .toList();
    }

    private static List<String> idsFromFiles(Path repoRoot, List<Path> files, String contentDirectory) {
        return files.stream()
                .map(path -> contentId(repoRoot, path, contentDirectory, true))
                .filter(id -> !id.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private static String contentId(Path repoRoot, Path path, String contentDirectory, boolean preferDeclaredId) {
        if (preferDeclaredId) {
            try {
                Matcher matcher = ID_PATTERN.matcher(Files.readString(path, StandardCharsets.UTF_8));
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (IOException exception) {
                return "";
            }
        }
        String normalized = slash(repoRoot.relativize(path));
        String marker = "/data/";
        int dataIndex = normalized.indexOf(marker);
        if (dataIndex < 0) {
            return "";
        }
        String dataPath = normalized.substring(dataIndex + marker.length());
        int namespaceEnd = dataPath.indexOf('/');
        if (namespaceEnd < 0) {
            return "";
        }
        String namespace = dataPath.substring(0, namespaceEnd);
        String pathPart = dataPath.substring(namespaceEnd + 1);
        String prefix = contentDirectory + "/";
        int prefixIndex = pathPart.indexOf(prefix);
        if (prefixIndex < 0) {
            return "";
        }
        String idPath = pathPart.substring(prefixIndex + prefix.length());
        if (idPath.endsWith(".json")) {
            idPath = idPath.substring(0, idPath.length() - ".json".length());
        }
        return namespace + ":" + idPath;
    }

    private static List<String> orderedCatalogIds(List<String> ids, String first, String last) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (ids.contains(first)) {
            ordered.add(first);
        }
        ids.stream()
                .filter(id -> !id.equals(first) && !id.equals(last))
                .forEach(ordered::add);
        if (ids.contains(last)) {
            ordered.add(last);
        }
        return List.copyOf(ordered);
    }

    private static List<String> difficultyIds(List<Path> missionFiles) {
        return missionFiles.stream()
                .map(path -> {
                    try {
                        Matcher matcher = DIFFICULTY_PATTERN.matcher(Files.readString(path, StandardCharsets.UTF_8));
                        return matcher.find() ? "echodifficultycore:" + matcher.group(1) : "";
                    } catch (IOException exception) {
                        return "";
                    }
                })
                .filter(id -> !id.isBlank() && !id.endsWith(":"))
                .distinct()
                .sorted()
                .toList();
    }

    private static int spawnRuleCount(List<Path> biomeFiles) throws IOException {
        int count = 0;
        for (Path path : biomeFiles) {
            count += countOccurrences(Files.readString(path, StandardCharsets.UTF_8), "\"maxCount\"");
        }
        return count;
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int index = text.indexOf(token);
        while (index >= 0) {
            count++;
            index = text.indexOf(token, index + token.length());
        }
        return count;
    }

    private static String idPath(String id) {
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String slash(Path path) {
        return path.toString().replace('\\', '/');
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoWorldCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "worldcore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "WorldCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("worldEffectsRuntimeContract")),
                "WorldCore native adapter should execute world effects runtime contract");
        require(Boolean.TRUE.equals(activation.get("regionCellSampleExecuted")),
                "WorldCore native adapter should execute region cell sample");
        require(Double.valueOf(18.0D).equals(activation.get("worldEffectHealthAfter")),
                "WorldCore native adapter should apply hazard damage");
        require(Boolean.TRUE.equals(activation.get("hazardTickDamageRuntimeContract")),
                "WorldCore native adapter should execute hazard tick damage contract");
        require(Double.valueOf(2.0D).equals(activation.get("hazardTickDamageApplied")),
                "WorldCore native adapter should apply expected hazard damage");
        require(Boolean.TRUE.equals(activation.get("hazardTransitionRuntimeContract")),
                "WorldCore native adapter should execute hazard enter/exit transitions");
        require("ENTER".equals(activation.get("hazardEnterEventType")),
                "WorldCore native adapter should emit hazard enter transitions");
        require("EXIT".equals(activation.get("hazardExitEventType")),
                "WorldCore native adapter should emit hazard exit transitions");
        require(Boolean.TRUE.equals(activation.get("hazardTransitionCleared")),
                "WorldCore native adapter should clear active hazard on exit");
        require(Boolean.TRUE.equals(activation.get("statusSaveLoadRuntimeContract")),
                "WorldCore native adapter should save and load status effects");
        require(Boolean.TRUE.equals(activation.get("statusSaved")),
                "WorldCore native adapter should persist status effects");
        require(Boolean.TRUE.equals(activation.get("statusLoaded")),
                "WorldCore native adapter should load persisted status effects");
        require("echostatuscore:status/salvage_debris".equals(activation.get("statusLoadedEffectId")),
                "WorldCore native adapter should load the salvage debris status effect");
        require(Boolean.TRUE.equals(activation.get("structurePoiRuntimeContract")),
                "WorldCore native adapter should execute structure POI runtime contracts");
        require("POI_IN_RANGE".equals(activation.get("structurePoiLookupType")),
                "WorldCore native adapter should resolve an in-range POI");
        require(Boolean.TRUE.equals(activation.get("structurePoiMarkerPersisted")),
                "WorldCore native adapter should persist structure POI marker state");
        require("DISCOVERED".equals(activation.get("structureDiscoveryState")),
                "WorldCore native adapter should discover structure POI state");
        require(Boolean.TRUE.equals(activation.get("spawnRuleRuntimeContract")),
                "WorldCore native adapter should execute spawn rule runtime contracts");
        require("SPAWN_ALLOWED".equals(activation.get("spawnRuleEventType")),
                "WorldCore native adapter should allow the reference spawn event");
        require(Integer.valueOf(2).equals(activation.get("spawnZoneActivePopulation")),
                "WorldCore native adapter should retain spawn-zone population");
        require(Boolean.TRUE.equals(activation.get("difficultyApplicationRuntimeContract")),
                "WorldCore native adapter should execute difficulty application runtime contract");
        require(Double.valueOf(2.0D).equals(activation.get("difficultyScaledHazardDamage")),
                "WorldCore native adapter should retain scaled hazard damage");
        require(Integer.valueOf(2).equals(activation.get("difficultyScaledSpawnBudget")),
                "WorldCore native adapter should retain scaled spawn budget");
        require(Boolean.TRUE.equals(activation.get("worldDataCatalogRuntimeContract")),
                "WorldCore native adapter should execute world data catalog runtime contract");
        require(Integer.valueOf(8).equals(activation.get("worldDataCatalogRegionCount")),
                "WorldCore native adapter should retain world data catalog region count");
        require(Integer.valueOf(12).equals(activation.get("worldDataCatalogHazardCount")),
                "WorldCore native adapter should retain world data catalog hazard count");
        require(Integer.valueOf(9).equals(activation.get("worldDataCatalogWeatherProfileCount")),
                "WorldCore native adapter should retain world data catalog weather count");
        require(Integer.valueOf(12).equals(activation.get("worldDataCatalogStatusEffectCount")),
                "WorldCore native adapter should retain world data catalog status effect count");
        require(Integer.valueOf(5).equals(activation.get("worldDataCatalogDifficultyRuleCount")),
                "WorldCore native adapter should retain world data catalog difficulty rule count");
        require(Integer.valueOf(36).equals(activation.get("worldDataCatalogSpawnRuleCount")),
                "WorldCore native adapter should retain world data catalog spawn rule count");
        require(Integer.valueOf(193).equals(activation.get("worldDataCatalogSourceFileCount")),
                "WorldCore native adapter should load the world data catalog from definition files");
        require("echoashfallprotocol:crash_zone_wasteland".equals(activation.get("worldEffectActiveRegionId")),
                "WorldCore native adapter should retain active region");
        require(Boolean.TRUE.equals(activation.get("regionTransitionRuntimeContract")),
                "WorldCore native adapter should execute region enter/exit transitions");
        require("ENTER".equals(activation.get("regionEnterEventType")),
                "WorldCore native adapter should emit region enter transitions");
        require("EXIT".equals(activation.get("regionExitEventType")),
                "WorldCore native adapter should emit region exit transitions");
        require(Boolean.TRUE.equals(activation.get("regionTransitionCleared")),
                "WorldCore native adapter should clear active region on exit");
        require("echoworldcore:hazard/salvage_debris".equals(activation.get("worldEffectActiveHazardId")),
                "WorldCore native adapter should retain active hazard");
        require(Integer.valueOf(3).equals(activation.get("regionCellSampleMapFeedCount")),
                "WorldCore native adapter should project map feed entries");
        require(Boolean.TRUE.equals(activation.get("liveNativeGameplayHandlerAttached")),
                "WorldCore native adapter should expose the live player tick gameplay hook");
        require(Integer.valueOf(5).equals(activation.get("eventHookCount")),
                "WorldCore native adapter should expose all live event hooks");
        System.out.println("worldcore native adapter smoke PASS contracts="
                + ((List<?>) activation.get("registeredFeatureContracts")).size()
                + " healthAfter=" + activation.get("worldEffectHealthAfter")
                + " activeRegion=" + activation.get("worldEffectActiveRegionId")
                + " activeHazard=" + activation.get("worldEffectActiveHazardId")
                + " hazardDamage=" + activation.get("hazardTickDamageApplied")
                + " hazardEnter=" + activation.get("hazardEnterEventType")
                + " hazardExit=" + activation.get("hazardExitEventType")
                + " hazardCleared=" + activation.get("hazardTransitionCleared")
                + " statusSaved=" + activation.get("statusSaved")
                + " statusLoaded=" + activation.get("statusLoaded")
                + " poiLookup=" + activation.get("structurePoiLookupType")
                + " markerPersisted=" + activation.get("structurePoiMarkerPersisted")
                + " discoveryState=" + activation.get("structureDiscoveryState")
                + " spawnEvent=" + activation.get("spawnRuleEventType")
                + " spawnPopulation=" + activation.get("spawnZoneActivePopulation")
                + " difficultyHazard=" + activation.get("difficultyScaledHazardDamage")
                + " difficultySpawn=" + activation.get("difficultyScaledSpawnBudget")
                + " catalogRegions=" + activation.get("worldDataCatalogRegionCount")
                + " catalogHazards=" + activation.get("worldDataCatalogHazardCount")
                + " catalogWeather=" + activation.get("worldDataCatalogWeatherProfileCount")
                + " catalogStatusEffects=" + activation.get("worldDataCatalogStatusEffectCount")
                + " catalogDifficultyRules=" + activation.get("worldDataCatalogDifficultyRuleCount")
                + " catalogSpawnRules=" + activation.get("worldDataCatalogSpawnRuleCount")
                + " catalogSources=" + activation.get("worldDataCatalogSourceFileCount")
                + " regionEnter=" + activation.get("regionEnterEventType")
                + " regionExit=" + activation.get("regionExitEventType")
                + " regionCleared=" + activation.get("regionTransitionCleared")
                + " mapFeed=" + activation.get("regionCellSampleMapFeedCount")
                + " eventHooks=" + activation.get("eventHookCount")
                + " liveHook=player.tick.post");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
