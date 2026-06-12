package com.knoxhack.echoworldcore.test;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.echoplatform.echocore.api.EchoWorldRuntimeBus;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoServiceRegistry;
import com.echoplatform.echocore.api.IMapMarker;
import com.echoplatform.echocore.api.NoOpWorldService;
import com.echoplatform.echocore.api.WorldDiscoverySource;
import com.echoplatform.echocore.api.WorldHazardDefinition;
import com.echoplatform.echocore.api.WorldMarker;
import com.echoplatform.echocore.api.WorldMarkerType;
import com.echoplatform.echocore.api.WorldRegionDefinition;
import com.echoplatform.echocore.api.WorldRegionInstance;
import com.echoplatform.echocore.api.WorldRegionType;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.knoxhack.echoworldcore.content.WorldCoreJsonReloadListener;
import com.knoxhack.echoworldcore.event.WorldCoreEvents;
import com.knoxhack.echoworldcore.integration.WorldCoreMapDataProvider;
import com.knoxhack.echoworldcore.registry.WorldCoreBuiltins;
import com.knoxhack.echoworldcore.service.WorldRegionService;
import com.knoxhack.echoworldcore.world.WorldRegionSavedData;
import com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoWorldCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_REGISTRY =
            TEST_FUNCTIONS.register("worldcore_registry", () -> ModGameTests::worldcoreRegistry);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_NOOP =
            TEST_FUNCTIONS.register("worldcore_noop", () -> ModGameTests::worldcoreNoop);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_JSON =
            TEST_FUNCTIONS.register("worldcore_json_definitions", () -> ModGameTests::worldcoreJsonDefinitions);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_DATA_OVERRIDES =
            TEST_FUNCTIONS.register("worldcore_data_overrides", () -> ModGameTests::worldcoreDataOverrides);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_SAVED_DATA =
            TEST_FUNCTIONS.register("worldcore_saved_data", () -> ModGameTests::worldcoreSavedData);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_RELOAD_DIAGNOSTICS =
            TEST_FUNCTIONS.register("worldcore_reload_diagnostics", () -> ModGameTests::worldcoreReloadDiagnostics);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_MARKER_IDEMPOTENCE =
            TEST_FUNCTIONS.register("worldcore_marker_idempotence", () -> ModGameTests::worldcoreMarkerIdempotence);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_MARKER_LOOKUP =
            TEST_FUNCTIONS.register("worldcore_marker_lookup", () -> ModGameTests::worldcoreMarkerLookup);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_VALIDATION_REPORT =
            TEST_FUNCTIONS.register("worldcore_validation_report", () -> ModGameTests::worldcoreValidationReport);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_CONTEXT_SNAPSHOT =
            TEST_FUNCTIONS.register("worldcore_context_snapshot", () -> ModGameTests::worldcoreContextSnapshot);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_RUNTIME_BUS =
            TEST_FUNCTIONS.register("worldcore_runtime_bus", () -> ModGameTests::worldcoreRuntimeBus);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_SCAN_TICK_BEHAVIOR =
            TEST_FUNCTIONS.register("worldcore_scan_tick_behavior", () -> ModGameTests::worldcoreScanTickBehavior);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_REGION_TRANSITION_EFFECTS =
            TEST_FUNCTIONS.register("worldcore_region_transition_effects", () -> ModGameTests::worldcoreRegionTransitionEffects);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_HAZARD_TICK_EFFECTS =
            TEST_FUNCTIONS.register("worldcore_hazard_tick_effects", () -> ModGameTests::worldcoreHazardTickEffects);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_SPAWN_RULE_EVENT =
            TEST_FUNCTIONS.register("worldcore_spawn_rule_event", () -> ModGameTests::worldcoreSpawnRuleEvent);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_STRUCTURE_POI_LOOKUP =
            TEST_FUNCTIONS.register("worldcore_structure_poi_lookup", () -> ModGameTests::worldcoreStructurePoiLookup);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_CELL_SAMPLE =
            TEST_FUNCTIONS.register("worldcore_cell_sample", () -> ModGameTests::worldcoreCellSample);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_TOXIC_SWAMP_SECOND_SLICE =
            TEST_FUNCTIONS.register("worldcore_toxic_swamp_second_slice", () -> ModGameTests::worldcoreToxicSwampSecondSlice);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_MAP_PROVIDER =
            TEST_FUNCTIONS.register("worldcore_map_provider", () -> ModGameTests::worldcoreMapProvider);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_HOLOMAP_ZONES =
            TEST_FUNCTIONS.register("worldcore_holomap_zones", () -> ModGameTests::worldcoreHoloMapZones);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_RELEASE_GUARDS =
            TEST_FUNCTIONS.register("worldcore_release_guards", () -> ModGameTests::worldcoreReleaseGuards);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_AGENT7_EXACT_HOOK_LEDGER =
            TEST_FUNCTIONS.register("worldcore_agent7_exact_hook_ledger",
                    () -> ModGameTests::worldcoreAgent7ExactHookLedger);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("worldcore"));
        register(event, environment, "worldcore_registry", WORLDCORE_REGISTRY.getId());
        register(event, environment, "worldcore_noop", WORLDCORE_NOOP.getId());
        register(event, environment, "worldcore_json_definitions", WORLDCORE_JSON.getId());
        register(event, environment, "worldcore_data_overrides", WORLDCORE_DATA_OVERRIDES.getId());
        register(event, environment, "worldcore_saved_data", WORLDCORE_SAVED_DATA.getId());
        register(event, environment, "worldcore_reload_diagnostics", WORLDCORE_RELOAD_DIAGNOSTICS.getId());
        register(event, environment, "worldcore_marker_idempotence", WORLDCORE_MARKER_IDEMPOTENCE.getId());
        register(event, environment, "worldcore_marker_lookup", WORLDCORE_MARKER_LOOKUP.getId());
        register(event, environment, "worldcore_validation_report", WORLDCORE_VALIDATION_REPORT.getId());
        register(event, environment, "worldcore_context_snapshot", WORLDCORE_CONTEXT_SNAPSHOT.getId());
        register(event, environment, "worldcore_runtime_bus", WORLDCORE_RUNTIME_BUS.getId());
        register(event, environment, "worldcore_scan_tick_behavior", WORLDCORE_SCAN_TICK_BEHAVIOR.getId());
        register(event, environment, "worldcore_region_transition_effects", WORLDCORE_REGION_TRANSITION_EFFECTS.getId());
        register(event, environment, "worldcore_hazard_tick_effects", WORLDCORE_HAZARD_TICK_EFFECTS.getId());
        register(event, environment, "worldcore_spawn_rule_event", WORLDCORE_SPAWN_RULE_EVENT.getId());
        register(event, environment, "worldcore_structure_poi_lookup", WORLDCORE_STRUCTURE_POI_LOOKUP.getId());
        register(event, environment, "worldcore_cell_sample", WORLDCORE_CELL_SAMPLE.getId());
        register(event, environment, "worldcore_toxic_swamp_second_slice", WORLDCORE_TOXIC_SWAMP_SECOND_SLICE.getId());
        register(event, environment, "worldcore_map_provider", WORLDCORE_MAP_PROVIDER.getId());
        register(event, environment, "worldcore_holomap_zones", WORLDCORE_HOLOMAP_ZONES.getId());
        register(event, environment, "worldcore_release_guards", WORLDCORE_RELEASE_GUARDS.getId());
        if (agent7HookRecordersAvailable()) {
            register(event, environment, "worldcore_agent7_exact_hook_ledger", WORLDCORE_AGENT7_EXACT_HOOK_LEDGER.getId());
        }
    }

    private static void worldcoreRegistry(GameTestHelper helper) {
        WorldRegionService service = new WorldRegionService();
        WorldCoreBuiltins.register(service);
        helper.assertTrue(service.regionDefinitions().isEmpty(),
                "Standalone WorldCore should not register chapter-owned region definitions");
        helper.assertTrue(service.hazardDefinitions().size() == 8,
                "WorldCore should register the shared hazard taxonomy");
        helper.assertTrue(service.validateMarkers(null).isEmpty(),
                "Standalone WorldCore shared hazards should validate");
        helper.succeed();
    }

    private static void worldcoreJsonDefinitions(GameTestHelper helper) {
        WorldHazardDefinition hazard = WorldCoreJsonReloadListener.parseHazardForTests(
                id("hazard/test"), JsonParser.parseString("""
                        {"displayName":"Test Hazard","summary":"Test summary","defaultSeverity":42,"ticking":true}
                        """).getAsJsonObject());
        helper.assertTrue(hazard.defaultSeverity() == 42, "JSON hazard parser should preserve severity");
        helper.assertTrue(hazard.ticking(), "JSON hazard parser should preserve ticking flag");
        WorldRegionDefinition region = WorldCoreJsonReloadListener.parseRegionForTests(
                id("test_region"), JsonParser.parseString("""
                        {
                          "type":"crash_zone",
                          "displayName":"Test Region",
                          "summary":"A test region.",
                          "biomeIds":["minecraft:plains"],
                          "hazardIds":["echoworldcore:hazard/test"],
                          "radius":64,
                          "sortOrder":7
                        }
                        """).getAsJsonObject());
        helper.assertTrue(region.type() == WorldRegionType.CRASH_ZONE, "JSON region parser should resolve region type");
        helper.assertTrue(region.biomeIds().contains(Identifier.withDefaultNamespace("plains")),
                "JSON region parser should preserve biome ids");
        helper.assertTrue(region.radius() == 64, "JSON region parser should preserve valid radius");
        try {
            WorldCoreJsonReloadListener.parseHazardForTests(id("hazard/bad"),
                    JsonParser.parseString("{\"defaultSeverity\":101}").getAsJsonObject());
            helper.fail("Invalid hazard severity should fail parsing");
        } catch (JsonParseException expected) {
        }
        try {
            WorldCoreJsonReloadListener.parseRegionForTests(id("bad_radius"),
                    JsonParser.parseString("{\"type\":\"crash_zone\",\"radius\":8}").getAsJsonObject());
            helper.fail("Invalid region radius should fail parsing");
        } catch (JsonParseException expected) {
        }
        try {
            WorldCoreJsonReloadListener.parseRegionForTests(id("bad_type"),
                    JsonParser.parseString("{\"type\":\"unknown_world_type\",\"radius\":64}").getAsJsonObject());
            helper.fail("Invalid region type should fail parsing");
        } catch (JsonParseException expected) {
        }
        helper.succeed();
    }

    private static void worldcoreDataOverrides(GameTestHelper helper) {
        WorldRegionService service = new WorldRegionService();
        Identifier hazardId = id("hazard/override");
        Identifier regionId = id("override_region");
        service.registerHazardDefinition(new WorldHazardDefinition(hazardId,
                "Bootstrap Hazard", "Bootstrap summary.", 5, false));
        service.registerRegionDefinition(new WorldRegionDefinition(
                regionId,
                WorldRegionType.RUINED_CITY,
                "Bootstrap Region",
                "Bootstrap region summary.",
                List.of(),
                List.of(),
                List.of(id("structure/override")),
                List.of(hazardId),
                regionId,
                48,
                id("region/bootstrap"),
                id("ambience/bootstrap"),
                1));
        service.replaceDataDefinitions(
                Map.of(hazardId, new WorldHazardDefinition(hazardId,
                        "Data Hazard", "Data summary.", 77, true)),
                Map.of(regionId, new WorldRegionDefinition(
                        regionId,
                        WorldRegionType.ANOMALY_ZONE,
                        "Data Region",
                        "Data region summary.",
                        List.of(),
                        List.of(),
                        List.of(id("structure/override")),
                        List.of(hazardId),
                        regionId,
                        96,
                        id("region/data"),
                        id("ambience/data"),
                        2)),
                List.of("Synthetic reload warning"));
        helper.assertTrue(service.dataHazardDefinitionCount() == 1,
                "Data hazard definitions should be counted separately from bootstrap definitions");
        helper.assertTrue(service.dataRegionDefinitionCount() == 1,
                "Data region definitions should be counted separately from bootstrap definitions");
        helper.assertTrue(service.hazardDefinition(hazardId).orElseThrow().defaultSeverity() == 77,
                "Data hazard definition should override bootstrap hazard definition");
        helper.assertTrue(service.regionDefinition(regionId).orElseThrow().type() == WorldRegionType.ANOMALY_ZONE,
                "Data region definition should override bootstrap region definition");
        helper.assertTrue(service.reloadWarnings().contains("Synthetic reload warning"),
                "Reload warnings should be retained for command diagnostics");
        helper.assertTrue(service.validateMarkers(null).contains("Synthetic reload warning"),
                "Validation should include retained reload warnings");
        helper.assertTrue(service.regionSourceCounts().getOrDefault(EchoWorldCore.MODID, 0) == 1,
                "Region source counts should summarize active definitions by namespace");
        helper.assertTrue(service.hazardSourceCounts().getOrDefault(EchoWorldCore.MODID, 0) == 1,
                "Hazard source counts should summarize active definitions by namespace");
        helper.succeed();
    }

    private static void worldcoreAgent7ExactHookLedger(GameTestHelper helper) {
        EchoNativeAgent7LiveHookEvidenceBridge.resetForTest();
        long gameTick = helper.getLevel().getGameTime();
        WorldCoreEvents.recordAgent7LiveHookForTests(gameTick);
        invokeAgent7TickHookRecorder("com.knoxhack.echoweathercore.event.WeatherCoreEvents", gameTick);
        invokeAgent7TickHookRecorder("com.knoxhack.echo.atmospherecore.EchoAtmosphereCoreEvents", gameTick);
        invokeAgent7TickHookRecorder("com.knoxhack.echo.biomecore.EchoBiomeCoreEvents", gameTick);
        invokeAgent7TickHookRecorder("com.knoxhack.echo.structurecore.EchoStructureCoreEvents", gameTick);
        invokeAgent7TickHookRecorder("com.knoxhack.echo.spawncore.EchoSpawnCoreEvents", gameTick);
        invokeAgent7ServerHookRecorder("com.knoxhack.echo.difficultycore.EchoDifficultyCoreEvents");
        invokeAgent7ServerHookRecorder("com.knoxhack.echo.statuscore.EchoStatusCoreEvents");

        Map<String, Object> snapshot = EchoNativeAgent7LiveHookEvidenceBridge.snapshot();
        helper.assertTrue(Integer.valueOf(8).equals(snapshot.get("requiredHookCount")),
                "Agent 7 exact hook ledger should track all owned hooks");
        helper.assertTrue(Integer.valueOf(8).equals(snapshot.get("verifiedHookCount")),
                "Agent 7 exact hook ledger should verify all owned hooks in the native host test");
        helper.assertTrue(Boolean.TRUE.equals(snapshot.get("allRequiredHooksVerified")),
                "Agent 7 exact hook ledger should mark all native-host test callbacks verified");
        Object hooks = snapshot.get("hooks");
        helper.assertTrue(hooks instanceof List<?> list && list.size() == 8,
                "Agent 7 exact hook ledger should retain eight hook rows");
        helper.assertTrue(Boolean.FALSE.equals(snapshot.get("directPersistenceConfigured")),
                "Agent 7 GameTest should not require direct sidecar persistence");
        helper.succeed();
    }

    private static boolean agent7HookRecordersAvailable() {
        return classAvailable("com.knoxhack.echoweathercore.event.WeatherCoreEvents")
                && classAvailable("com.knoxhack.echo.atmospherecore.EchoAtmosphereCoreEvents")
                && classAvailable("com.knoxhack.echo.biomecore.EchoBiomeCoreEvents")
                && classAvailable("com.knoxhack.echo.structurecore.EchoStructureCoreEvents")
                && classAvailable("com.knoxhack.echo.spawncore.EchoSpawnCoreEvents")
                && classAvailable("com.knoxhack.echo.difficultycore.EchoDifficultyCoreEvents")
                && classAvailable("com.knoxhack.echo.statuscore.EchoStatusCoreEvents");
    }

    private static boolean classAvailable(String className) {
        try {
            Class.forName(className, false, ModGameTests.class.getClassLoader());
            return true;
        } catch (LinkageError | ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void invokeAgent7TickHookRecorder(String className, long gameTick) {
        try {
            Class.forName(className)
                    .getMethod("recordAgent7LiveHookForTests", long.class)
                    .invoke(null, gameTick);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Agent 7 tick hook recorder unavailable: " + className, exception);
        }
    }

    private static void invokeAgent7ServerHookRecorder(String className) {
        try {
            Class.forName(className)
                    .getMethod("recordAgent7LiveHookForTests")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Agent 7 server hook recorder unavailable: " + className, exception);
        }
    }

    private static void worldcoreReloadDiagnostics(GameTestHelper helper) {
        WorldRegionService service = new WorldRegionService();
        List<String> warnings = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            warnings.add("Reload warning " + i);
        }
        service.replaceDataDefinitions(Map.of(), Map.of(), warnings);
        helper.assertTrue(service.reloadWarnings().size() == 65,
                "Reload warning diagnostics should be bounded with one omission marker");
        helper.assertTrue(service.reloadWarnings().get(64).contains("omitted"),
                "Bounded reload warnings should explain omitted diagnostics");
        service.replaceDataDefinitions(Map.of(), Map.of());
        helper.assertTrue(service.reloadWarnings().isEmpty(),
                "Reload warnings should reset cleanly on the next replacement");
        helper.succeed();
    }

    private static void worldcoreMarkerIdempotence(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        WorldRegionService service = new WorldRegionService();
        Identifier regionId = id("marker_idempotence_region");
        service.registerRegionDefinition(new WorldRegionDefinition(
                regionId,
                WorldRegionType.ANOMALY_ZONE,
                "Marker Idempotence Region",
                "Region used to verify repeated marker reveals do not duplicate events.",
                List.of(),
                List.of(),
                List.of(id("structure/marker_idempotence")),
                List.of(),
                regionId,
                64,
                id("region/marker_idempotence"),
                id("ambience/marker_idempotence"),
                997));
        AtomicInteger revealed = new AtomicInteger();
        EchoWorldRuntimeBus.onMarkerRevealed(event -> revealed.incrementAndGet());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        WorldMarker marker = new WorldMarker(
                id("marker/idempotence/" + player.getUUID()),
                regionId,
                WorldMarkerType.ANOMALY,
                "Idempotent Marker",
                "Repeated reveals should update canonical state once.",
                player.level().dimension(),
                player.blockPosition(),
                64,
                false,
                player.level().getGameTime());
        service.revealMarker(player, marker);
        service.revealMarker(player, marker);
        long stored = service.markers(player).stream()
                .filter(candidate -> candidate.id().equals(marker.id()))
                .count();
        helper.assertTrue(revealed.get() == 1,
                "Repeated marker reveals should not spam duplicate MarkerRevealed events");
        helper.assertTrue(stored == 1,
                "Repeated marker reveals should persist one canonical marker record");
        EchoWorldRuntimeBus.clearForTests();
        helper.succeed();
    }

    private static void worldcoreMarkerLookup(GameTestHelper helper) {
        WorldRegionService service = new WorldRegionService();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Identifier markerId = id("marker/dimension_lookup/" + player.getUUID());
        WorldMarker marker = new WorldMarker(
                markerId,
                id("lookup_region"),
                WorldMarkerType.ORBITAL_DEBRIS,
                "Dimension Lookup Marker",
                "Marker used to verify non-player marker queries.",
                Level.NETHER,
                player.blockPosition(),
                48,
                true,
                player.level().getGameTime());
        service.revealMarker(player.level(), marker);
        helper.assertTrue(service.markerById(player.level(), markerId).isPresent(),
                "WorldCore markerById should find persisted markers by id");
        helper.assertTrue(service.markers(player.level(), Level.NETHER).stream()
                        .anyMatch(candidate -> candidate.id().equals(markerId)),
                "WorldCore markers(level, dimension) should return markers for requested dimensions");
        helper.assertTrue(service.markers(player).stream().noneMatch(candidate -> candidate.id().equals(markerId)),
                "Player marker queries should remain scoped to the player's current dimension");
        helper.succeed();
    }

    private static void worldcoreValidationReport(GameTestHelper helper) {
        WorldRegionService service = new WorldRegionService();
        WorldCoreBuiltins.register(service);
        Identifier regionId = id("validation_report_region");
        Identifier missingHazard = id("hazard/missing_for_report");
        service.registerRegionDefinition(new WorldRegionDefinition(
                regionId,
                WorldRegionType.RADIATION_ZONE,
                "Validation Report Region",
                "Region intentionally references a missing hazard.",
                List.of(),
                List.of(),
                List.of(id("structure/validation_report")),
                List.of(missingHazard),
                regionId,
                64,
                id("region/validation_report"),
                id("ambience/validation_report"),
                997));
        var report = service.validationReport(null);
        helper.assertTrue(!report.valid(), "Validation report should fail when a region references a missing hazard");
        helper.assertTrue(report.errorCount() > 0, "Validation report should classify missing hazard refs as errors");
        helper.assertTrue(report.messages().stream().anyMatch(message -> message.contains(missingHazard.toString())),
                "Validation report should preserve old warning message text");
        helper.assertTrue(report.regionSourceCounts().getOrDefault(EchoWorldCore.MODID, 0) == 1,
                "Validation report should include active region source counts");
        helper.succeed();
    }

    private static void worldcoreContextSnapshot(GameTestHelper helper) {
        WorldRegionService service = new WorldRegionService();
        Identifier hazardId = id("hazard/context_snapshot");
        Identifier regionId = id("context_snapshot_region");
        service.registerHazardDefinition(new WorldHazardDefinition(hazardId,
                "Context Snapshot Hazard", "Hazard for context snapshot tests.", 66, false));
        service.registerRegionDefinition(new WorldRegionDefinition(
                regionId,
                WorldRegionType.ANOMALY_ZONE,
                "Context Snapshot Region",
                "Marker-backed region for context snapshot tests.",
                List.of(),
                List.of(),
                List.of(id("structure/context_snapshot")),
                List.of(hazardId),
                regionId,
                64,
                id("region/context_snapshot"),
                id("ambience/context_snapshot"),
                996));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        service.revealMarker(player, new WorldMarker(
                id("marker/context_snapshot/" + player.getUUID()),
                regionId,
                WorldMarkerType.ANOMALY,
                "Context Snapshot Marker",
                "Marker-backed region for context snapshots.",
                player.level().dimension(),
                player.blockPosition(),
                64,
                true,
                player.level().getGameTime()));
        service.tickPlayer(player);
        var snapshot = service.worldContext(player);
        helper.assertTrue(snapshot.currentRegionOptional().isPresent(),
                "World context snapshot should expose the active region");
        helper.assertTrue(!snapshot.nearbyMarkers().isEmpty(),
                "World context snapshot should expose marker context");
        helper.assertTrue(snapshot.hazard().hazardIds().contains(hazardId),
                "World context snapshot should expose hazard context");
        helper.assertTrue(snapshot.discoveredRegionIds().contains(regionId),
                "World context snapshot should expose discovery context");
        helper.succeed();
    }

    private static void worldcoreSavedData(GameTestHelper helper) {
        WorldRegionSavedData data = new WorldRegionSavedData();
        Identifier regionId = id("test_region");
        Identifier markerId = id("marker/test");
        Identifier hazardId = id("hazard/saved_data");
        UUID playerId = UUID.randomUUID();
        data.saveMarker(new WorldMarker(markerId, regionId, WorldMarkerType.CRASH_SITE,
                "Test Marker", "Round trip marker.", Level.OVERWORLD, BlockPos.ZERO, 32, true, 12L));
        data.recordDiscovery(playerId, regionId, WorldDiscoverySource.SCAN, new BlockPos(1, 2, 3), 99L);
        data.recordHazardExposure(playerId, hazardId, id("status/saved_data"),
                "echoworldcore.hazard.saved_data.status", 200, 2, 1.25F, 100L);
        helper.assertTrue(data.markers().size() == 1, "SavedData should retain markers");
        helper.assertTrue(data.discoveries(playerId).contains(regionId), "SavedData should retain structured discoveries");
        helper.assertTrue(data.hazardExposures(playerId).contains(hazardId),
                "SavedData should retain hazard exposure state");
        helper.assertTrue(data.hazardExposureSaveKey(playerId, hazardId).equals("echoworldcore.hazard.saved_data.status"),
                "SavedData should expose the persisted hazard status save key");
        helper.assertTrue(data.hazardExposureStatusState(playerId, hazardId)
                        .containsKey("echoworldcore.hazard.saved_data.status"),
                "SavedData should reconstruct the persisted status payload for load");
        var encoded = WorldRegionSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();
        WorldRegionSavedData decoded = WorldRegionSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        helper.assertTrue(decoded.markers().size() == 1, "SavedData codec should round-trip markers");
        helper.assertTrue(decoded.discoveries(playerId).contains(regionId), "SavedData codec should round-trip discoveries");
        helper.assertTrue(decoded.hazardExposures(playerId).contains(hazardId),
                "SavedData codec should round-trip hazard exposure state");
        helper.assertTrue(decoded.hazardExposureStatusState(playerId, hazardId)
                        .containsKey("echoworldcore.hazard.saved_data.status"),
                "SavedData codec should round-trip status payload fields needed for load");
        helper.succeed();
    }

    private static void worldcoreRuntimeBus(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        AtomicInteger entered = new AtomicInteger();
        AtomicInteger exited = new AtomicInteger();
        AtomicInteger discovered = new AtomicInteger();
        AtomicInteger scanned = new AtomicInteger();
        AtomicInteger marker = new AtomicInteger();
        AtomicInteger hazard = new AtomicInteger();
        EchoWorldRuntimeBus.onRegionEntered(event -> entered.incrementAndGet());
        EchoWorldRuntimeBus.onRegionExited(event -> exited.incrementAndGet());
        EchoWorldRuntimeBus.onRegionDiscovered(event -> discovered.incrementAndGet());
        EchoWorldRuntimeBus.onRegionScanned(event -> scanned.incrementAndGet());
        EchoWorldRuntimeBus.onMarkerRevealed(event -> marker.incrementAndGet());
        EchoWorldRuntimeBus.onHazardChanged(event -> hazard.incrementAndGet());
        Identifier regionId = id("test_region");
        WorldRegionInstance region = new WorldRegionInstance(id("instance/test"), regionId,
                WorldRegionType.ORBITAL_DEBRIS_FIELD, "Test Region", Level.OVERWORLD, BlockPos.ZERO, 32, java.util.List.of(), true);
        WorldMarker worldMarker = new WorldMarker(id("marker/runtime"), regionId,
                WorldMarkerType.REGION_CENTER, "Runtime Marker", "", Level.OVERWORLD, BlockPos.ZERO, 32, true, 1L);
        EchoWorldRuntimeBus.fireRegionEntered(new EchoWorldRuntimeBus.RegionEntered(null, region));
        EchoWorldRuntimeBus.fireRegionExited(new EchoWorldRuntimeBus.RegionExited(null, region));
        EchoWorldRuntimeBus.fireRegionDiscovered(new EchoWorldRuntimeBus.RegionDiscovered(
                null, region, WorldDiscoverySource.DEBUG, true));
        EchoWorldRuntimeBus.fireRegionScanned(new EchoWorldRuntimeBus.RegionScanned(null, region, worldMarker));
        EchoWorldRuntimeBus.fireMarkerRevealed(new EchoWorldRuntimeBus.MarkerRevealed(null, worldMarker));
        EchoWorldRuntimeBus.fireHazardChanged(new EchoWorldRuntimeBus.HazardChanged(
                null, com.echoplatform.echocore.api.WorldHazardSnapshot.nominal(),
                new com.echoplatform.echocore.api.WorldHazardSnapshot(java.util.List.of(regionId),
                        java.util.List.of(id("hazard/test")), 25, false, "test")));
        helper.assertTrue(entered.get() == 1 && exited.get() == 1 && discovered.get() == 1 && scanned.get() == 1
                && marker.get() == 1 && hazard.get() == 1, "Runtime bus should deliver each world event");
        EchoWorldRuntimeBus.clearForTests();
        helper.succeed();
    }

    private static void worldcoreScanTickBehavior(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        WorldRegionService service = new WorldRegionService();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Identifier hazardId = id("hazard/scan_tick");
        Identifier regionId = id("scan_tick_region/" + player.getUUID() + "/" + helper.getLevel().getGameTime());
        service.registerHazardDefinition(new WorldHazardDefinition(hazardId,
                "Scan Tick Hazard", "Hazard used by repeated scan tick tests.", 40, false));
        service.registerRegionDefinition(new WorldRegionDefinition(
                regionId,
                WorldRegionType.ANOMALY_ZONE,
                "Scan Tick Region",
                "Marker-backed region used by repeated scan tick tests.",
                List.of(),
                List.of(),
                List.of(id("structure/scan_tick")),
                List.of(hazardId),
                regionId,
                64,
                id("region/scan_tick"),
                id("ambience/scan_tick"),
                998));
        AtomicInteger entered = new AtomicInteger();
        AtomicInteger exited = new AtomicInteger();
        AtomicInteger discovered = new AtomicInteger();
        AtomicInteger enterDiscovered = new AtomicInteger();
        AtomicInteger hazard = new AtomicInteger();
        EchoWorldRuntimeBus.onRegionEntered(event -> entered.incrementAndGet());
        EchoWorldRuntimeBus.onRegionExited(event -> exited.incrementAndGet());
        EchoWorldRuntimeBus.onRegionDiscovered(event -> {
            if (event.region().definitionId().equals(regionId)) {
                discovered.incrementAndGet();
                if (event.source() == WorldDiscoverySource.ENTER) {
                    enterDiscovered.incrementAndGet();
                }
            }
        });
        EchoWorldRuntimeBus.onHazardChanged(event -> hazard.incrementAndGet());
        BlockPos pos = player.blockPosition();
        service.revealMarker(player.level(), new WorldMarker(
                id("marker/scan_tick/" + player.getUUID()),
                regionId,
                WorldMarkerType.ANOMALY,
                "Scan Tick Marker",
                "Marker-backed region for repeated scan tick tests.",
                player.level().dimension(),
                pos,
                64,
                true,
                player.level().getGameTime()));
        service.tickPlayer(player);
        service.tickPlayer(player);
        helper.assertTrue(entered.get() == 1,
                "Repeated scan ticks should only fire RegionEntered when the primary region changes");
        helper.assertTrue(enterDiscovered.get() <= 1,
                "Repeated scan ticks should not spam duplicate RegionDiscovered events for ENTER discoveries");
        helper.assertTrue(hazard.get() == 1,
                "First non-safe hazard snapshot should emit one HazardChanged event from nominal state");
        EchoWorldRuntimeBus.clearForTests();
        helper.succeed();
    }

    private static void worldcoreRegionTransitionEffects(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> worldcoreRegionTransitionEffectsIsolated(helper));
    }

    private static void worldcoreRegionTransitionEffectsIsolated(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        WorldRegionService service = new WorldRegionService();
        Identifier hazardId = id("hazard/transition_tick");
        Identifier regionId = id("transition_tick_region");
        Identifier missionId = id("mission/transition_tick");
        AtomicInteger missionsStarted = new AtomicInteger();
        EchoCoreServices.registerMissionService(missionServiceForRegion(missionId, regionId, "hard", missionsStarted));
        service.registerHazardDefinition(new WorldHazardDefinition(hazardId,
                "Transition Tick Hazard", "Hazard used by region transition tests.", 50, true));
        service.registerRegionDefinition(new WorldRegionDefinition(
                regionId,
                WorldRegionType.ANOMALY_ZONE,
                "Transition Tick Region",
                "Marker-backed region used by enter/exit transition tests.",
                List.of(),
                List.of(),
                List.of(id("structure/transition_tick")),
                List.of(hazardId),
                regionId,
                64,
                id("region/transition_tick"),
                id("ambience/transition_tick"),
                996));
        AtomicInteger entered = new AtomicInteger();
        AtomicInteger exited = new AtomicInteger();
        EchoWorldRuntimeBus.onRegionEntered(event -> entered.incrementAndGet());
        EchoWorldRuntimeBus.onRegionExited(event -> exited.incrementAndGet());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos pos = player.blockPosition();
        service.revealMarker(player.level(), new WorldMarker(
                id("marker/transition_tick/" + player.getUUID()),
                regionId,
                WorldMarkerType.ANOMALY,
                "Transition Tick Marker",
                "Marker-backed region for transition tests.",
                player.level().dimension(),
                pos,
                64,
                true,
                player.level().getGameTime()));
        service.tickPlayer(player);
        var enteredTransition = service.lastRegionTransition(player).orElseThrow();
        helper.assertTrue(entered.get() == 1 && enteredTransition.regionEntered(),
                "Native WorldCore transition should fire RegionEntered on first active region");
        helper.assertTrue(regionId.toString().equals(enteredTransition.currentRegionId()),
                "Native WorldCore transition should report the current region id");
        helper.assertTrue(enteredTransition.missionEvents().contains(missionId.toString()),
                "Native WorldCore transition should carry the mission id bound to the entered region");
        helper.assertTrue(missionsStarted.get() == 1,
                "Native WorldCore transition should start the mission bound to the entered region");
        helper.assertTrue(service.activeRegionId(player).equals(regionId.toString())
                        && service.startedRegionMissions(player).contains(missionId.toString()),
                "Native WorldCore transition should retain active region and started mission state");
        var spawnResult = service.lastSpawnRuleEvent(player).orElseThrow();
        helper.assertTrue(spawnResult.difficultyId().equals("echodifficultycore:hard")
                        && spawnResult.spawnMultiplier() == 1.25D
                        && spawnResult.scaledBudget() == 3,
                "Native WorldCore spawn rule event should use the region mission difficulty profile");
        helper.assertTrue(service.activeDifficultyProfile(player).orElseThrow().id().equals("echodifficultycore:hard")
                        && service.regionDifficultyProfile(regionId).orElseThrow().spawnMultiplier() == 1.25D,
                "Native WorldCore region tick should retain active mission difficulty modifiers");
        var difficultySelection = service.regionDifficultyProfileSelection(regionId).orElseThrow();
        helper.assertTrue(difficultySelection.difficultyId().equals("echodifficultycore:hard")
                        && difficultySelection.selectedDifficulty().equals("hard")
                        && difficultySelection.hazardMultiplier() == 1.5D
                        && difficultySelection.spawnMultiplier() == 1.25D
                        && difficultySelection.selected(),
                "Native WorldCore region tick should materialize AdapterCore DifficultyCore profile selection");
        var damageResult = service.lastHazardTickDamage(player).orElseThrow();
        helper.assertTrue(damageResult.difficultyId().equals("echodifficultycore:hard")
                        && damageResult.hazardMultiplier() == 1.5D
                        && damageResult.damageApplied() == 1.5D,
                "Native WorldCore hazard tick damage should use the region mission difficulty profile");
        var difficultyState = service.activeDifficultyApplicationState(player).orElseThrow();
        helper.assertTrue(difficultyState.get("difficultyId").equals("echodifficultycore:hard")
                        && difficultyState.get("appliedHazardId").equals(damageResult.hazardId())
                        && difficultyState.get("scaledHazardDamage").equals(damageResult.damageApplied())
                        && difficultyState.get("appliedSpawnRuleId").equals(spawnResult.ruleId())
                        && difficultyState.get("scaledSpawnBudget").equals(spawnResult.scaledBudget()),
                "Native WorldCore region tick should retain applied difficulty hazard and spawn modifier state");
        var adapterDifficultyResult = service.activeDifficultyApplicationResult(player).orElseThrow();
        helper.assertTrue(adapterDifficultyResult.difficultyId().equals("echodifficultycore:hard")
                        && adapterDifficultyResult.appliedHazardId().equals(damageResult.hazardId())
                        && adapterDifficultyResult.scaledHazardDamage() == damageResult.damageApplied()
                        && adapterDifficultyResult.appliedSpawnRuleId().equals(spawnResult.ruleId())
                        && adapterDifficultyResult.scaledSpawnBudget() == spawnResult.scaledBudget()
                        && adapterDifficultyResult.applied(),
                "Native WorldCore region tick should materialize AdapterCore difficulty application state");
        player.setPos(pos.getX() + 10000.5D, pos.getY(), pos.getZ() + 10000.5D);
        service.tickPlayer(player);
        var exitedTransition = service.lastRegionTransition(player).orElseThrow();
        helper.assertTrue(exited.get() == 1 && exitedTransition.regionExited(),
                "Native WorldCore transition should fire RegionExited after leaving the active region");
        helper.assertTrue(regionId.toString().equals(exitedTransition.previousRegionId())
                        && exitedTransition.currentRegionId().isBlank(),
                "Native WorldCore transition should preserve previous region and clear current region on exit");
        helper.assertTrue(service.activeRegionId(player).isBlank(),
                "Native WorldCore transition should clear retained active region state on exit");
        EchoWorldRuntimeBus.clearForTests();
        helper.succeed();
    }

    private static void worldcoreHazardTickEffects(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        WorldRegionService service = new WorldRegionService();
        AtomicInteger statusSaved = new AtomicInteger();
        AtomicInteger statusLoaded = new AtomicInteger();
        EchoWorldRuntimeBus.onStatusEffectSaved(event -> statusSaved.incrementAndGet());
        EchoWorldRuntimeBus.onStatusEffectLoaded(event -> statusLoaded.incrementAndGet());
        Identifier hazardId = id("hazard/live_tick");
        Identifier regionId = id("live_tick_region");
        service.registerHazardDefinition(new WorldHazardDefinition(hazardId,
                "Live Tick Hazard", "Ticking hazard used by live native effect tests.", 50, true));
        service.registerRegionDefinition(new WorldRegionDefinition(
                regionId,
                WorldRegionType.ANOMALY_ZONE,
                "Live Tick Region",
                "Marker-backed region used by live hazard tick tests.",
                List.of(),
                List.of(),
                List.of(id("structure/live_tick")),
                List.of(hazardId),
                regionId,
                64,
                id("region/live_tick"),
                id("ambience/live_tick"),
                997));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setHealth(20.0F);
        BlockPos pos = player.blockPosition();
        service.revealMarker(player.level(), new WorldMarker(
                id("marker/live_tick/" + player.getUUID()),
                regionId,
                WorldMarkerType.ANOMALY,
                "Live Tick Marker",
                "Marker-backed region for live hazard tick tests.",
                player.level().dimension(),
                pos,
                64,
                true,
                player.level().getGameTime()));
        service.tickPlayer(player);
        var result = service.lastHazardTick(player).orElseThrow();
        var damageResult = service.lastHazardTickDamage(player).orElseThrow();
        var statusResult = service.lastStatusEffectSave(player).orElseThrow();
        var savedStatusApplication = service.lastStatusEffectApplication(player).orElseThrow();
        helper.assertTrue(result.hazardId().equals(hazardId),
                "Native WorldCore hazard tick should apply the active ticking hazard");
        helper.assertTrue(damageResult.hazardId().equals(hazardId.toString())
                        && damageResult.damageApplied() == result.damageApplied(),
                "Native WorldCore hazard tick should route damage through AdapterCore");
        helper.assertTrue(damageResult.difficultyId().equals("echodifficultycore:normal")
                        && damageResult.hazardMultiplier() == 1.0D,
                "Native WorldCore hazard tick should preserve the difficulty profile used for damage scaling");
        var difficultyState = service.activeDifficultyApplicationState(player).orElseThrow();
        helper.assertTrue(difficultyState.get("appliedHazardId").equals(damageResult.hazardId())
                        && difficultyState.get("baseHazardDamage").equals(damageResult.baseDamage())
                        && difficultyState.get("scaledHazardDamage").equals(damageResult.damageApplied())
                        && difficultyState.get("sourceReason").equals("WorldRegionService.applyHazardTickDamage"),
                "Native WorldCore hazard tick should retain applied difficulty damage state");
        helper.assertTrue(result.damageApplied() >= 1.0F,
                "Native WorldCore hazard tick should apply severity-scaled damage");
        helper.assertTrue(result.healthAfter() < result.healthBefore(),
                "Native WorldCore hazard tick should mutate player health");
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),
                "Native WorldCore hazard tick should apply a status effect");
        helper.assertTrue(statusSaved.get() == 1 && statusResult.saved(),
                "Native WorldCore hazard tick should persist the status effect through AdapterCore");
        helper.assertTrue(service.activeStatusEffects(player).contains(statusResult.effectId()),
                "Native WorldCore hazard tick should retain active status effect state");
        helper.assertTrue(savedStatusApplication.applied()
                        && !savedStatusApplication.loaded()
                        && savedStatusApplication.effectId().equals(statusResult.effectId())
                        && savedStatusApplication.expiresAtTick() == statusResult.gameTick() + statusResult.durationTicks()
                        && savedStatusApplication.activeStatusState().get("moduleId").equals(EchoWorldCore.MODID),
                "Native WorldCore hazard tick should route saved status through AdapterCore status apply");
        var savedRuntimeState = service.activeStatusEffectState(player, statusResult.effectId()).orElseThrow();
        helper.assertTrue(savedRuntimeState.get("durationTicks").equals(statusResult.durationTicks())
                        && savedRuntimeState.get("amplifier").equals(statusResult.amplifier())
                        && savedRuntimeState.get("expiresAtTick").equals(statusResult.gameTick() + statusResult.durationTicks()),
                "Native WorldCore hazard tick should retain timed status duration/amplifier runtime state");
        var savedProfileState = service.activeStatusProfileState(player, statusResult.effectId()).orElseThrow();
        helper.assertTrue(savedProfileState.get("statusKind").equals("ENVIRONMENTAL_HAZARD")
                        && savedProfileState.get("severity").equals("MEDIUM")
                        && savedProfileState.get("stackingPolicy").equals("REFRESH_DURATION")
                        && Boolean.TRUE.equals(savedProfileState.get("persisted"))
                        && Boolean.FALSE.equals(savedProfileState.get("loaded")),
                "Native WorldCore hazard tick should retain StatusCore-style status profile and exposure state");
        long refreshedGameTick = statusResult.gameTick() + 2L;
        var refreshedStatusResult = service.persistStatusEffect(
                player,
                hazardId,
                new EchoWorldContracts.EchoStatusEffect(
                        statusResult.effectId(),
                        statusResult.durationTicks(),
                        statusResult.amplifier() + 1,
                        statusResult.saveKey()),
                statusResult.damageApplied() + 1.0F,
                refreshedGameTick).orElseThrow();
        var statusStacking = service.lastStatusEffectStacking(player, statusResult.effectId()).orElseThrow();
        var stackedRuntimeState = service.activeStatusEffectState(player, statusResult.effectId()).orElseThrow();
        helper.assertTrue(statusStacking.hadPrevious()
                        && statusStacking.refreshed()
                        && statusStacking.amplifierUpgraded()
                        && !statusStacking.stacked()
                        && statusStacking.durationTicks() == statusResult.durationTicks()
                        && statusStacking.amplifier() == statusResult.amplifier() + 1
                        && statusStacking.appliedGameTick() == refreshedGameTick
                        && statusStacking.expiresAtTick() == refreshedGameTick + statusResult.durationTicks()
                        && service.activeStatusEffects(player).size() == 1,
                "Native WorldCore status stacking should refresh duration and upgrade amplifier through AdapterCore");
        helper.assertTrue(stackedRuntimeState.get("durationTicks").equals(statusStacking.durationTicks())
                        && stackedRuntimeState.get("amplifier").equals(statusStacking.amplifier())
                        && stackedRuntimeState.get("expiresAtTick").equals(statusStacking.expiresAtTick()),
                "Native WorldCore status stacking should update the live active status state");
        helper.assertTrue(statusResult.effectId().equals(result.statusEffectId().toString())
                        && statusResult.saveKey().equals("echoworldcore.hazard.hazard_live_tick.status"),
                "Native WorldCore status save should preserve effect id and save key");
        var loadResult = service.loadStatusEffect(player, hazardId, player.level().getGameTime() + 1L).orElseThrow();
        var loadedStatusApplication = service.lastStatusEffectApplication(player).orElseThrow();
        helper.assertTrue(statusLoaded.get() == 1 && loadResult.loaded(),
                "Native WorldCore status load should rehydrate the persisted status effect through AdapterCore");
        helper.assertTrue(loadResult.effectId().equals(statusResult.effectId())
                        && loadResult.durationTicks() == refreshedStatusResult.durationTicks()
                        && loadResult.amplifier() == refreshedStatusResult.amplifier(),
                "Native WorldCore status load should match the saved status effect payload");
        helper.assertTrue(service.lastStatusEffectLoad(player).orElseThrow().equals(loadResult),
                "Native WorldCore status load should retain the latest AdapterCore result for parity inspection");
        helper.assertTrue(service.activeStatusEffects(player).contains(loadResult.effectId())
                        && service.activeStatusEffects(player).size() == 1,
                "Native WorldCore status load should retain rehydrated active status effect state");
        helper.assertTrue(loadedStatusApplication.applied()
                        && loadedStatusApplication.loaded()
                        && loadedStatusApplication.effectId().equals(loadResult.effectId())
                        && loadedStatusApplication.appliedGameTick() == loadResult.loadedGameTick(),
                "Native WorldCore status load should route rehydrated status through AdapterCore status apply");
        var loadedRuntimeState = service.activeStatusEffectState(player, loadResult.effectId()).orElseThrow();
        helper.assertTrue(Boolean.TRUE.equals(loadedRuntimeState.get("loaded"))
                        && loadedRuntimeState.get("appliedGameTick").equals(loadResult.loadedGameTick())
                        && loadedRuntimeState.get("expiresAtTick").equals(loadResult.loadedGameTick() + loadResult.durationTicks()),
                "Native WorldCore status load should retain timed rehydrated status effect state");
        var loadedProfileState = service.activeStatusProfileState(player, loadResult.effectId()).orElseThrow();
        helper.assertTrue(Boolean.TRUE.equals(loadedProfileState.get("loaded"))
                        && loadedProfileState.get("effectId").equals(loadResult.effectId())
                        && loadedProfileState.get("hazardId").equals(loadResult.hazardId())
                        && service.activeStatusProfileStates(player).containsKey(loadResult.effectId()),
                "Native WorldCore status load should retain rehydrated StatusCore-style profile state");
        helper.assertTrue(service.tickStatusEffects(player, loadResult.loadedGameTick() + loadResult.durationTicks() + 1L) == 0
                        && service.activeStatusEffectStates(player).isEmpty()
                        && service.activeStatusProfileStates(player).isEmpty(),
                "Native WorldCore status runtime should expire active status effects by duration");
        var expiry = service.lastStatusEffectExpiry(player, loadResult.effectId()).orElseThrow();
        helper.assertTrue(expiry.expired()
                        && !expiry.retained()
                        && expiry.effectId().equals(loadResult.effectId())
                        && expiry.expiresAtTick() == loadResult.loadedGameTick() + loadResult.durationTicks(),
                "Native WorldCore status runtime should materialize AdapterCore status expiry state");
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            helper.assertTrue(WorldRegionSavedData.get(serverLevel).hazardExposures(player.getUUID()).contains(hazardId),
                    "Native WorldCore hazard tick should persist hazard exposure for save/load");
            helper.assertTrue(WorldRegionSavedData.get(serverLevel).hazardExposureStatusEffects(player.getUUID())
                            .contains(result.statusEffectId()),
                    "Native WorldCore hazard tick should persist status effect id for save/load");
            helper.assertTrue(WorldRegionSavedData.get(serverLevel).hazardExposureStatusState(player.getUUID(), hazardId)
                            .containsKey(statusResult.saveKey()),
                    "Native WorldCore hazard tick should persist a reloadable status effect payload");
        } else {
            helper.fail("WorldCore hazard tick test requires a server level");
        }
        Identifier weatherHazardId = Identifier.fromNamespaceAndPath("echoworldcore", "hazard/toxic_air");
        service.registerHazardDefinition(new WorldHazardDefinition(
                weatherHazardId,
                "Weather Toxic Air",
                "Weather-driven WorldCore fallback hazard.",
                55,
                false));
        ServerPlayer weatherPlayer = helper.makeMockServerPlayerInLevel();
        weatherPlayer.setHealth(20.0F);
        var weatherTick = service.applyHazardTick(weatherPlayer, new com.echoplatform.echocore.api.WorldHazardSnapshot(
                List.of(regionId),
                List.of(weatherHazardId),
                50,
                false,
                "Weather hazard active: echoweathercore:ash_storm.")).orElseThrow();
        var weatherDamage = service.lastHazardTickDamage(weatherPlayer).orElseThrow();
        var weatherStatus = service.lastStatusEffectSave(weatherPlayer).orElseThrow();
        helper.assertTrue(weatherTick.hazardId().equals(weatherHazardId)
                        && weatherDamage.hazardId().equals(weatherHazardId.toString()),
                "Native WorldCore should tick WeatherCore-mapped shared hazard ids even when taxonomy entries are non-ticking");
        helper.assertTrue(weatherTick.damageApplied() > 0.0F
                        && weatherTick.healthAfter() < weatherTick.healthBefore()
                        && weatherDamage.damageApplied() == weatherTick.damageApplied(),
                "Native WorldCore weather-origin fallback hazards should damage the player through AdapterCore");
        helper.assertTrue(weatherTick.statusEffectId().toString().equals("echostatuscore:status/hazard/toxic_air")
                        && weatherStatus.effectId().equals(weatherTick.statusEffectId().toString())
                        && service.activeStatusEffects(weatherPlayer).contains(weatherStatus.effectId()),
                "Native WorldCore weather-origin fallback hazards should save/apply StatusCore-compatible exposure state");
        EchoWorldRuntimeBus.clearForTests();
        helper.succeed();
    }

    private static void worldcoreSpawnRuleEvent(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        WorldRegionService service = new WorldRegionService();
        AtomicInteger spawned = new AtomicInteger();
        EchoWorldRuntimeBus.onSpawnRuleTriggered(event -> spawned.incrementAndGet());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Identifier regionId = id("spawn_rule_region");
        WorldRegionInstance region = new WorldRegionInstance(
                id("region/spawn_rule/" + player.getUUID()),
                regionId,
                WorldRegionType.CRASH_ZONE,
                "Spawn Rule Region",
                player.level().dimension(),
                player.blockPosition(),
                64,
                List.of(),
                true);
        EchoWorldContracts.EchoSpawnRule spawnRule = new EchoWorldContracts.EchoSpawnRule(
                "echospawncore:spawn/rad_zombie_crash_zone",
                "echoashfallprotocol:rad_zombie",
                regionId.toString(),
                2,
                21.0D);
        EchoWorldContracts.EchoDifficultyProfile difficulty = new EchoWorldContracts.EchoDifficultyProfile(
                "echodifficultycore:hard",
                1.5D,
                1.25D);
        var result = service.triggerSpawnRuleEvent(player, region, spawnRule, difficulty, 1).orElseThrow();
        helper.assertTrue(spawned.get() == 1,
                "Native WorldCore spawn rule event should fire through the runtime bus");
        helper.assertTrue(result.scaledBudget() == 3 && result.spawnCount() == 2,
                "Native WorldCore spawn rule event should apply difficulty-scaled budget after active mob count");
        helper.assertTrue(service.activeDifficultyProfile(player).orElseThrow().id().equals("echodifficultycore:hard")
                        && service.regionDifficultyProfile(regionId).orElseThrow().hazardMultiplier() == 1.5D,
                "Native WorldCore spawn rule event should retain active difficulty modifier state");
        var difficultyState = service.regionDifficultyApplicationState(regionId).orElseThrow();
        helper.assertTrue(difficultyState.get("difficultyId").equals(result.difficultyId())
                        && difficultyState.get("appliedSpawnRuleId").equals(result.ruleId())
                        && difficultyState.get("scaledSpawnBudget").equals(result.scaledBudget())
                        && difficultyState.get("activeSpawnPopulation").equals(result.activeMobCount() + result.spawnCount()),
                "Native WorldCore spawn rule event should retain applied difficulty spawn-budget state");
        var adapterDifficultyResult = service.regionDifficultyApplicationResult(regionId).orElseThrow();
        helper.assertTrue(adapterDifficultyResult.difficultyId().equals(result.difficultyId())
                        && adapterDifficultyResult.appliedSpawnRuleId().equals(result.ruleId())
                        && adapterDifficultyResult.scaledSpawnBudget() == result.scaledBudget()
                        && adapterDifficultyResult.activeSpawnPopulation() == result.activeMobCount() + result.spawnCount()
                        && service.difficultyApplicationResults().containsKey(regionId.toString()),
                "Native WorldCore spawn rule event should materialize AdapterCore difficulty application state");
        helper.assertTrue(result.entityId().equals("echoashfallprotocol:rad_zombie")
                        && result.ruleId().equals("echospawncore:spawn/rad_zombie_crash_zone"),
                "Native WorldCore spawn rule event should preserve the data-backed spawn rule identity");
        helper.assertTrue(service.lastSpawnRuleEvent(player).orElseThrow().equals(result),
                "Native WorldCore spawn rule event should retain the latest AdapterCore result for parity inspection");
        helper.assertTrue(service.activeSpawnPopulation(regionId, spawnRule.id()) == result.scaledBudget()
                        && service.activeSpawnPopulations().size() == 1,
                "Native WorldCore spawn rule event should retain active spawn-zone runtime state");
        var adapterZoneState = service.lastSpawnZoneState(player).orElseThrow();
        helper.assertTrue(adapterZoneState.zoneKey().equals(regionId + "|" + spawnRule.id())
                        && adapterZoneState.entityId().equals(result.entityId())
                        && adapterZoneState.scaledBudget() == result.scaledBudget()
                        && adapterZoneState.activePopulation() == result.activeMobCount() + result.spawnCount()
                        && service.activeSpawnZoneStateResult(regionId, spawnRule.id()).orElseThrow().equals(adapterZoneState)
                        && service.activeSpawnZoneStateResults().size() == 1,
                "Native WorldCore spawn rule event should retain AdapterCore spawn-zone state");
        var zoneState = service.activeSpawnZoneState(regionId, spawnRule.id()).orElseThrow();
        helper.assertTrue(zoneState.get("entityId").equals(result.entityId())
                        && zoneState.get("scaledBudget").equals(result.scaledBudget())
                        && zoneState.get("activePopulation").equals(result.activeMobCount() + result.spawnCount())
                        && zoneState.get("difficultyId").equals(result.difficultyId())
                        && zoneState.get("lastGameTick").equals(result.gameTick()),
                "Native WorldCore spawn rule event should retain detailed spawn-zone simulation state");
        helper.assertTrue(service.activeSpawnZoneStates().size() == 1
                        && service.activeSpawnZoneStates().containsKey(regionId + "|" + spawnRule.id()),
                "Native WorldCore spawn-zone state should be queryable by zone key");
        EchoWorldRuntimeBus.clearForTests();
        helper.succeed();
    }

    private static void worldcoreStructurePoiLookup(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        WorldRegionService service = new WorldRegionService();
        AtomicInteger resolved = new AtomicInteger();
        AtomicInteger scanned = new AtomicInteger();
        EchoWorldRuntimeBus.onStructurePoiResolved(event -> resolved.incrementAndGet());
        EchoWorldRuntimeBus.onRegionScanned(event -> scanned.incrementAndGet());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos pos = player.blockPosition();
        Identifier regionId = id("structure_poi_region");
        Identifier structureId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "drop_pod");
        WorldRegionInstance region = new WorldRegionInstance(
                id("region/structure_poi/" + player.getUUID()),
                regionId,
                WorldRegionType.CRASH_ZONE,
                "Structure POI Region",
                player.level().dimension(),
                pos,
                96,
                List.of(),
                true);
        EchoWorldContracts.EchoStructurePlacement structure = new EchoWorldContracts.EchoStructurePlacement(
                structureId.toString(),
                "echoashfallprotocol:poi/drop_pod",
                pos.getX() + 3,
                pos.getY(),
                pos.getZ() + 4);
        var result = service.triggerStructurePoiLookup(player, region, structure, 8).orElseThrow();
        helper.assertTrue(result.inRange() && result.distanceSquared() == 25L,
                "Native WorldCore structure/POI lookup should resolve the in-range POI distance");
        helper.assertTrue(result.structureId().equals("echoashfallprotocol:drop_pod")
                        && result.poiId().equals("echoashfallprotocol:poi/drop_pod"),
                "Native WorldCore structure/POI lookup should preserve structure and POI ids");
        helper.assertTrue(resolved.get() == 1 && scanned.get() == 1,
                "Native WorldCore structure/POI lookup should fire resolved and scan runtime events");
        helper.assertTrue(service.markerById(player.level(), Identifier.tryParse(result.markerId())).isPresent(),
                "Native WorldCore structure/POI lookup should persist a marker for map and save/load consumers");
        helper.assertTrue(service.lastStructurePoiLookup(player).orElseThrow().equals(result),
                "Native WorldCore structure/POI lookup should retain the latest AdapterCore result for parity inspection");
        var markerState = service.lastStructurePoiMarkerState(player).orElseThrow();
        helper.assertTrue(markerState.markerId().equals(result.markerId())
                        && markerState.structureId().equals(result.structureId())
                        && markerState.poiId().equals(result.poiId())
                        && markerState.markerPersisted()
                        && service.resolvedStructurePoiMarkerState(result.markerId()).orElseThrow().equals(markerState)
                        && service.resolvedStructurePoiMarkerStates().size() == 1,
                "Native WorldCore structure/POI lookup should persist AdapterCore marker state");
        var discoveryState = service.lastStructureDiscoveryState(player).orElseThrow();
        helper.assertTrue(discoveryState.markerId().equals(markerState.markerId())
                        && discoveryState.discoveryState().equals("DISCOVERED")
                        && discoveryState.discovered()
                        && discoveryState.holomapMarkerActive()
                        && service.resolvedStructureDiscoveryState(markerState.markerId()).orElseThrow().equals(discoveryState)
                        && service.resolvedStructureDiscoveryStates().size() == 1,
                "Native WorldCore structure/POI lookup should materialize StructureCore discovery state");
        var poiState = service.resolvedStructurePoiState(result.markerId()).orElseThrow();
        helper.assertTrue(poiState.get("markerId").equals(result.markerId())
                        && poiState.get("structureId").equals(result.structureId())
                        && poiState.get("poiId").equals(result.poiId())
                        && poiState.get("distanceSquared").equals(result.distanceSquared())
                        && poiState.get("lastGameTick").equals(result.gameTick()),
                "Native WorldCore structure/POI lookup should retain detailed POI runtime state");
        helper.assertTrue(service.resolvedStructurePoiStates().size() == 1
                        && service.resolvedStructurePoiStates().containsKey(result.markerId()),
                "Native WorldCore structure/POI runtime state should be queryable by marker id");
        EchoWorldRuntimeBus.clearForTests();
        helper.succeed();
    }

    private static void worldcoreCellSample(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        WorldRegionService service = new WorldRegionService();
        AtomicInteger sampled = new AtomicInteger();
        EchoWorldRuntimeBus.onWorldCellSampled(event -> sampled.incrementAndGet());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos pos = player.blockPosition();
        Identifier regionId = id("cell_sample_region");
        Identifier hazardId = id("hazard/cell_sample");
        WorldRegionInstance region = new WorldRegionInstance(
                id("region/cell_sample/" + player.getUUID()),
                regionId,
                WorldRegionType.CRASH_ZONE,
                "Cell Sample Region",
                player.level().dimension(),
                pos,
                64,
                List.of(hazardId),
                true);
        EchoWorldContracts.EchoWorldHazard hazard = new EchoWorldContracts.EchoWorldHazard(
                hazardId.toString(),
                "field",
                pos.getX(),
                pos.getZ(),
                8,
                1.0D,
                "echostatuscore:status/cell_sample");
        EchoWorldContracts.EchoBiomeProfile biome = new EchoWorldContracts.EchoBiomeProfile(
                "echoashfallprotocol:crash_zone_wasteland",
                "#echoashfallprotocol:common_wasteland_biomes",
                hazardId.toString());
        EchoWorldContracts.EchoStructurePlacement structure = new EchoWorldContracts.EchoStructurePlacement(
                "echoashfallprotocol:drop_pod",
                "echoashfallprotocol:poi/drop_pod",
                pos.getX(),
                pos.getY(),
                pos.getZ());
        var result = service.sampleWorldCell(
                player,
                region,
                hazard,
                biome,
                structure,
                "echoashfallprotocol:ashfall_crash_zone_definition_world").orElseThrow();
        helper.assertTrue(sampled.get() == 1,
                "Native WorldCore world cell sample should fire through the runtime bus");
        helper.assertTrue(result.inRegion() && result.inHazard(),
                "Native WorldCore world cell sample should detect the active region and hazard field");
        helper.assertTrue(result.activeRegionId().equals(regionId.toString())
                        && result.activeHazardId().equals(hazardId.toString()),
                "Native WorldCore world cell sample should preserve active region and hazard ids");
        helper.assertTrue(result.biomeProfileId().equals("echoashfallprotocol:crash_zone_wasteland")
                        && result.poiId().equals("echoashfallprotocol:poi/drop_pod"),
                "Native WorldCore world cell sample should expose biome and POI identity");
        helper.assertTrue(service.lastWorldCellSample(player).orElseThrow().equals(result),
                "Native WorldCore world cell sample should retain the latest AdapterCore result for parity inspection");
        helper.assertTrue(service.sampledWorldCell(result.cellKey()).orElseThrow().equals(result)
                        && service.sampledHazardField(result.activeHazardId()).orElseThrow().equals(result)
                        && service.sampledWorldCells().size() == 1
                        && service.sampledHazardFields().size() == 1,
                "Native WorldCore world cell sample should retain sampled cell and hazard-field runtime state");
        var hazardEnter = service.lastHazardTransition(player).orElseThrow();
        helper.assertTrue(hazardEnter.eventType().equals("ENTER")
                        && hazardEnter.hazardEntered()
                        && hazardEnter.currentHazardId().equals(result.activeHazardId())
                        && hazardEnter.statusEffects().contains("echostatuscore:status/cell_sample")
                        && service.activeHazardId(player).equals(result.activeHazardId()),
                "Native WorldCore world cell sample should materialize hazard-field enter state");
        String chunkKey = result.worldId() + ":chunk:" + Math.floorDiv(result.x(), 16) + ":" + Math.floorDiv(result.z(), 16);
        var adapterChunkState = service.lastWorldChunkState(player).orElseThrow();
        helper.assertTrue(adapterChunkState.chunkKey().equals(chunkKey)
                        && adapterChunkState.lastCellKey().equals(result.cellKey())
                        && adapterChunkState.activeHazardId().equals(result.activeHazardId())
                        && service.sampledWorldChunkState(chunkKey).orElseThrow().equals(adapterChunkState)
                        && service.sampledWorldChunkStates().size() == 1,
                "Native WorldCore world cell sample should materialize AdapterCore chunk state");
        var chunkState = service.sampledWorldChunk(chunkKey).orElseThrow();
        helper.assertTrue(chunkState.get("lastCellKey").equals(result.cellKey())
                        && chunkState.get("activeHazardId").equals(result.activeHazardId())
                        && chunkState.get("chunkX").equals(Math.floorDiv(result.x(), 16))
                        && service.sampledWorldChunks().size() == 1,
                "Native WorldCore world cell sample should materialize retained chunk runtime state");
        var hazardFieldState = service.sampledHazardFieldState(result.activeHazardId()).orElseThrow();
        var adapterHazardFieldState = service.lastHazardFieldState(player).orElseThrow();
        helper.assertTrue(adapterHazardFieldState.hazardId().equals(result.activeHazardId())
                        && adapterHazardFieldState.lastCellKey().equals(result.cellKey())
                        && adapterHazardFieldState.sampledInside()
                        && service.sampledHazardFieldStateResult(result.activeHazardId()).orElseThrow()
                                .equals(adapterHazardFieldState)
                        && service.sampledHazardFieldStateResults().size() == 1,
                "Native WorldCore world cell sample should materialize AdapterCore hazard-field state");
        helper.assertTrue(hazardFieldState.get("lastCellKey").equals(result.cellKey())
                        && hazardFieldState.get("radius").equals(hazard.radius())
                        && Boolean.TRUE.equals(hazardFieldState.get("sampledInside"))
                        && service.sampledHazardFieldStates().size() == 1,
                "Native WorldCore world cell sample should materialize retained hazard-field runtime state");
        var biomeOverlay = service.lastBiomeHazardOverlay(player).orElseThrow();
        helper.assertTrue(biomeOverlay.active()
                        && biomeOverlay.visibleOnHud()
                        && biomeOverlay.biomeProfileId().equals(result.biomeProfileId())
                        && biomeOverlay.hazardId().equals(result.activeHazardId())
                        && biomeOverlay.cellKey().equals(result.cellKey())
                        && service.sampledBiomeHazardOverlay(result.cellKey()).orElseThrow().equals(biomeOverlay)
                        && service.sampledBiomeHazardOverlays().size() == 1,
                "Native WorldCore world cell sample should resolve and retain BiomeCore hazard overlay state");
        EchoWorldContracts.EchoWorldHazard exitedHazard = new EchoWorldContracts.EchoWorldHazard(
                hazardId.toString(),
                "field",
                pos.getX() + 64,
                pos.getZ() + 64,
                8,
                1.0D,
                "echostatuscore:status/cell_sample");
        service.sampleWorldCell(
                player,
                region,
                exitedHazard,
                biome,
                structure,
                "echoashfallprotocol:ashfall_crash_zone_definition_world").orElseThrow();
        var hazardExit = service.lastHazardTransition(player).orElseThrow();
        helper.assertTrue(hazardExit.eventType().equals("EXIT")
                        && hazardExit.hazardExited()
                        && hazardExit.previousHazardId().equals(result.activeHazardId())
                        && hazardExit.currentHazardId().isBlank()
                        && service.activeHazardId(player).isBlank(),
                "Native WorldCore world cell sample should materialize hazard-field exit state");
        EchoWorldRuntimeBus.clearForTests();
        helper.succeed();
    }

    private static void worldcoreToxicSwampSecondSlice(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> worldcoreToxicSwampSecondSliceIsolated(helper));
    }

    private static void worldcoreToxicSwampSecondSliceIsolated(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        WorldRegionService service = new WorldRegionService();
        Identifier regionId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "toxic_swamp");
        Identifier hazardId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "hazard/toxic_ash");
        Identifier missionId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "mission/first_relay_station_route");
        Identifier structureId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "toxic_swamp");
        AtomicInteger missionsStarted = new AtomicInteger();
        AtomicInteger entered = new AtomicInteger();
        AtomicInteger statusSaved = new AtomicInteger();
        AtomicInteger spawned = new AtomicInteger();
        AtomicInteger sampled = new AtomicInteger();
        AtomicInteger resolved = new AtomicInteger();
        EchoCoreServices.registerMissionService(missionServiceForRegion(missionId, regionId, "hard", missionsStarted));
        EchoWorldRuntimeBus.onRegionEntered(event -> entered.incrementAndGet());
        EchoWorldRuntimeBus.onStatusEffectSaved(event -> statusSaved.incrementAndGet());
        EchoWorldRuntimeBus.onSpawnRuleTriggered(event -> spawned.incrementAndGet());
        EchoWorldRuntimeBus.onWorldCellSampled(event -> sampled.incrementAndGet());
        EchoWorldRuntimeBus.onStructurePoiResolved(event -> resolved.incrementAndGet());
        service.registerHazardDefinition(new WorldHazardDefinition(hazardId,
                "Toxic Ash", "Fine corrosive ash that raises exposure during unprotected Ashfall routes.", 62, true));
        service.registerRegionDefinition(new WorldRegionDefinition(
                regionId,
                WorldRegionType.TOXIC_SWAMP,
                "Toxic Swamp",
                "Corroded wetlands, chemical runoff, bio-lab remains, and spore exposure.",
                List.of(Identifier.fromNamespaceAndPath("echoashfallprotocol", "toxic_swamp")),
                List.of(Identifier.fromNamespaceAndPath("echoashfallprotocol", "toxic_air_biomes")),
                List.of(structureId),
                List.of(hazardId),
                regionId,
                96,
                Identifier.fromNamespaceAndPath("echoworldcore", "region/toxic_swamp"),
                Identifier.fromNamespaceAndPath("echoworldcore", "ambience/toxic_swamp"),
                30));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setHealth(20.0F);
        BlockPos pos = player.blockPosition();
        service.revealMarker(player.level(), new WorldMarker(
                id("marker/toxic_swamp_second_slice/" + player.getUUID()),
                regionId,
                WorldMarkerType.HAZARD,
                "Toxic Swamp",
                "Second Agent 7 live native world/weather/hazard slice.",
                player.level().dimension(),
                pos,
                96,
                true,
                player.level().getGameTime()));
        service.tickPlayer(player);
        var transition = service.lastRegionTransition(player).orElseThrow();
        helper.assertTrue(entered.get() == 1 && transition.regionEntered()
                        && transition.currentRegionId().equals(regionId.toString())
                        && transition.missionEvents().contains(missionId.toString())
                        && missionsStarted.get() == 1,
                "Native toxic-swamp slice should enter the region and trigger the route mission");
        var hazardDamage = service.lastHazardTickDamage(player).orElseThrow();
        helper.assertTrue(hazardDamage.hazardId().equals(hazardId.toString())
                        && hazardDamage.statusEffectId().equals("echostatuscore:status/toxic_ash")
                        && hazardDamage.difficultyId().equals("echodifficultycore:hard")
                        && Math.abs(hazardDamage.baseDamage() - 1.24D) < 0.0001D
                        && Math.abs(hazardDamage.damageApplied() - 1.86D) < 0.0001D
                        && hazardDamage.healthAfter() < hazardDamage.healthBefore(),
                "Native toxic-swamp slice should apply hard-scaled toxic ash tick damage");
        var statusSave = service.lastStatusEffectSave(player).orElseThrow();
        var statusProfile = service.activeStatusProfileState(player, statusSave.effectId()).orElseThrow();
        helper.assertTrue(statusSaved.get() == 1
                        && statusSave.effectId().equals("echostatuscore:status/toxic_ash")
                        && statusSave.saveKey().equals("echoworldcore.hazard.hazard_toxic_ash.status")
                        && statusProfile.get("statusKind").equals("TOXIN")
                        && statusProfile.get("severity").equals("MEDIUM"),
                "Native toxic-swamp slice should save the toxic status and retain StatusCore-style profile state");
        var spawn = service.lastSpawnRuleEvent(player).orElseThrow();
        helper.assertTrue(spawned.get() == 1
                        && spawn.regionId().equals(regionId.toString())
                        && spawn.difficultyId().equals("echodifficultycore:hard")
                        && spawn.scaledBudget() == 3
                        && service.activeSpawnPopulation(regionId, spawn.ruleId()) == 3,
                "Native toxic-swamp slice should plan a hard-scaled spawn-zone event");
        EchoWorldContracts.EchoWorldHazard hazard = new EchoWorldContracts.EchoWorldHazard(
                hazardId.toString(),
                "toxic_ash",
                pos.getX(),
                pos.getZ(),
                12,
                1.24D,
                "echostatuscore:status/toxic_ash");
        EchoWorldContracts.EchoBiomeProfile biome = new EchoWorldContracts.EchoBiomeProfile(
                "echoashfallprotocol:toxic_swamp",
                "#echoashfallprotocol:toxic_air_biomes",
                hazardId.toString());
        EchoWorldContracts.EchoStructurePlacement structure = new EchoWorldContracts.EchoStructurePlacement(
                structureId.toString(),
                "echoashfallprotocol:poi/toxic_swamp",
                pos.getX(),
                pos.getY(),
                pos.getZ());
        WorldRegionInstance region = new WorldRegionInstance(
                id("region/toxic_swamp_second_slice/" + player.getUUID()),
                regionId,
                WorldRegionType.TOXIC_SWAMP,
                "Toxic Swamp",
                player.level().dimension(),
                pos,
                96,
                List.of(hazardId),
                true);
        int sampledBeforeExplicitCell = sampled.get();
        var cell = service.sampleWorldCell(player, region, hazard, biome, structure,
                "echoashfallprotocol:ashfall_toxic_swamp_definition_world").orElseThrow();
        int resolvedBeforeExplicitLookup = resolved.get();
        var poi = service.triggerStructurePoiLookup(player, region, structure, 128).orElseThrow();
        helper.assertTrue(sampled.get() == sampledBeforeExplicitCell + 1
                        && cell.inRegion()
                        && cell.inHazard()
                        && cell.biomeProfileId().equals("echoashfallprotocol:toxic_swamp")
                        && cell.poiId().equals("echoashfallprotocol:poi/toxic_swamp"),
                "Native toxic-swamp slice should sample the toxic swamp cell and hazard field");
        helper.assertTrue(resolved.get() == resolvedBeforeExplicitLookup + 1
                        && poi.inRange()
                        && poi.structureId().equals(structureId.toString())
                        && poi.poiId().equals("echoashfallprotocol:poi/toxic_swamp")
                        && service.resolvedStructurePoiState(poi.markerId()).isPresent(),
                "Native toxic-swamp slice should resolve and retain the toxic swamp POI");
        var difficultyState = service.regionDifficultyApplicationState(regionId).orElseThrow();
        var difficultySelection = service.regionDifficultyProfileSelection(regionId).orElseThrow();
        helper.assertTrue(difficultyState.get("difficultyId").equals("echodifficultycore:hard")
                        && difficultyState.get("appliedHazardId").equals(hazardId.toString())
                        && difficultyState.get("appliedSpawnRuleId").equals(spawn.ruleId()),
                "Native toxic-swamp slice should retain applied hard difficulty hazard and spawn state");
        helper.assertTrue(difficultySelection.difficultyId().equals("echodifficultycore:hard")
                        && difficultySelection.selectedDifficulty().equals("hard")
                        && difficultySelection.hazardMultiplier() == 1.5D
                        && difficultySelection.spawnMultiplier() == 1.25D,
                "Native toxic-swamp slice should retain AdapterCore DifficultyCore profile selection state");
        EchoWorldRuntimeBus.clearForTests();
        helper.succeed();
    }

    private static com.echoplatform.echocore.api.mission.IMissionService missionServiceForRegion(
            Identifier missionId,
            Identifier regionId,
            String difficulty,
            AtomicInteger starts) {
        com.echoplatform.echocore.api.mission.InMemoryMissionRegistry registry =
                new com.echoplatform.echocore.api.mission.InMemoryMissionRegistry();
        registry.registerMission("gametest", com.echoplatform.echocore.api.mission.MissionDefinition
                .builder(missionId, id("chapter/worldcore_region"))
                .category("gametest", difficulty)
                .metadata("worldRegion", regionId.toString())
                .build());
        return new com.echoplatform.echocore.api.mission.IMissionService() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public void registerChapter(String source, com.echoplatform.echocore.api.mission.MissionChapterDefinition chapter) {
                registry.registerChapter(source, chapter);
            }

            @Override
            public void registerMission(String source, com.echoplatform.echocore.api.mission.MissionDefinition mission) {
                registry.registerMission(source, mission);
            }

            @Override
            public java.util.Optional<com.echoplatform.echocore.api.mission.MissionChapterDefinition> chapter(Identifier chapterId) {
                return registry.chapter(chapterId);
            }

            @Override
            public java.util.Optional<com.echoplatform.echocore.api.mission.MissionDefinition> missionDefinition(Identifier requestedMissionId) {
                return registry.missionDefinition(requestedMissionId);
            }

            @Override
            public List<com.echoplatform.echocore.api.mission.MissionChapterDefinition> chapters() {
                return registry.chapters();
            }

            @Override
            public List<com.echoplatform.echocore.api.mission.MissionDefinition> missionDefinitions() {
                return registry.missionDefinitions();
            }

            @Override
            public List<com.echoplatform.echocore.api.mission.IMissionProgressView> missions(net.minecraft.world.entity.player.Player player) {
                return List.of();
            }

            @Override
            public List<com.echoplatform.echocore.api.mission.IMissionProgressView> missions(
                    net.minecraft.world.entity.player.Player player,
                    Identifier chapterId) {
                return List.of();
            }

            @Override
            public java.util.Optional<com.echoplatform.echocore.api.mission.IMissionProgressView> mission(
                    net.minecraft.world.entity.player.Player player,
                    Identifier requestedMissionId) {
                return java.util.Optional.empty();
            }

            @Override
            public boolean startMission(ServerPlayer player, Identifier requestedMissionId) {
                if (missionId.equals(requestedMissionId)) {
                    starts.incrementAndGet();
                    return true;
                }
                return false;
            }

            @Override
            public boolean completeMission(ServerPlayer player, Identifier requestedMissionId) {
                return false;
            }

            @Override
            public boolean claimReward(ServerPlayer player, Identifier requestedMissionId) {
                return false;
            }

            @Override
            public boolean handleAction(ServerPlayer player, Identifier requestedMissionId, String actionId) {
                return false;
            }

            @Override
            public boolean recordObjective(ServerPlayer player,
                    com.echoplatform.echocore.api.mission.MissionObjectiveType type,
                    Identifier target,
                    int amount,
                    Map<String, String> context) {
                return false;
            }

            @Override
            public String debugState(net.minecraft.world.entity.player.Player player, Identifier requestedMissionId) {
                return missionId.equals(requestedMissionId) ? "region mission active" : "mission not active";
            }
        };
    }

    private static void worldcoreMapProvider(GameTestHelper helper) {
        WorldRegionService service = WorldRegionService.INSTANCE;
        WorldCoreBuiltins.register(service);
        Identifier hazardId = id("hazard/map_provider_test");
        Identifier regionId = id("map_provider_region");
        service.registerHazardDefinition(new WorldHazardDefinition(hazardId,
                "Map Provider Hazard", "Hazard used by the WorldCore map-provider smoke test.", 35, false));
        service.registerRegionDefinition(new WorldRegionDefinition(
                regionId,
                WorldRegionType.ANOMALY_ZONE,
                "Map Provider Region",
                "Marker-backed test region.",
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(id("structure/map_provider")),
                java.util.List.of(hazardId),
                regionId,
                64,
                id("region/map_provider"),
                id("ambience/map_provider"),
                999));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos pos = player.blockPosition();
        service.revealMarker(player, new WorldMarker(
                id("marker/map_provider"),
                regionId,
                WorldMarkerType.ANOMALY,
                "Map Provider Marker",
                "Marker-backed region for map provider tests.",
                player.level().dimension(),
                pos,
                64,
                true,
                player.level().getGameTime()));
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            WorldRegionSavedData.get(serverLevel).saveMarker(new WorldMarker(
                    id("marker/map_provider_locked"),
                    regionId,
                    WorldMarkerType.ANOMALY,
                    "Locked Map Provider Marker",
                    "Undiscovered marker should not load into HoloMap.",
                    player.level().dimension(),
                    pos.offset(12, 0, 0),
                    64,
                    false,
                    player.level().getGameTime()));
            WorldRegionSavedData.get(serverLevel).saveMarker(new WorldMarker(
                    id("marker/map_provider_far"),
                    regionId,
                    WorldMarkerType.ANOMALY,
                    "Far Map Provider Marker",
                    "Far discovered marker should not load into HoloMap.",
                    player.level().dimension(),
                    pos.offset(1200, 0, 0),
                    64,
                    true,
                    player.level().getGameTime()));
        }
        java.util.List<IMapMarker> markers = WorldCoreMapDataProvider.INSTANCE.markers(player);
        helper.assertTrue(markers.stream().anyMatch(marker -> marker.id().getPath().contains("map/world_marker")),
                "WorldCore map provider should emit persisted world markers");
        helper.assertTrue(markers.stream().anyMatch(marker -> marker.id().getPath().contains("map/region")),
                "WorldCore map provider should emit active region markers");
        helper.assertTrue(markers.stream().anyMatch(marker -> marker.id().getPath().contains("map/hazard/world_snapshot")),
                "WorldCore map provider should emit hazard overlay markers");
        helper.assertTrue(markers.stream().noneMatch(marker -> marker.state() == IMapMarker.MarkerState.LOCKED
                        || marker.title().contains("Undiscovered")),
                "WorldCore map provider should not emit locked or undiscovered HoloMap entries");
        helper.assertTrue(markers.stream().noneMatch(marker -> marker.id().getPath().contains("map_provider_far")),
                "WorldCore map provider should not emit far discovered markers into HoloMap");
        helper.succeed();
    }

    private static void worldcoreHoloMapZones(GameTestHelper helper) {
        if (!ModList.get().isLoaded("echoholomap")) {
            helper.succeed();
            return;
        }
        WorldRegionService service = WorldRegionService.INSTANCE;
        WorldCoreBuiltins.register(service);
        Identifier hazardId = id("hazard/holomap_zone_test");
        Identifier regionId = id("holomap_zone_region");
        service.registerHazardDefinition(new WorldHazardDefinition(hazardId,
                "HoloMap Zone Hazard", "Hazard used by the WorldCore rich HoloMap zone smoke test.", 55, false));
        service.registerRegionDefinition(new WorldRegionDefinition(
                regionId,
                WorldRegionType.RADIATION_ZONE,
                "HoloMap Zone Region",
                "Marker-backed test region for rich HoloMap zones.",
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(id("structure/holomap_zone")),
                java.util.List.of(hazardId),
                regionId,
                96,
                id("region/holomap_zone"),
                id("ambience/holomap_zone"),
                1000));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        service.revealMarker(player, new WorldMarker(
                id("marker/holomap_zone"),
                regionId,
                WorldMarkerType.HAZARD,
                "HoloMap Zone Marker",
                "Marker-backed region for rich HoloMap zone tests.",
                player.level().dimension(),
                player.blockPosition(),
                96,
                true,
                player.level().getGameTime()));

        try {
            Class<?> providerClass = Class.forName("com.knoxhack.echoworldcore.integration.WorldCoreHoloMapRichProvider");
            Object provider = providerClass.getField("INSTANCE").get(null);
            @SuppressWarnings("unchecked")
            List<Object> zones = (List<Object>) providerClass.getMethod("zones", net.minecraft.world.entity.player.Player.class)
                    .invoke(provider, player);
            helper.assertTrue(zones.stream().anyMatch(zone -> "CIRCLE".equals(propertyName(zone, "shape"))
                            && "HAZARD_STRIPES".equals(propertyName(zone, "pattern"))
                            && radius(zone) == 96.0F),
                    "WorldCore rich HoloMap provider should convert active radius regions into styled circle zones");
            helper.assertTrue(zones.stream().anyMatch(zone -> zoneId(zone).contains("zone/hazard/world_snapshot")
                            && "HAZARD_STRIPES".equals(propertyName(zone, "pattern"))),
                    "WorldCore rich HoloMap provider should emit the active hazard snapshot as a zone");
            helper.assertTrue(zones.stream().noneMatch(zone -> "LOCKED".equals(propertyName(zone, "state"))
                            || "Undiscovered Region".equals(propertyName(zone, "title"))),
                    "WorldCore rich HoloMap provider should not emit locked or undiscovered zones");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("WorldCore HoloMap rich zone provider reflection failed", exception);
        }
        helper.succeed();
    }

    private static void worldcoreNoop(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            helper.assertTrue(EchoCoreServices.worldRegions() == NoOpWorldService.INSTANCE,
                    "Missing WorldCore service should resolve to the no-op implementation");
            helper.assertTrue(EchoCoreServices.regionService().regionDefinitions().isEmpty(),
                    "No-op region service should expose no definitions");
            helper.assertTrue(EchoCoreServices.hazardService().hazardSnapshot(null).safeZone(),
                    "No-op hazard service should report nominal state");
        });
        helper.succeed();
    }

    private static void worldcoreReleaseGuards(GameTestHelper helper) {
        WorldRegionService service = new WorldRegionService();
        WorldCoreBuiltins.register(service);
        helper.assertTrue(service.regionDefinitions().isEmpty(),
                "WorldCore v1.2 should not own chapter-specific region definitions");
        helper.assertTrue(service.hazardDefinitions().size() == 8,
                "WorldCore v1.2 should keep the eight shared framework hazard definitions");
        helper.assertTrue(service.validateMarkers(null).isEmpty(),
                "WorldCore v1.2 release guard should not ship invalid standalone definitions");
        helper.succeed();
    }

    private static String propertyName(Object target, String methodName) {
        Object value = property(target, methodName);
        return value == null ? "" : value.toString();
    }

    private static String zoneId(Object zone) {
        return propertyName(zone, "id");
    }

    private static float radius(Object zone) {
        Object value = property(zone, "radius");
        return value instanceof Number number ? number.floatValue() : -1.0F;
    }

    private static Object property(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not read reflected property " + methodName, exception);
        }
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                100,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                2);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoWorldCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, path);
    }
}
