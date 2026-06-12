package com.knoxhack.echoholomap.test;

import com.google.gson.JsonElement;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoHazardTelemetry;
import com.echoplatform.echocore.api.EchoMapLayer;
import com.echoplatform.echocore.api.EchoMapMarker;
import com.echoplatform.echocore.api.EchoRouteRecord;
import com.echoplatform.echocore.api.IMapDataProvider;
import com.echoplatform.echocore.api.IMapLayer;
import com.echoplatform.echocore.api.IMapMarker;
import com.echoplatform.echocore.api.IWorldRegionService;
import com.echoplatform.echocore.api.WorldDiscoverySource;
import com.echoplatform.echocore.api.WorldHazardDefinition;
import com.echoplatform.echocore.api.WorldHazardSnapshot;
import com.echoplatform.echocore.api.WorldMarker;
import com.echoplatform.echocore.api.WorldMarkerType;
import com.echoplatform.echocore.api.WorldRegionDefinition;
import com.echoplatform.echocore.api.WorldRegionInstance;
import com.echoplatform.echocore.api.WorldRegionType;
import com.echoplatform.echocore.api.mission.InMemoryMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.action.EchoActionRegistry;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.HoloMapIds;
import com.knoxhack.echoholomap.api.HoloMapChunkActionResult;
import com.knoxhack.echoholomap.api.HoloMapChunkSelection;
import com.knoxhack.echoholomap.api.HoloMapLayerData;
import com.knoxhack.echoholomap.api.HoloMapMarkerData;
import com.knoxhack.echoholomap.api.HoloMapOverlayData;
import com.knoxhack.echoholomap.api.HoloMapOverlayKind;
import com.knoxhack.echoholomap.api.HoloMapPrecision;
import com.knoxhack.echoholomap.api.HoloMapRouteData;
import com.knoxhack.echoholomap.api.HoloMapRoutePoint;
import com.knoxhack.echoholomap.api.HoloMapZoneData;
import com.knoxhack.echoholomap.api.HoloMapZonePattern;
import com.knoxhack.echoholomap.api.HoloMapZonePoint;
import com.knoxhack.echoholomap.api.HoloMapZoneShape;
import com.knoxhack.echoholomap.api.IHoloMapDataProvider;
import com.knoxhack.echoholomap.client.HoloMapRenderer;
import com.knoxhack.echoholomap.client.HoloMapUiController;
import com.knoxhack.echoholomap.client.HoloMapViewState;
import com.knoxhack.echoholomap.client.HoloMapVisualStyle;
import com.knoxhack.echoholomap.integration.HoloMapMissionCoreIntegration;
import com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration;
import com.knoxhack.echoholomap.map.BuiltinHoloMapChunkActionProvider;
import com.knoxhack.echoholomap.map.BuiltinHoloMapRouteHazardProvider;
import com.knoxhack.echoholomap.map.HoloMapChunkActions;
import com.knoxhack.echoholomap.map.HoloMapLayers;
import com.knoxhack.echoholomap.map.HoloMapService;
import com.knoxhack.echoholomap.map.HoloMapTerrainPalette;
import com.knoxhack.echoholomap.map.HoloMapTerrainScanner;
import com.knoxhack.echoholomap.map.HoloMapTerrainTile;
import com.knoxhack.echoholomap.map.HoloMapVisibility;
import com.knoxhack.echoholomap.network.HoloMapClientState;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import com.knoxhack.echoholomap.network.HoloMapTerrainClientState;
import com.knoxhack.echoholomap.network.HoloMapTileBatchPacket;
import com.knoxhack.echoholomap.network.HoloMapTileRequestPacket;
import com.knoxhack.echoholomap.network.HoloMapWaypointClientState;
import com.knoxhack.echoholomap.network.HoloMapWaypointSyncPacket;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint.Scope;
import com.knoxhack.echoholomap.world.HoloMapSavedData;
import com.knoxhack.echoholomap.world.HoloMapTerrainSavedData;
import com.knoxhack.echoholomap.world.HoloMapWaypointSavedData;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoHoloMap.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILTIN_PROVIDER_REGISTRATION =
            TEST_FUNCTIONS.register("builtin_provider_registration", () -> ModGameTests::builtinProviderRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SERVICE_PROVIDER_ISOLATION =
            TEST_FUNCTIONS.register("service_provider_isolation", () -> ModGameTests::serviceProviderIsolation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SNAPSHOT_FILTERING_AND_CAP =
            TEST_FUNCTIONS.register("snapshot_filtering_and_cap", () -> ModGameTests::snapshotFilteringAndCap);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DISCOVERY_GATED_SNAPSHOT =
            TEST_FUNCTIONS.register("discovery_gated_snapshot", () -> ModGameTests::discoveryGatedSnapshot);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEBUG_MARKER_SAVED_DATA_CODEC =
            TEST_FUNCTIONS.register("debug_marker_saved_data_codec", () -> ModGameTests::debugMarkerSavedDataCodec);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERRAIN_SAVED_DATA_CODEC =
            TEST_FUNCTIONS.register("terrain_saved_data_codec", () -> ModGameTests::terrainSavedDataCodec);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERRAIN_V3_TILE_METADATA =
            TEST_FUNCTIONS.register("terrain_v3_tile_metadata", () -> ModGameTests::terrainV3TileMetadata);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERRAIN_PALETTE_DETERMINISM =
            TEST_FUNCTIONS.register("terrain_palette_determinism", () -> ModGameTests::terrainPaletteDeterminism);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERRAIN_SCANNER_AND_REQUEST_CAPS =
            TEST_FUNCTIONS.register("terrain_scanner_and_request_caps", () -> ModGameTests::terrainScannerAndRequestCaps);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WAYPOINT_SAVED_DATA_CODEC =
            TEST_FUNCTIONS.register("waypoint_saved_data_codec", () -> ModGameTests::waypointSavedDataCodec);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WAYPOINT_MUTATION_RULES =
            TEST_FUNCTIONS.register("waypoint_mutation_rules", () -> ModGameTests::waypointMutationRules);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHUNK_ACTION_CREATES_SAVED_WAYPOINT =
            TEST_FUNCTIONS.register("chunk_action_creates_saved_waypoint",
                    () -> ModGameTests::chunkActionCreatesSavedWaypoint);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WAYPOINT_CLIENT_MERGE =
            TEST_FUNCTIONS.register("waypoint_client_merge", () -> ModGameTests::waypointClientMerge);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEATHPOINT_PERSONAL_VISIBILITY =
            TEST_FUNCTIONS.register("deathpoint_personal_visibility", () -> ModGameTests::deathpointPersonalVisibility);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEATHPOINT_CODEC_AND_RETENTION =
            TEST_FUNCTIONS.register("deathpoint_codec_and_retention", () -> ModGameTests::deathpointCodecAndRetention);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSION_CORE_CONTENT =
            TEST_FUNCTIONS.register("missioncore_content_registration", () -> ModGameTests::missionCoreContentRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> THEME_CORE_STYLE_FALLBACK =
            TEST_FUNCTIONS.register("theme_core_style_fallback", () -> ModGameTests::themeCoreStyleFallback);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RICH_PROVIDER_SNAPSHOT =
            TEST_FUNCTIONS.register("rich_provider_snapshot", () -> ModGameTests::richProviderSnapshot);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILTIN_ROUTE_HAZARD_PROVIDER_SNAPSHOT =
            TEST_FUNCTIONS.register("builtin_route_hazard_provider_snapshot",
                    () -> ModGameTests::builtinRouteHazardProviderSnapshot);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_PROVIDER_RICH_ADAPTATION =
            TEST_FUNCTIONS.register("core_provider_rich_adaptation", () -> ModGameTests::coreProviderRichAdaptation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CLIENT_RENDER_CACHE_STATE =
            TEST_FUNCTIONS.register("client_render_cache_state", () -> ModGameTests::clientRenderCacheState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ZONE_PACKET_ROUND_TRIP =
            TEST_FUNCTIONS.register("zone_packet_round_trip", () -> ModGameTests::zonePacketRoundTrip);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TACTICAL_POLISH_HELPERS =
            TEST_FUNCTIONS.register("tactical_polish_helpers", () -> ModGameTests::tacticalPolishHelpers);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCREENCORE_HOLOMAP_REGISTRATION =
            TEST_FUNCTIONS.register("screencore_holomap_registration", () -> ModGameTests::screenCoreHoloMapRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MINIMAP_ENABLED_DEFAULT =
            TEST_FUNCTIONS.register("minimap_enabled_default", () -> ModGameTests::minimapEnabledDefault);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("holomap_service"));
        Holder<TestEnvironmentDefinition<?>> richEnvironment = event.registerEnvironment(id("holomap_rich_service"));
        Holder<TestEnvironmentDefinition<?>> adapterEnvironment = event.registerEnvironment(id("holomap_adapter_service"));
        register(event, environment, "builtin_provider_registration", BUILTIN_PROVIDER_REGISTRATION.getId());
        register(event, environment, "service_provider_isolation", SERVICE_PROVIDER_ISOLATION.getId());
        register(event, environment, "snapshot_filtering_and_cap", SNAPSHOT_FILTERING_AND_CAP.getId());
        register(event, environment, "discovery_gated_snapshot", DISCOVERY_GATED_SNAPSHOT.getId());
        register(event, environment, "debug_marker_saved_data_codec", DEBUG_MARKER_SAVED_DATA_CODEC.getId());
        register(event, environment, "terrain_saved_data_codec", TERRAIN_SAVED_DATA_CODEC.getId());
        register(event, environment, "terrain_v3_tile_metadata", TERRAIN_V3_TILE_METADATA.getId());
        register(event, environment, "terrain_palette_determinism", TERRAIN_PALETTE_DETERMINISM.getId());
        register(event, environment, "terrain_scanner_and_request_caps", TERRAIN_SCANNER_AND_REQUEST_CAPS.getId());
        register(event, environment, "waypoint_saved_data_codec", WAYPOINT_SAVED_DATA_CODEC.getId());
        register(event, environment, "waypoint_mutation_rules", WAYPOINT_MUTATION_RULES.getId());
        register(event, environment, "chunk_action_creates_saved_waypoint", CHUNK_ACTION_CREATES_SAVED_WAYPOINT.getId());
        register(event, environment, "waypoint_client_merge", WAYPOINT_CLIENT_MERGE.getId());
        register(event, environment, "deathpoint_personal_visibility", DEATHPOINT_PERSONAL_VISIBILITY.getId());
        register(event, environment, "deathpoint_codec_and_retention", DEATHPOINT_CODEC_AND_RETENTION.getId());
        register(event, environment, "missioncore_content_registration", MISSION_CORE_CONTENT.getId());
        register(event, environment, "theme_core_style_fallback", THEME_CORE_STYLE_FALLBACK.getId());
        register(event, richEnvironment, "rich_provider_snapshot", RICH_PROVIDER_SNAPSHOT.getId());
        register(event, richEnvironment, "builtin_route_hazard_provider_snapshot",
                BUILTIN_ROUTE_HAZARD_PROVIDER_SNAPSHOT.getId());
        register(event, adapterEnvironment, "core_provider_rich_adaptation", CORE_PROVIDER_RICH_ADAPTATION.getId());
        register(event, environment, "client_render_cache_state", CLIENT_RENDER_CACHE_STATE.getId());
        register(event, environment, "zone_packet_round_trip", ZONE_PACKET_ROUND_TRIP.getId());
        register(event, environment, "tactical_polish_helpers", TACTICAL_POLISH_HELPERS.getId());
        register(event, environment, "screencore_holomap_registration", SCREENCORE_HOLOMAP_REGISTRATION.getId());
        register(event, environment, "minimap_enabled_default", MINIMAP_ENABLED_DEFAULT.getId());
    }

    private static void builtinProviderRegistration(GameTestHelper helper) {
        HoloMapService.INSTANCE.clearForTests();
        HoloMapService.INSTANCE.registerBuiltins();
        HoloMapService.INSTANCE.registerBuiltins();
        helper.assertTrue(HoloMapService.INSTANCE.providerCount() == 1,
                "Built-in HoloMap provider should register once");
        List<Identifier> layerIds = HoloMapService.INSTANCE.layers(null).stream().map(IMapLayer::id).toList();
        helper.assertTrue(layerIds.containsAll(requiredLayerIds()),
                "Built-in HoloMap provider should expose every required layer");
        resetHoloMapService();
        helper.succeed();
    }

    private static void themeCoreStyleFallback(GameTestHelper helper) {
        HoloMapSnapshotPacket.MarkerData route = HoloMapSnapshotPacket.MarkerData.from(marker(
                id("theme_route"), HoloMapIds.ROUTES, HoloMapIds.ROUTE_SOURCE,
                IMapMarker.MarkerKind.ROUTE, IMapMarker.MarkerState.DISCOVERED, 0.0D));
        HoloMapSnapshotPacket.MarkerData hazard = HoloMapSnapshotPacket.MarkerData.from(marker(
                id("theme_hazard"), HoloMapIds.HAZARDS, HoloMapIds.HAZARD_SOURCE,
                IMapMarker.MarkerKind.HAZARD, IMapMarker.MarkerState.DISCOVERED, 1.0D));
        HoloMapSnapshotPacket.MarkerData nexus = HoloMapSnapshotPacket.MarkerData.from(marker(
                id("theme_nexus"), HoloMapIds.NEXUS_ANOMALY, HoloMapIds.CORE_SOURCE,
                IMapMarker.MarkerKind.NEXUS_ANOMALY, IMapMarker.MarkerState.DISCOVERED, 2.0D));
        helper.assertTrue(HoloMapVisualStyle.markerColor(null, route) == HoloMapVisualStyle.SUCCESS,
                "HoloMap route marker fallback should remain stable without ThemeCore.");
        helper.assertTrue(HoloMapVisualStyle.markerColor(null, hazard) == HoloMapVisualStyle.DANGER,
                "HoloMap hazard marker fallback should remain stable without ThemeCore.");
        helper.assertTrue(HoloMapVisualStyle.markerColor(null, nexus) == 0xFFFF8FEA,
                "HoloMap Nexus marker fallback should remain stable without ThemeCore.");
        helper.succeed();
    }

    private static void screenCoreHoloMapRegistration(GameTestHelper helper) {
        if (!screenCoreClassesAvailable()) {
            helper.succeed();
            return;
        }
        HoloMapScreenCoreIntegration.register();
        helper.assertTrue(EchoScreenRegistry.componentFactory("holomap-canvas").isPresent(),
                "ScreenCore HoloMap canvas component should be registered");
        helper.assertTrue(EchoScreenRegistry.componentFactory("holomap-mode-button").isPresent(),
                "ScreenCore HoloMap mode button component should be registered");
        helper.assertTrue(EchoScreenRegistry.componentFactory("holomap-virtual-list").isPresent(),
                "ScreenCore HoloMap virtual list component should be registered");
        helper.assertTrue(EchoActionRegistry.action("holomap.center").isPresent(),
                "ScreenCore HoloMap center action should be registered");
        helper.assertTrue(EchoActionRegistry.action("holomap.sync").isPresent(),
                "ScreenCore HoloMap sync action should be registered");
        helper.assertTrue(EchoActionRegistry.action("holomap.close").isPresent(),
                "ScreenCore HoloMap close action should be registered");
        helper.assertTrue(HoloMapScreenCoreIntegration.canvasMeasuredHeightForTests(1080) >= 1080,
                "ScreenCore HoloMap canvas should measure to the available fullscreen height");
        helper.assertTrue(HoloMapScreenCoreIntegration.canvasMeasuredHeightForTests(80) >= 240,
                "ScreenCore HoloMap canvas should enforce its minimum renderable height");
        HoloMapViewState state = new HoloMapViewState("minecraft:overworld", 0, 0, 240, 180,
                0.0D, 0.0D, 1.35D, true, true, "", "", 120, 90, 0.0D, 0.0D, 0.0F);
        helper.assertTrue(HoloMapRenderer.fallbackGridLineCountForTests(state) > 0,
                "ScreenCore HoloMap should have visible grid content before terrain sync completes");
        try {
            Path root = workspaceRoot();
            String page = Files.readString(root.resolve(
                    "addons/echoholomap/src/main/resources/assets/echoholomap/eui/pages/fullscreen_holomap.eui.xml"));
            String style = Files.readString(root.resolve(
                    "addons/echoholomap/src/main/resources/assets/echoholomap/eui/styles/holomap_fullscreen.eui.css"));
            helper.assertTrue(page.contains("styles=\"echothemecore:cyberglass_kit,holomap_fullscreen\"")
                            && page.contains("holomap-fullscreen-canvas")
                            && !page.contains("<inspector-panel")
                            && !page.contains("<app-header")
                            && !page.contains("<app-footer"),
                    "ScreenCore HoloMap page should give the canvas the dominant fullscreen shape");
            helper.assertTrue(style.contains(".holomap-fullscreen-shell")
                            && style.contains("texture-alpha: 34")
                            && style.contains(".holomap-fullscreen-canvas"),
                    "ScreenCore HoloMap fullscreen stylesheet should soften the shell chrome");
        } catch (IOException exception) {
            helper.fail("Could not inspect ScreenCore HoloMap page resources: " + exception.getMessage());
            return;
        }
        helper.succeed();
    }

    private static boolean screenCoreClassesAvailable() {
        if (!ModList.get().isLoaded("echoscreencore")) {
            return false;
        }
        try {
            ClassLoader loader = ModGameTests.class.getClassLoader();
            Class.forName("com.knoxhack.echoscreencore.api.EchoScreenRegistry", false, loader);
            Class.forName("com.knoxhack.echoscreencore.api.action.EchoActionRegistry", false, loader);
            Class.forName("com.knoxhack.echoscreencore.api.component.EchoComponentFactory", false, loader);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static void minimapEnabledDefault(GameTestHelper helper) {
        helper.assertTrue(Boolean.TRUE.equals(Config.MINIMAP_ENABLED.getDefault()),
                "HoloMap minimap should be enabled by default for new client configs");
        helper.succeed();
    }

    private static void clientRenderCacheState(GameTestHelper helper) {
        long terrainRevision = HoloMapTerrainClientState.revision();
        HoloMapTerrainClientState.clear();
        helper.assertTrue(HoloMapTerrainClientState.revision() > terrainRevision,
                "Terrain client cache clear should advance the renderer revision");
        terrainRevision = HoloMapTerrainClientState.revision();
        HoloMapTerrainClientState.apply(new HoloMapTileBatchPacket("minecraft:overworld", 1, 10L,
                List.of(new HoloMapTerrainTile("minecraft:overworld", 0, 0, 10L,
                        HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.SURFACE_SHADED,
                        filledPixels(0xFF224455)))));
        helper.assertTrue(HoloMapTerrainClientState.revision() > terrainRevision,
                "Terrain client cache apply should advance the renderer revision");
        HoloMapTerrainClientState.apply(new HoloMapTileBatchPacket("minecraft:overworld", 1, 11L,
                List.of(new HoloMapTerrainTile("minecraft:overworld", 3, 3, 11L,
                                HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.SURFACE_SHADED,
                                filledPixels(0xFF224466)),
                        new HoloMapTerrainTile("minecraft:overworld", 60, 60, 11L,
                                HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.SURFACE_SHADED,
                                filledPixels(0xFF224477)))));
        helper.assertTrue(HoloMapTerrainClientState.tiles("minecraft:overworld", 3, 3, 3, 3).size() == 1,
                "Terrain client visible-range lookup should return only requested chunk keys");
        helper.assertTrue(HoloMapTerrainClientState.hasRenderableTile("minecraft:overworld", 3, 3),
                "Terrain client helper should report cached current terrain as renderable");
        helper.assertFalse(HoloMapTerrainClientState.hasRenderableTile("minecraft:overworld", 4, 3),
                "Terrain client helper should reject missing chunks");
        helper.assertTrue(HoloMapTerrainClientState.tiles("minecraft:the_nether", 3, 3, 3, 3).isEmpty(),
                "Terrain client visible-range lookup should stay dimension-scoped");

        long waypointRevision = HoloMapWaypointClientState.revision();
        HoloMapWaypoint waypoint = HoloMapWaypoint.create(Scope.LOCAL, UUID.randomUUID(), "minecraft:overworld",
                8.0D, 64.0D, 8.0D, "Render waypoint", 0xFF92F7A6, 10L);
        HoloMapWaypointClientState.setLocalWaypoints(List.of(waypoint));
        helper.assertTrue(HoloMapWaypointClientState.revision() > waypointRevision,
                "Waypoint client changes should advance the renderer revision");

        HoloMapViewState state = new HoloMapViewState("minecraft:overworld", 0, 0, 240, 180,
                0.0D, 0.0D, 1.35D, true, true, "", "", 0, 0, 0.0D, 0.0D, 0.0F);
        HoloMapViewState hoverMoved = new HoloMapViewState("minecraft:overworld", 0, 0, 240, 180,
                0.0D, 0.0D, 1.35D, true, true, "", "", 180, 120, 0.0D, 0.0D, 0.0F);
        HoloMapViewState moved = new HoloMapViewState("minecraft:overworld", 0, 0, 240, 180,
                32.0D, 0.0D, 1.35D, true, true, "", "", 0, 0, 0.0D, 0.0D, 0.0F);
        HoloMapViewState zoomed = new HoloMapViewState("minecraft:overworld", 0, 0, 240, 180,
                0.0D, 0.0D, 2.75D, true, true, "", "", 0, 0, 0.0D, 0.0D, 0.0F);
        int baseKey = HoloMapRenderer.cacheFingerprintForTests(state, HoloMapSnapshotPacket.empty(), List.of(),
                HoloMapWaypointClientState.waypoints(), HoloMapRenderer.FULLSCREEN_BUDGET);
        helper.assertTrue(baseKey == HoloMapRenderer.cacheFingerprintForTests(hoverMoved,
                        HoloMapSnapshotPacket.empty(), List.of(), HoloMapWaypointClientState.waypoints(),
                        HoloMapRenderer.FULLSCREEN_BUDGET),
                "Renderer cache key should ignore raw mouse movement");
        HoloMapRenderer.clearTerrainStyleCacheForTests();
        HoloMapRenderer renderer = new HoloMapRenderer();
        renderer.prepareModelForTests(state, HoloMapSnapshotPacket.empty(), List.of(),
                HoloMapWaypointClientState.waypoints(), HoloMapRenderer.FULLSCREEN_BUDGET);
        int modelBuilds = renderer.modelBuildsForTests();
        long terrainStyleBuilds = HoloMapRenderer.terrainStyleCacheBuildsForTests();
        helper.assertTrue(modelBuilds == 1 && terrainStyleBuilds > 0,
                "Renderer model preparation should build one cached model and styled terrain cache");
        renderer.prepareModelForTests(hoverMoved, HoloMapSnapshotPacket.empty(), List.of(),
                HoloMapWaypointClientState.waypoints(), HoloMapRenderer.FULLSCREEN_BUDGET);
        helper.assertTrue(renderer.modelBuildsForTests() == modelBuilds
                        && HoloMapRenderer.terrainStyleCacheBuildsForTests() == terrainStyleBuilds,
                "Mouse-only movement should reuse the render model and terrain style cache");
        HoloMapTerrainClientState.clear();
        List<HoloMapTerrainTile> stableTiles = new ArrayList<>();
        for (int i = 0; i < 48; i++) {
            stableTiles.add(new HoloMapTerrainTile("minecraft:overworld",
                    i % 33 - 16, i / 33 - 8, 80L + i,
                    HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.SURFACE_SHADED,
                    filledPixels(0xFF445566 + i)));
        }
        HoloMapTerrainClientState.apply(new HoloMapTileBatchPacket("minecraft:overworld",
                stableTiles.size(), 120L, stableTiles));
        HoloMapViewState largeTerrainState = new HoloMapViewState("minecraft:overworld", 0, 0, 1600, 960,
                0.0D, 0.0D, 1.0D, true, true, "", "", 0, 0, 0.0D, 0.0D, 0.0F);
        HoloMapRenderer terrainRenderer = new HoloMapRenderer();
        HoloMapRenderer.RenderResult idleTerrain = terrainRenderer.prepareModelForTests(largeTerrainState,
                HoloMapSnapshotPacket.empty(), List.of(), List.of(), HoloMapRenderer.FULLSCREEN_BUDGET);
        int terrainModelBuilds = terrainRenderer.terrainModelBuildsForTests();
        HoloMapRenderer.RenderResult dragTerrain = terrainRenderer.prepareModelForTests(largeTerrainState,
                HoloMapSnapshotPacket.empty(), List.of(), List.of(), HoloMapRenderer.FULLSCREEN_INTERACTIVE_BUDGET);
        HoloMapViewState smallPanTerrainState = new HoloMapViewState("minecraft:overworld", 0, 0, 1600, 960,
                8.0D, 0.0D, 1.0D, true, true, "", "", 0, 0, 0.0D, 0.0D, 0.0F);
        HoloMapRenderer.RenderResult smallPanTerrain = terrainRenderer.prepareModelForTests(smallPanTerrainState,
                HoloMapSnapshotPacket.empty(),
                List.of(), List.of(), HoloMapRenderer.FULLSCREEN_BUDGET);
        helper.assertTrue(idleTerrain.terrainTiles() == stableTiles.size()
                        && dragTerrain.terrainTiles() == stableTiles.size()
                        && smallPanTerrain.terrainTiles() == stableTiles.size()
                        && idleTerrain.culledTerrainTiles() == 0
                        && dragTerrain.culledTerrainTiles() == 0
                        && smallPanTerrain.culledTerrainTiles() == 0
                        && terrainRenderer.terrainModelBuildsForTests() <= terrainModelBuilds + 1,
                "Fullscreen drag and small pans should reuse synced real terrain tiles");
        helper.assertTrue(HoloMapRenderer.terrainStatusLabel(
                        new HoloMapRenderer.RenderResult(96, 32, 0, 0, 0, 0, List.of(), List.of(), false),
                        240).equals("96 rendered / 128 visible synced / 240 remembered real chunks"),
                "Terrain status should distinguish rendered tiles from synced real terrain tiles");
        HoloMapTerrainClientState.clear();
        helper.assertTrue(baseKey != HoloMapRenderer.cacheFingerprintForTests(moved, HoloMapSnapshotPacket.empty(),
                        List.of(), HoloMapWaypointClientState.waypoints(), HoloMapRenderer.FULLSCREEN_BUDGET),
                "Renderer cache key should change when the viewport bucket changes");
        renderer.prepareModelForTests(moved, HoloMapSnapshotPacket.empty(), List.of(),
                HoloMapWaypointClientState.waypoints(), HoloMapRenderer.FULLSCREEN_BUDGET);
        helper.assertTrue(renderer.modelBuildsForTests() == modelBuilds + 1,
                "Viewport movement should rebuild the cached render model");
        helper.assertTrue(baseKey != HoloMapRenderer.cacheFingerprintForTests(zoomed, HoloMapSnapshotPacket.empty(),
                        List.of(), HoloMapWaypointClientState.waypoints(), HoloMapRenderer.FULLSCREEN_BUDGET),
                "Renderer cache key should change when the zoom bucket changes");
        HoloMapSnapshotPacket laterSnapshot = new HoloMapSnapshotPacket(List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), "later", 99L);
        helper.assertTrue(baseKey != HoloMapRenderer.cacheFingerprintForTests(state, laterSnapshot, List.of(),
                        HoloMapWaypointClientState.waypoints(), HoloMapRenderer.FULLSCREEN_BUDGET),
                "Renderer cache key should change when the snapshot game time changes");

        HoloMapSnapshotPacket.MarkerData near = HoloMapSnapshotPacket.MarkerData.from(marker(
                id("render_near"), HoloMapIds.MISSIONS, HoloMapIds.CORE_SOURCE,
                IMapMarker.MarkerKind.MISSION, IMapMarker.MarkerState.DISCOVERED, 4.0D));
        HoloMapSnapshotPacket.MarkerData far = HoloMapSnapshotPacket.MarkerData.from(marker(
                id("render_far"), HoloMapIds.MISSIONS, HoloMapIds.CORE_SOURCE,
                IMapMarker.MarkerKind.MISSION, IMapMarker.MarkerState.DISCOVERED, 400.0D));
        helper.assertTrue(HoloMapRenderer.selectedMarkerIdsForTests(state, List.of(far, near), 1)
                        .equals(List.of(near.id())),
                "Renderer marker budget should keep nearer high-priority markers first");
        HoloMapViewState selectedState = new HoloMapViewState("minecraft:overworld", 0, 0, 240, 180,
                0.0D, 0.0D, 1.35D, true, true, far.id().toString(), "", 0, 0, 0.0D, 0.0D, 0.0F);
        helper.assertTrue(baseKey != HoloMapRenderer.cacheFingerprintForTests(selectedState,
                        HoloMapSnapshotPacket.empty(), List.of(), HoloMapWaypointClientState.waypoints(),
                        HoloMapRenderer.FULLSCREEN_BUDGET),
                "Renderer cache key should change when marker selection changes");
        helper.assertTrue(HoloMapRenderer.selectedMarkerIdsForTests(selectedState, List.of(near, far), 1)
                        .equals(List.of(far.id())),
                "Renderer marker budget should keep selected markers before nearer unselected markers");
        List<Identifier> zoneIds = HoloMapRenderer.selectedZoneIdsForTests(state, List.of(
                        zone(id("zone/low_near"), 8.0D, 8.0D, 20),
                        zone(id("zone/high_near"), 40.0D, 40.0D, 90),
                        zone(id("zone/mid_near"), 16.0D, 16.0D, 50)), 2);
        helper.assertTrue(zoneIds.equals(List.of(id("zone/high_near"), id("zone/mid_near"))),
                "Renderer zone budget should keep higher-priority zones within the LOD cap");
        HoloMapViewState farCamera = new HoloMapViewState("minecraft:overworld", 0, 0, 240, 180,
                400.0D, 0.0D, 1.35D, true, true, "", "", 0, 0, 0.0D, 0.0D, 0.0F);
        HoloMapSnapshotPacket.ZoneData farField = zone(id("zone/far_auto_field"), 400.0D, 0.0D, 90);
        helper.assertTrue(HoloMapRenderer.selectedZoneIdsForTests(farCamera, List.of(farField), 4).isEmpty(),
                "Auto-near field mode should skip far field draw plans even when the map camera is panned to them");
        helper.assertTrue(HoloMapRenderer.selectedZoneIdsForTests(
                        farCamera, List.of(farField), 4, HoloMapVisibility.FieldMode.ALL)
                        .equals(List.of(farField.id())),
                "All field mode should still permit explicitly drawing far discovered fields");
        helper.assertTrue(HoloMapRenderer.edgeIndicatorSectorLimitForTests(96) == 2
                        && HoloMapRenderer.edgeIndicatorSectorLimitForTests(256) == 4,
                "Marker edge indicators should use bounded per-sector caps");

        helper.assertFalse(HoloMapRenderer.highDetailTerrainAllowed(2.75D, 12, false),
                "High-detail terrain should be disabled by default");
        helper.assertTrue(HoloMapRenderer.highDetailTerrainAllowed(2.75D, 64, true),
                "High-detail terrain should be allowed only when enabled, zoomed in, and within tile budget");
        helper.assertFalse(HoloMapRenderer.highDetailTerrainAllowed(2.0D, 64, true),
                "High-detail terrain should stay off below the zoom threshold");
        helper.assertFalse(HoloMapRenderer.highDetailTerrainAllowed(2.75D, 65, true),
                "High-detail terrain should stay off above the tile threshold");
        HoloMapRenderer.clearTerrainStyleCacheForTests();
        HoloMapTerrainTile cacheTile = new HoloMapTerrainTile("minecraft:overworld", 9, 9, 42L,
                HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.SURFACE_SHADED,
                filledPixels(0xFF315966));
        int averageA = HoloMapRenderer.styledTerrainAverageForTests(cacheTile);
        int averageB = HoloMapRenderer.styledTerrainAverageForTests(cacheTile);
        helper.assertTrue(averageA == averageB
                        && HoloMapRenderer.terrainStyleCacheEntriesForTests() == 1
                        && HoloMapRenderer.terrainStyleCacheBuildsForTests() == 1L,
                "Terrain render cache should reuse styled average and pixel colors for unchanged tiles");
        HoloMapSnapshotPacket.MarkerData openClientMarker = markerData(id("marker/client_open"),
                IMapMarker.MarkerKind.HAZARD, "Client Open", 4.0D);
        HoloMapSnapshotPacket.MarkerData lockedClientMarker = new HoloMapSnapshotPacket.MarkerData(
                id("marker/client_locked"), HoloMapIds.HAZARDS, HoloMapIds.HAZARD_SOURCE,
                IMapMarker.MarkerKind.HAZARD, IMapMarker.MarkerState.LOCKED, "Client Locked", "",
                Level.OVERWORLD.identifier().toString(), 8.0D, 64.0D, 8.0D, 32.0F, null, "",
                -1, HoloMapPrecision.ESTIMATED, 50);
        HoloMapSnapshotPacket.ZoneData openClientZone = zone(id("zone/client_open"), 4.0D, 4.0D, 50);
        HoloMapSnapshotPacket.ZoneData lockedClientZone = HoloMapSnapshotPacket.ZoneData.from(HoloMapZoneData.circle(
                id("zone/client_locked"), HoloMapIds.HAZARDS, HoloMapIds.HAZARD_SOURCE,
                HoloMapZonePattern.SOLID, IMapMarker.MarkerState.LOCKED, "Client Locked Zone", "",
                Level.OVERWORLD, 8.0D, 64.0D, 8.0D, 32.0F, 0x335CDAFF, 0xAA5CDAFF,
                HoloMapPrecision.ESTIMATED, 50));
        HoloMapClientState.apply(new HoloMapSnapshotPacket(List.of(), List.of(openClientMarker, lockedClientMarker),
                List.of(), List.of(), List.of(openClientZone, lockedClientZone), List.of(), "client test", 77L));
        helper.assertTrue(HoloMapClientState.markersForDimension("minecraft:overworld").size() == 1
                        && HoloMapClientState.markersForDimension("minecraft:overworld").getFirst().id().equals(openClientMarker.id())
                        && HoloMapClientState.zonesForDimension("minecraft:overworld").size() == 1
                        && HoloMapClientState.zonesForDimension("minecraft:overworld").getFirst().id().equals(openClientZone.id()),
                "Client snapshot index should defensively hard-hide locked normal-view content");
        HoloMapClientState.apply(HoloMapSnapshotPacket.empty());
        HoloMapWaypointClientState.clearForTests();
        HoloMapTerrainClientState.clear();
        helper.succeed();
    }

    private static void richProviderSnapshot(GameTestHelper helper) {
        HoloMapService.INSTANCE.clearForTests();
        EchoCoreServices.registerMapMarkerService(HoloMapService.INSTANCE);
        Identifier providerId = id("provider/rich");
        Identifier duplicateProviderId = providerId;
        Identifier layerId = id("layer/rich");
        Identifier routeId = id("route/rich");
        Identifier overlayId = id("overlay/rich");
        boolean registered = HoloMapService.INSTANCE.registerHoloProvider(new IHoloMapDataProvider() {
            @Override
            public Identifier providerId() {
                return providerId;
            }

            @Override
            public List<HoloMapLayerData> layers(Player player) {
                return List.of(new HoloMapLayerData(layerId, "Rich Layer", 333, 0xFF38DFF4, true));
            }

            @Override
            public List<HoloMapMarkerData> markers(Player player) {
                return List.of(new HoloMapMarkerData(
                        id("marker/rich"),
                        layerId,
                        providerId,
                        IMapMarker.MarkerKind.MISSION,
                        IMapMarker.MarkerState.DISCOVERED,
                        "Rich Marker",
                        "Rich provider marker.",
                        Level.OVERWORLD,
                        4.0D,
                        64.0D,
                        6.0D,
                        0.0F,
                        null,
                        null,
                        -1,
                        HoloMapPrecision.PRECISE,
                        25));
            }

            @Override
            public List<HoloMapRouteData> routes(Player player) {
                return List.of(new HoloMapRouteData(routeId, layerId, providerId, "Rich Route", "",
                        Level.OVERWORLD, 0xFF92F7A6, IMapMarker.MarkerState.DISCOVERED,
                        List.of(new HoloMapRoutePoint(Level.OVERWORLD, 1.0D, 64.0D, 1.0D, 0, "A",
                                        HoloMapPrecision.PRECISE),
                                new HoloMapRoutePoint(Level.OVERWORLD, 8.0D, 64.0D, 8.0D, 1, "B",
                                        HoloMapPrecision.PRECISE))));
            }

            @Override
            public List<HoloMapOverlayData> overlays(Player player) {
                return List.of(new HoloMapOverlayData(overlayId, layerId, providerId, HoloMapOverlayKind.SCAN,
                        IMapMarker.MarkerState.DISCOVERED, "Rich Overlay", "",
                        Level.OVERWORLD, 5.0D, 64.0D, 5.0D, 32.0F, 0x6638DFF4, HoloMapPrecision.ESTIMATED));
            }

            @Override
            public List<HoloMapZoneData> zones(Player player) {
                return List.of(new HoloMapZoneData(id("zone/rich"), layerId, providerId,
                        HoloMapZoneShape.POLYGON, HoloMapZonePattern.SCAN_GRID,
                        IMapMarker.MarkerState.DISCOVERED, "Rich Zone", "",
                        Level.OVERWORLD, 6.0D, 64.0D, 6.0D, 24.0F, 48.0F, 48.0F,
                        0x3338DFF4, 0xAA38DFF4, HoloMapPrecision.PRECISE, 80,
                        List.of(new HoloMapZonePoint(Level.OVERWORLD, 2.0D, 64.0D, 2.0D, 2),
                                new HoloMapZonePoint(Level.OVERWORLD, 10.0D, 64.0D, 2.0D, 0),
                                new HoloMapZonePoint(Level.OVERWORLD, 6.0D, 64.0D, 10.0D, 1))));
            }
        });
        helper.assertTrue(registered, "Rich HoloMap provider should register");
        helper.assertFalse(HoloMapService.INSTANCE.registerProvider(new IMapDataProvider() {
            @Override
            public Identifier providerId() {
                return duplicateProviderId;
            }
        }), "Duplicate provider ids should be rejected across Core and rich providers");

        HoloMapSnapshotPacket snapshot = HoloMapSnapshotPacket.from(helper.makeMockServerPlayerInLevel());
        helper.assertTrue(HoloMapService.INSTANCE.richProviderCount() == 1, "Rich provider count should be tracked");
        helper.assertTrue(snapshot.layers().stream().anyMatch(layer -> layer.id().equals(layerId)),
                "Rich provider layer should be present in snapshots");
        helper.assertTrue(snapshot.markers().stream().anyMatch(marker -> marker.id().equals(id("marker/rich"))
                        && marker.precision() == HoloMapPrecision.PRECISE),
                "Rich provider marker and precision should be present in snapshots");
        helper.assertTrue(snapshot.routes().stream().anyMatch(route -> route.id().equals(routeId)
                        && route.points().size() == 2),
                "Rich provider routes should be present in snapshots");
        helper.assertTrue(snapshot.overlays().stream().anyMatch(overlay -> overlay.id().equals(overlayId)
                        && overlay.kind() == HoloMapOverlayKind.SCAN),
                "Rich provider overlays should be present in snapshots");
        helper.assertTrue(snapshot.zones().stream().anyMatch(zone -> zone.id().equals(id("zone/rich"))
                        && zone.shape() == HoloMapZoneShape.POLYGON
                        && zone.points().getFirst().order() == 0),
                "Rich provider zones should be present in snapshots with pre-sorted points");
        helper.assertTrue(snapshot.zones().stream().anyMatch(zone -> zone.id().toString().contains("overlay/rich")
                        && zone.shape() == HoloMapZoneShape.CIRCLE
                        && zone.pattern() == HoloMapZonePattern.SCAN_GRID),
                "Legacy radius overlays should be converted into compatible circle zones");
        helper.assertTrue(snapshot.diagnostics().stream().anyMatch(diagnostic -> diagnostic.providerId().equals(providerId)
                        && diagnostic.healthy()),
                "Rich provider diagnostics should report healthy providers");
        resetHoloMapService();
        helper.succeed();
    }

    private static void builtinRouteHazardProviderSnapshot(GameTestHelper helper) {
        EchoCoreServices.clearPlatformServicesForTests();
        HoloMapService.INSTANCE.clearForTests();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Identifier regionId = id("world/route_hazard_region");
        Identifier hazardId = id("hazard/runtime_proof");
        EchoCoreServices.registerWorldRegionService(new IWorldRegionService() {
            @Override
            public boolean registerRegionDefinition(WorldRegionDefinition definition) {
                return false;
            }

            @Override
            public List<WorldRegionDefinition> regionDefinitions() {
                return List.of();
            }

            @Override
            public Optional<WorldRegionDefinition> regionDefinition(Identifier id) {
                return Optional.empty();
            }

            @Override
            public List<WorldRegionInstance> nearbyRegions(Level level, BlockPos pos, int radius) {
                return List.of(region(pos));
            }

            @Override
            public List<WorldRegionInstance> activeRegions(Player player) {
                return List.of(region(player.blockPosition()));
            }

            @Override
            public boolean registerHazardDefinition(WorldHazardDefinition definition) {
                return false;
            }

            @Override
            public List<WorldHazardDefinition> hazardDefinitions() {
                return List.of();
            }

            @Override
            public Optional<WorldHazardDefinition> hazardDefinition(Identifier id) {
                return Optional.empty();
            }

            @Override
            public WorldHazardSnapshot hazardSnapshot(Player player) {
                return new WorldHazardSnapshot(List.of(regionId), List.of(hazardId), 72, false,
                        "Runtime proof hazard from WorldCore snapshot state.");
            }

            @Override
            public WorldMarker revealMarker(ServerPlayer player, WorldMarker marker) {
                return marker;
            }

            @Override
            public WorldMarker revealMarker(Level level, WorldMarker marker) {
                return marker;
            }

            @Override
            public List<WorldMarker> nearbyMarkers(Level level, BlockPos pos, int radius) {
                return List.of(
                        marker(pos, "start", WorldMarkerType.ROUTE_START, 8, 0, 24),
                        marker(pos, "checkpoint", WorldMarkerType.ROUTE_CHECKPOINT, 32, 0, 24),
                        marker(pos, "destination", WorldMarkerType.ROUTE_DESTINATION, 56, 0, 24),
                        marker(pos, "hazard", WorldMarkerType.HAZARD, 24, 0, 56));
            }

            @Override
            public List<WorldMarker> markers(Player player) {
                return nearbyMarkers(player.level(), player.blockPosition(), Config.mapInterestRadiusBlocks());
            }

            @Override
            public List<String> validateMarkers(Level level) {
                return List.of();
            }

            @Override
            public boolean recordStructureScan(ServerPlayer player, Identifier structureId, BlockPos pos,
                    String displayName, String summary) {
                return false;
            }

            @Override
            public boolean recordStructureEntry(ServerPlayer player, Identifier structureId, BlockPos pos,
                    String displayName, String summary) {
                return false;
            }

            @Override
            public boolean hasDiscoveredRegion(Player player, Identifier regionId) {
                return true;
            }

            @Override
            public Set<Identifier> discoveredRegions(Player player) {
                return Set.of(regionId);
            }

            @Override
            public boolean discoverRegion(ServerPlayer player, Identifier regionId, WorldDiscoverySource source) {
                return true;
            }

            @Override
            public Optional<WorldRegionInstance> currentRegion(Player player) {
                return Optional.of(region(player.blockPosition()));
            }

            @Override
            public void tickPlayer(ServerPlayer player) {
            }

            private WorldRegionInstance region(BlockPos base) {
                return new WorldRegionInstance(regionId, regionId, WorldRegionType.RADIATION_ZONE,
                        "Runtime Proof Hazard Region", Level.OVERWORLD, base.offset(24, 0, 24), 96,
                        List.of(hazardId), true);
            }

            private WorldMarker marker(BlockPos base, String path, WorldMarkerType type, int dx, int dy, int dz) {
                return new WorldMarker(id("world/route_hazard/" + path), regionId, type,
                        "Runtime Proof " + path, "WorldCore marker used by HoloMap route/hazard proof.",
                        Level.OVERWORLD, base.offset(dx, dy, dz), type == WorldMarkerType.HAZARD ? 80 : 24,
                        true, 42L);
            }
        });
        EchoCoreServices.registerRouteRecordService(routePlayer -> List.of(new EchoRouteRecord(
                id("route/chapter_runtime_proof"), EchoHoloMap.MODID, "Chapter Runtime Route",
                "Route", Level.OVERWORLD.identifier().toString(), "ACTIVE",
                "Chapter route records should still feed HoloMap routes.", false)));
        EchoCoreServices.registerHazardTelemetryService(hazardPlayer -> new EchoHazardTelemetry(
                20, 75, 0, 100, 100, 0, 0, 0, "Runtime proof live hazard telemetry."));
        try {
            HoloMapService.INSTANCE.registerBuiltins();
            helper.assertTrue(HoloMapService.INSTANCE.registerHoloProvider(BuiltinHoloMapRouteHazardProvider.INSTANCE),
                    "Built-in HoloMap route/hazard provider should register as a rich provider.");
            EchoCoreServices.registerMapMarkerService(HoloMapService.INSTANCE);

            HoloMapSnapshotPacket snapshot = HoloMapSnapshotPacket.from(player);
            helper.assertTrue(snapshot.routes().stream().anyMatch(route -> route.id().equals(regionId)
                            && route.sourceId().equals(HoloMapIds.WORLD_SOURCE)
                            && route.points().size() == 3),
                    "WorldCore route markers should produce an explicit HoloMap route snapshot.");
            helper.assertTrue(snapshot.overlays().stream().anyMatch(overlay -> HoloMapIds.WORLD_SOURCE.equals(overlay.sourceId())
                            && HoloMapIds.HAZARDS.equals(overlay.layerId())
                            && overlay.kind() == HoloMapOverlayKind.HAZARD),
                    "WorldCore hazard markers/regions should produce explicit HoloMap hazard overlays.");
            helper.assertTrue(snapshot.overlays().stream().anyMatch(overlay -> HoloMapIds.HAZARD_SOURCE.equals(overlay.sourceId())
                            && overlay.kind() == HoloMapOverlayKind.HAZARD
                            && overlay.summary().contains("Runtime proof")),
                    "Runtime hazard telemetry/snapshot state should produce a HoloMap hazard overlay.");
            helper.assertTrue(snapshot.zones().stream().anyMatch(zone -> HoloMapIds.WORLD_SOURCE.equals(zone.sourceId())
                            && zone.shape() == HoloMapZoneShape.CORRIDOR
                            && zone.pattern() == HoloMapZonePattern.ROUTE_BANDS),
                    "WorldCore routes should produce explicit HoloMap route corridor zones.");
            helper.assertTrue(snapshot.diagnostics().stream().anyMatch(diagnostic ->
                            BuiltinHoloMapRouteHazardProvider.INSTANCE.providerId().equals(diagnostic.providerId())
                                    && diagnostic.healthy()
                                    && diagnostic.routes() > 0
                                    && diagnostic.overlays() > 0),
                    "Built-in route/hazard provider diagnostics should report real route and overlay payloads.");
        } finally {
            EchoCoreServices.clearPlatformServicesForTests();
            EchoCoreServices.registerWorldRegionService(null);
            resetHoloMapService();
        }
        helper.succeed();
    }

    private static void coreProviderRichAdaptation(GameTestHelper helper) {
        HoloMapService.INSTANCE.clearForTests();
        EchoCoreServices.registerMapMarkerService(HoloMapService.INSTANCE);
        Identifier providerId = id("provider/core_adapter");
        Identifier routeId = id("route/core_adapter");
        HoloMapService.INSTANCE.registerProvider(new IMapDataProvider() {
            @Override
            public Identifier providerId() {
                return providerId;
            }

            @Override
            public List<IMapLayer> layers(Player player) {
                return List.of(new EchoMapLayer(HoloMapIds.ROUTES, "Routes", 20, 0xFF92F7A6, true),
                        new EchoMapLayer(HoloMapIds.HAZARDS, "Hazards", 30, 0xFFFF5C7A, true));
            }

            @Override
            public List<IMapMarker> markers(Player player) {
                return List.of(
                        new EchoMapMarker(id("marker/route_b"), HoloMapIds.ROUTES, providerId,
                                IMapMarker.MarkerKind.ROUTE, IMapMarker.MarkerState.DISCOVERED,
                                "Route B", "", Level.OVERWORLD, 20.0D, 64.0D, 20.0D,
                                0.0F, null, routeId, 1, true),
                        new EchoMapMarker(id("marker/route_a"), HoloMapIds.ROUTES, providerId,
                                IMapMarker.MarkerKind.ROUTE, IMapMarker.MarkerState.DISCOVERED,
                                "Route A", "", Level.OVERWORLD, 10.0D, 64.0D, 10.0D,
                                0.0F, null, routeId, 0, true),
                        new EchoMapMarker(id("marker/estimated"), HoloMapIds.HAZARDS, providerId,
                                IMapMarker.MarkerKind.HAZARD, IMapMarker.MarkerState.DISCOVERED,
                                "Estimated", "", Level.OVERWORLD, 4.0D, 64.0D, 4.0D,
                                64.0F, null, null, -1, false),
                        new EchoMapMarker(id("marker/virtual"), HoloMapIds.DRONES_SCANS,
                                HoloMapIds.DISCOVERY_SOURCE, IMapMarker.MarkerKind.DRONE_SCAN,
                                IMapMarker.MarkerState.DISCOVERED, "Virtual", "", Level.OVERWORLD,
                                40.0D, 64.0D, 40.0D, 0.0F, null, null, -1, false));
            }
        });

        HoloMapSnapshotPacket snapshot = HoloMapSnapshotPacket.from(helper.makeMockServerPlayerInLevel());
        HoloMapSnapshotPacket.RouteData route = snapshot.routes().stream()
                .filter(candidate -> candidate.sourceId().equals(providerId) && candidate.points().size() == 2)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Core route markers should synthesize a rich route; routes="
                        + snapshot.routes() + " markers="
                        + snapshot.markers().stream().map(marker -> marker.id() + ":" + marker.routeId()).toList()));
        helper.assertTrue(route.id().toString().contains("core_adapter"),
                "Synthesized Core routes should retain a recognizable route id");
        helper.assertTrue(route.points().size() == 2, "Core route markers should synthesize route points");
        helper.assertTrue(route.points().getFirst().order() == 0 && route.points().getFirst().x() == 10.0D,
                "Core route points should sort by routeOrder");
        helper.assertTrue(snapshot.overlays().stream().anyMatch(overlay -> overlay.id().toString().contains("estimated")
                        && overlay.kind() == HoloMapOverlayKind.HAZARD
                        && overlay.precision() == HoloMapPrecision.ESTIMATED),
                "Core radius markers should synthesize estimated overlays");
        helper.assertTrue(snapshot.markers().stream().anyMatch(marker -> marker.id().equals(id("marker/virtual"))
                        && marker.precision() == HoloMapPrecision.VIRTUAL),
                "Synthetic discovery source markers should map to virtual precision");
        resetHoloMapService();
        helper.succeed();
    }

    private static void zonePacketRoundTrip(GameTestHelper helper) {
        HoloMapSnapshotPacket.ZoneData circle = zone(id("zone/packet_circle"), 4.0D, 4.0D, 90);
        HoloMapSnapshotPacket.ZoneData polygon = HoloMapSnapshotPacket.ZoneData.from(new HoloMapZoneData(
                id("zone/packet_polygon"),
                HoloMapIds.ORBITAL_SCANS,
                id("provider/zones"),
                HoloMapZoneShape.POLYGON,
                HoloMapZonePattern.SCAN_GRID,
                IMapMarker.MarkerState.DISCOVERED,
                "Packet Polygon",
                "",
                Level.OVERWORLD,
                16.0D,
                64.0D,
                16.0D,
                12.0F,
                64.0F,
                64.0F,
                0x3324CCFF,
                0xAA24CCFF,
                HoloMapPrecision.PRECISE,
                70,
                List.of(new HoloMapZonePoint(Level.OVERWORLD, 24.0D, 64.0D, 12.0D, 2),
                        new HoloMapZonePoint(Level.OVERWORLD, 12.0D, 64.0D, 12.0D, 0),
                        new HoloMapZonePoint(Level.OVERWORLD, 18.0D, 64.0D, 24.0D, 1))));
        HoloMapSnapshotPacket.ZoneData corridor = HoloMapSnapshotPacket.ZoneData.from(new HoloMapZoneData(
                id("zone/packet_corridor"),
                HoloMapIds.ROUTES,
                id("provider/zones"),
                HoloMapZoneShape.CORRIDOR,
                HoloMapZonePattern.ROUTE_BANDS,
                IMapMarker.MarkerState.LOCKED,
                "Packet Corridor",
                "",
                Level.OVERWORLD,
                0.0D,
                64.0D,
                0.0D,
                9.0F,
                18.0F,
                18.0F,
                0x3392F7A6,
                0xAA92F7A6,
                HoloMapPrecision.ESTIMATED,
                60,
                List.of(new HoloMapZonePoint(Level.OVERWORLD, 0.0D, 64.0D, 0.0D, 0),
                        new HoloMapZonePoint(Level.OVERWORLD, 48.0D, 64.0D, 0.0D, 1),
                        new HoloMapZonePoint(Level.OVERWORLD, 64.0D, 64.0D, 24.0D, 2))));
        HoloMapSnapshotPacket packet = new HoloMapSnapshotPacket(List.of(), List.of(), List.of(), List.of(),
                List.of(circle, polygon, corridor), List.of(), "zones", 42L);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        HoloMapSnapshotPacket decoded;
        try {
            HoloMapSnapshotPacket.CODEC.encode(buffer, packet);
            decoded = HoloMapSnapshotPacket.CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
        helper.assertTrue(decoded.zones().size() == 3, "Zone packet round-trip should preserve zone count");
        HoloMapSnapshotPacket.ZoneData decodedCircle = decoded.zones().stream()
                .filter(zone -> zone.id().equals(circle.id()))
                .findFirst()
                .orElseThrow();
        HoloMapSnapshotPacket.ZoneData decodedPolygon = decoded.zones().stream()
                .filter(zone -> zone.id().equals(polygon.id()))
                .findFirst()
                .orElseThrow();
        HoloMapSnapshotPacket.ZoneData decodedCorridor = decoded.zones().stream()
                .filter(zone -> zone.id().equals(corridor.id()))
                .findFirst()
                .orElseThrow();
        helper.assertTrue(decodedCircle.shape() == HoloMapZoneShape.CIRCLE && decodedCircle.radius() == 32.0F,
                "Circle zones should survive packet round-trip");
        helper.assertTrue(decodedPolygon.shape() == HoloMapZoneShape.POLYGON
                        && decodedPolygon.pattern() == HoloMapZonePattern.SCAN_GRID
                        && decodedPolygon.points().getFirst().order() == 0,
                "Polygon zones should survive packet round-trip with sorted points");
        helper.assertTrue(decodedCorridor.shape() == HoloMapZoneShape.CORRIDOR
                        && decodedCorridor.pattern() == HoloMapZonePattern.ROUTE_BANDS
                        && decodedCorridor.state() == IMapMarker.MarkerState.LOCKED,
                "Corridor zones should survive packet round-trip");
        HoloMapClientState.apply(decoded);
        List<HoloMapSnapshotPacket.ZoneData> indexedZones =
                HoloMapClientState.snapshotForDimension("minecraft:overworld").zones();
        helper.assertTrue(indexedZones.size() == 2
                        && indexedZones.stream().noneMatch(zone -> zone.id().equals(corridor.id())),
                "Client snapshot index should hard-hide locked zones in normal map views");
        helper.assertTrue(HoloMapClientState.snapshotForDimension("minecraft:the_nether").zones().isEmpty(),
                "Client snapshot index should not expose zones from unrelated dimensions");
        HoloMapClientState.apply(HoloMapSnapshotPacket.empty());
        helper.succeed();
    }

    private static void tacticalPolishHelpers(GameTestHelper helper) {
        HoloMapViewState state = new HoloMapViewState("minecraft:overworld", 0, 0, 240, 180,
                0.0D, 0.0D, 1.0D, true, true, "", "", 0, 0, 0.0D, 0.0D, 0.0F);
        HoloMapSnapshotPacket.ZoneData circle = zone(id("zone/polish_circle"), 0.0D, 0.0D, 50);
        helper.assertTrue(HoloMapRenderer.zoneHitForTests(state, circle,
                        state.worldToScreenX(0.0D), state.worldToScreenZ(0.0D)),
                "Circle zone hit testing should use true radius hit detection");
        helper.assertFalse(HoloMapRenderer.zoneHitForTests(state, circle,
                        state.worldToScreenX(80.0D), state.worldToScreenZ(0.0D)),
                "Circle zone hit testing should reject points outside the radius");

        HoloMapSnapshotPacket.ZoneData polygon = HoloMapSnapshotPacket.ZoneData.from(new HoloMapZoneData(
                id("zone/polish_polygon"), HoloMapIds.HAZARDS, HoloMapIds.HAZARD_SOURCE,
                HoloMapZoneShape.POLYGON, HoloMapZonePattern.HAZARD_STRIPES,
                IMapMarker.MarkerState.DISCOVERED, "Polish Polygon", "",
                Level.OVERWORLD, 0.0D, 64.0D, 0.0D, 8.0F, 32.0F, 32.0F,
                0x335CDAFF, 0xAA5CDAFF, HoloMapPrecision.ESTIMATED, 60,
                List.of(new HoloMapZonePoint(Level.OVERWORLD, -24.0D, 64.0D, -16.0D, 0),
                        new HoloMapZonePoint(Level.OVERWORLD, 24.0D, 64.0D, -16.0D, 1),
                        new HoloMapZonePoint(Level.OVERWORLD, 0.0D, 64.0D, 24.0D, 2))));
        helper.assertTrue(HoloMapRenderer.zoneHitForTests(state, polygon,
                        state.worldToScreenX(0.0D), state.worldToScreenZ(0.0D)),
                "Polygon zone hit testing should use polygon containment");
        helper.assertFalse(HoloMapRenderer.zoneHitForTests(state, polygon,
                        state.worldToScreenX(40.0D), state.worldToScreenZ(40.0D)),
                "Polygon zone hit testing should reject outside points inside only the loose bounds");

        HoloMapSnapshotPacket.ZoneData corridor = HoloMapSnapshotPacket.ZoneData.from(new HoloMapZoneData(
                id("zone/polish_corridor"), HoloMapIds.ROUTES, HoloMapIds.ROUTE_SOURCE,
                HoloMapZoneShape.CORRIDOR, HoloMapZonePattern.ROUTE_BANDS,
                IMapMarker.MarkerState.DISCOVERED, "Polish Corridor", "",
                Level.OVERWORLD, 0.0D, 64.0D, 0.0D, 6.0F, 12.0F, 12.0F,
                0x3392F7A6, 0xAA92F7A6, HoloMapPrecision.PRECISE, 55,
                List.of(new HoloMapZonePoint(Level.OVERWORLD, -40.0D, 64.0D, 0.0D, 0),
                        new HoloMapZonePoint(Level.OVERWORLD, 40.0D, 64.0D, 0.0D, 1))));
        helper.assertTrue(HoloMapRenderer.zoneHitForTests(state, corridor,
                        state.worldToScreenX(0.0D), state.worldToScreenZ(4.0D)),
                "Corridor zone hit testing should allow points near the route band");
        helper.assertFalse(HoloMapRenderer.zoneHitForTests(state, corridor,
                        state.worldToScreenX(0.0D), state.worldToScreenZ(28.0D)),
                "Corridor zone hit testing should reject points away from the route band");

        int smallAlpha = HoloMapRenderer.tacticalZoneFillAlphaForTests(0x775CDAFF, 90, false);
        int largeAlpha = HoloMapRenderer.tacticalZoneFillAlphaForTests(0x775CDAFF, 760, false);
        int hoveredLargeAlpha = HoloMapRenderer.tacticalZoneFillAlphaForTests(0x775CDAFF, 760, true);
        helper.assertTrue(largeAlpha < smallAlpha && hoveredLargeAlpha > largeAlpha,
                "Tactical zone styling should dim large zones and brighten hovered zones");
        helper.assertTrue(HoloMapRenderer.zoneFillStrideForTests(760) > HoloMapRenderer.zoneFillStrideForTests(60),
                "Large zones should use coarser fill stride for bounded LOD");
        HoloMapSnapshotPacket.ZoneData largeZone = HoloMapSnapshotPacket.ZoneData.from(HoloMapZoneData.circle(
                id("zone/polish_large_draw_plan"), HoloMapIds.HAZARDS, HoloMapIds.HAZARD_SOURCE,
                HoloMapZonePattern.HAZARD_STRIPES, IMapMarker.MarkerState.DISCOVERED,
                "Large Draw Plan", "", Level.OVERWORLD, 0.0D, 64.0D, 0.0D,
                760.0F, 0x335CDAFF, 0xAA5CDAFF, HoloMapPrecision.ESTIMATED, 40));
        int largeDrawSpans = HoloMapRenderer.zoneDrawSpanCountForTests(state, largeZone);
        helper.assertTrue(largeDrawSpans > 0 && largeDrawSpans <= 96,
                "Large zone draw plans should precompute bounded fill spans");
        helper.assertTrue(HoloMapRenderer.zoneDrawPlanClippedToViewportForTests(state, largeZone),
                "Large zone draw plans should clip fill, pattern, contour, and outline geometry to the viewport");
        helper.assertTrue(HoloMapRenderer.zonePatternPrimitiveCountForTests(state, largeZone) > 0,
                "Hazard stripe zones should retain visible pattern primitives after clipping");
        HoloMapSnapshotPacket.ZoneData scanGrid = HoloMapSnapshotPacket.ZoneData.from(HoloMapZoneData.circle(
                id("zone/polish_scan_grid"), HoloMapIds.ORBITAL_SCANS, HoloMapIds.CORE_SOURCE,
                HoloMapZonePattern.SCAN_GRID, IMapMarker.MarkerState.DISCOVERED,
                "Scan Grid", "", Level.OVERWORLD, 0.0D, 64.0D, 0.0D,
                140.0F, 0x3326DFF4, 0xAA26DFF4, HoloMapPrecision.PRECISE, 35));
        helper.assertTrue(HoloMapRenderer.zonePatternPrimitiveCountForTests(state, scanGrid) > 0,
                "Scan grid zones should retain visible grid primitives after clipping");
        HoloMapRenderer.clearZoneDrawPlanCacheForTests();
        int clippedPrimitiveCount = HoloMapRenderer.zoneDrawPrimitiveCountForTests(state, largeZone);
        long zoneBuilds = HoloMapRenderer.zoneDrawPlanBuildsForTests();
        helper.assertTrue(clippedPrimitiveCount > 0 && zoneBuilds == 1L
                        && HoloMapRenderer.zoneDrawPlanCacheEntriesForTests() == 1,
                "Zone draw planning should cache clipped geometry after the first build");
        HoloMapRenderer.zoneDrawPrimitiveCountForTests(state, largeZone);
        helper.assertTrue(HoloMapRenderer.zoneDrawPlanBuildsForTests() == zoneBuilds,
                "Zone draw planning should reuse cached geometry for unchanged screen-space fields");
        HoloMapSnapshotPacket.ZoneData selectedFar = zone(id("zone/polish_selected_far"), 420.0D, 0.0D, 80);
        HoloMapViewState selectedFarState = new HoloMapViewState("minecraft:overworld", 0, 0, 240, 180,
                420.0D, 0.0D, 1.0D, true, true, selectedFar.id().toString(), "",
                0, 0, 0.0D, 0.0D, 0.0F);
        helper.assertTrue(HoloMapRenderer.retainedZoneFillSpanCountForTests(selectedFarState, selectedFar, 4) > 0,
                "Selected far fields should keep full fill treatment in auto field mode");
        HoloMapRenderer.RenderBudget idleBudget = HoloMapUiController.fullscreenBudgetForTests(false);
        HoloMapRenderer.RenderBudget movingBudget = HoloMapUiController.fullscreenBudgetForTests(true);
        helper.assertTrue(idleBudget.equals(HoloMapRenderer.FULLSCREEN_BUDGET)
                        && movingBudget.equals(HoloMapRenderer.FULLSCREEN_INTERACTIVE_BUDGET),
                "Fullscreen controller should switch between idle and interactive render budgets");
        helper.assertTrue(movingBudget.maxTerrainTiles() == idleBudget.maxTerrainTiles()
                        && movingBudget.maxRouteSegments() < idleBudget.maxRouteSegments()
                        && movingBudget.maxOverlays() < idleBudget.maxOverlays()
                        && movingBudget.maxZones() < idleBudget.maxZones()
                        && movingBudget.labelLimit() < idleBudget.labelLimit()
                        && movingBudget.drawPlayer()
                        && movingBudget.edgeIndicators(),
                "Interactive fullscreen budget should keep terrain stable while lowering expensive overlay layers");
        List<HoloMapSnapshotPacket.ZoneData> largeAutoFields = List.of(
                HoloMapSnapshotPacket.ZoneData.from(HoloMapZoneData.circle(
                        id("zone/polish_idle_field_a"), HoloMapIds.HAZARDS, HoloMapIds.HAZARD_SOURCE,
                        HoloMapZonePattern.HAZARD_STRIPES, IMapMarker.MarkerState.DISCOVERED,
                        "Idle Field A", "", Level.OVERWORLD, -72.0D, 64.0D, -20.0D,
                        150.0F, 0x335CDAFF, 0xAA5CDAFF, HoloMapPrecision.ESTIMATED, 80)),
                HoloMapSnapshotPacket.ZoneData.from(HoloMapZoneData.circle(
                        id("zone/polish_idle_field_b"), HoloMapIds.HAZARDS, HoloMapIds.HAZARD_SOURCE,
                        HoloMapZonePattern.SCAN_GRID, IMapMarker.MarkerState.DISCOVERED,
                        "Idle Field B", "", Level.OVERWORLD, -36.0D, 64.0D, 12.0D,
                        150.0F, 0x335CDAFF, 0xAA5CDAFF, HoloMapPrecision.ESTIMATED, 70)),
                HoloMapSnapshotPacket.ZoneData.from(HoloMapZoneData.circle(
                        id("zone/polish_idle_field_c"), HoloMapIds.HAZARDS, HoloMapIds.HAZARD_SOURCE,
                        HoloMapZonePattern.ROUTE_BANDS, IMapMarker.MarkerState.DISCOVERED,
                        "Idle Field C", "", Level.OVERWORLD, 0.0D, 64.0D, -12.0D,
                        150.0F, 0x335CDAFF, 0xAA5CDAFF, HoloMapPrecision.ESTIMATED, 60)),
                HoloMapSnapshotPacket.ZoneData.from(HoloMapZoneData.circle(
                        id("zone/polish_idle_field_d"), HoloMapIds.HAZARDS, HoloMapIds.HAZARD_SOURCE,
                        HoloMapZonePattern.HAZARD_STRIPES, IMapMarker.MarkerState.DISCOVERED,
                        "Idle Field D", "", Level.OVERWORLD, 36.0D, 64.0D, 20.0D,
                        150.0F, 0x335CDAFF, 0xAA5CDAFF, HoloMapPrecision.ESTIMATED, 50)),
                HoloMapSnapshotPacket.ZoneData.from(HoloMapZoneData.circle(
                        id("zone/polish_idle_field_e"), HoloMapIds.HAZARDS, HoloMapIds.HAZARD_SOURCE,
                        HoloMapZonePattern.SCAN_GRID, IMapMarker.MarkerState.DISCOVERED,
                        "Idle Field E", "", Level.OVERWORLD, 72.0D, 64.0D, -18.0D,
                        150.0F, 0x335CDAFF, 0xAA5CDAFF, HoloMapPrecision.ESTIMATED, 40)));
        helper.assertTrue(HoloMapRenderer.retainedZoneFillSpanCountForTests(state, largeAutoFields, idleBudget)
                        > HoloMapRenderer.retainedZoneFillSpanCountForTests(state, largeAutoFields, movingBudget),
                "Idle fullscreen rendering should restore denser field fill treatment after movement settles");
        helper.assertTrue(HoloMapRenderer.retainedZoneFillSpanCountForTests(
                        selectedFarState, List.of(selectedFar), movingBudget) > 0,
                "Selected fields should keep full visual treatment even under the interactive budget");
        HoloMapSnapshotPacket.ZoneData offscreenZone = HoloMapSnapshotPacket.ZoneData.from(HoloMapZoneData.circle(
                id("zone/polish_offscreen_prefilter"), HoloMapIds.HAZARDS, HoloMapIds.HAZARD_SOURCE,
                HoloMapZonePattern.SCAN_GRID, IMapMarker.MarkerState.DISCOVERED,
                "Offscreen Prefilter", "", Level.OVERWORLD, 5000.0D, 64.0D, 0.0D,
                48.0F, 0x335CDAFF, 0xAA5CDAFF, HoloMapPrecision.ESTIMATED, 30));
        HoloMapViewState selectedOffscreenState = new HoloMapViewState("minecraft:overworld", 0, 0, 240, 180,
                0.0D, 0.0D, 1.0D, true, true, offscreenZone.id().toString(), "",
                0, 0, 0.0D, 0.0D, 0.0F);
        helper.assertFalse(HoloMapRenderer.zoneWorldPrefilterForTests(state, offscreenZone),
                "World-space zone prefilter should reject distant unselected fields before screen planning");
        helper.assertTrue(HoloMapRenderer.zoneWorldPrefilterForTests(selectedOffscreenState, offscreenZone),
                "World-space zone prefilter should retain selected fields for later screen-space checks");
        helper.assertTrue(HoloMapRenderer.acceptedLabelCountForTests(new int[][] {
                        {0, 0, 40, 10},
                        {10, 0, 40, 10},
                        {60, 0, 20, 10}
                }) == 2,
                "Label collision placement should reject overlapping non-forced labels");

        HoloMapSnapshotPacket.MarkerData duplicateA = markerData(id("marker/polish_dup_a"),
                IMapMarker.MarkerKind.HAZARD, "Buried Guardian Signature", 8.0D);
        HoloMapSnapshotPacket.MarkerData duplicateB = markerData(id("marker/polish_dup_b"),
                IMapMarker.MarkerKind.HAZARD, "Buried Guardian Signature", 12.0D);
        HoloMapSnapshotPacket.MarkerData mission = markerData(id("marker/polish_mission"),
                IMapMarker.MarkerKind.MISSION, "Escort a Salvager Convoy", 20.0D);
        HoloMapSnapshotPacket.MarkerData fieldMarker = new HoloMapSnapshotPacket.MarkerData(
                id("marker/polish_field"), HoloMapIds.HAZARDS, HoloMapIds.CORE_SOURCE,
                IMapMarker.MarkerKind.HAZARD, IMapMarker.MarkerState.DISCOVERED,
                "Local Veil Field", "GameTest field marker.",
                Level.OVERWORLD.identifier().toString(), 36.0D, 64.0D, 36.0D,
                48.0F, null, "", -1, HoloMapPrecision.ESTIMATED, 45);
        HoloMapWaypoint waypoint = HoloMapWaypoint.create(Scope.LOCAL, UUID.randomUUID(),
                Level.OVERWORLD.identifier().toString(), 4.0D, 64.0D, 4.0D,
                "Local Cache", 0xFF92F7A6, 1L);
        List<HoloMapUiController.Entry> entries = HoloMapUiController.groupedEntriesForTests(
                List.of(duplicateA, duplicateB, mission, fieldMarker), List.of(waypoint), "", "");
        helper.assertTrue(entries.stream().anyMatch(entry -> entry.header() && entry.title().equals("Waypoints"))
                        && entries.stream().anyMatch(entry -> entry.header() && entry.title().equals("Missions"))
                        && entries.stream().anyMatch(entry -> entry.header() && entry.title().equals("Hazards/Regions")),
                "Fullscreen ScreenCore list should expose grouped section headers");
        HoloMapUiController.Entry duplicateEntry = entries.stream()
                .filter(entry -> !entry.header() && entry.title().equals("Buried Guardian Signature"))
                .findFirst()
                .orElseThrow();
        helper.assertTrue(duplicateEntry.count() == 2 && duplicateEntry.id().equals(duplicateA.id())
                        && duplicateEntry.dimension().equals(Level.OVERWORLD.identifier().toString()),
                "Fullscreen ScreenCore list should collapse duplicate marker titles with counts");
        helper.assertTrue(duplicateEntry.distanceLabel().equals("11m")
                        && duplicateEntry.coordinateLabel().equals("8, 8")
                        && duplicateEntry.kindLabel().equals("Hazard"),
                "Fullscreen ScreenCore list entries should include distance, coordinates, and type metadata");
        HoloMapUiController.Entry fieldEntry = entries.stream()
                .filter(entry -> !entry.header() && entry.title().equals("Local Veil Field"))
                .findFirst()
                .orElseThrow();
        helper.assertTrue(fieldEntry.field() && fieldEntry.kindLabel().equals("Field")
                        && fieldEntry.prefix().equals("F"),
                "Field-backed marker rows should expose field metadata and glyphs");
        List<HoloMapUiController.Entry> selectedEntries = HoloMapUiController.groupedEntriesForTests(
                List.of(duplicateA, duplicateB, mission, fieldMarker), List.of(waypoint),
                duplicateB.id().toString(), "", 0.0D, 0.0D);
        int hazardsHeader = -1;
        for (int i = 0; i < selectedEntries.size(); i++) {
            if (selectedEntries.get(i).header() && selectedEntries.get(i).title().equals("Hazards/Regions")) {
                hazardsHeader = i;
                break;
            }
        }
        HoloMapUiController.Entry selectedHazard = hazardsHeader >= 0 && hazardsHeader + 1 < selectedEntries.size()
                ? selectedEntries.get(hazardsHeader + 1)
                : null;
        helper.assertTrue(selectedHazard != null && selectedHazard.selected()
                        && selectedHazard.id().equals(duplicateB.id()),
                "Selected marker rows should sort to the top of their section and preserve the selected id");
        helper.assertTrue(HoloMapUiController.selectedEntryForTests(selectedEntries) != null
                        && HoloMapUiController.selectedEntryForTests(selectedEntries).id().equals(duplicateB.id()),
                "Selected detail overlay should be able to resolve the active row");
        HoloMapUiController.EntryFocusTarget waypointTarget =
                HoloMapUiController.focusTargetForTests(entries, waypoint.id());
        HoloMapUiController.EntryFocusTarget markerTarget =
                HoloMapUiController.focusTargetForTests(entries, duplicateEntry.id());
        helper.assertTrue(waypointTarget != null && waypointTarget.waypoint()
                        && waypointTarget.x() == 4.0D && waypointTarget.z() == 4.0D
                        && markerTarget != null && !markerTarget.waypoint()
                        && markerTarget.x() == 8.0D && markerTarget.z() == 8.0D,
                "Index drawer row selection should expose the expected waypoint and marker center targets");
        helper.assertTrue("CLOSE".equals(HoloMapUiController.overlayActionForTests(0, 0, 640, 360, 620, 18))
                        && "SYNC".equals(HoloMapUiController.overlayActionForTests(0, 0, 640, 360, 576, 18))
                        && "TOGGLE_INDEX".equals(HoloMapUiController.overlayActionForTests(0, 0, 640, 360, 318, 343)),
                "Fullscreen overlay hitboxes should route controls before the map drag surface");
        helper.assertTrue(HoloMapUiController.controlLabelForTests("markers", true,
                        HoloMapVisibility.FieldMode.AUTO_NEAR, true).equals("MARKERS ON")
                        && HoloMapUiController.controlLabelForTests("markers", false,
                                HoloMapVisibility.FieldMode.AUTO_NEAR, true).equals("MARKERS OFF")
                        && HoloMapUiController.controlLabelForTests("fields", true,
                                HoloMapVisibility.FieldMode.AUTO_NEAR, true).equals("FIELDS AUTO")
                        && HoloMapUiController.controlLabelForTests("fields", true,
                                HoloMapVisibility.FieldMode.ALL, true).equals("FIELDS ALL")
                        && HoloMapUiController.controlLabelForTests("fields", true,
                                HoloMapVisibility.FieldMode.OFF, true).equals("FIELDS OFF")
                        && HoloMapUiController.controlLabelForTests("waypoints", true,
                                HoloMapVisibility.FieldMode.AUTO_NEAR, false).equals("WAYPOINTS OFF"),
                "Fullscreen ScreenCore mode buttons should expose dynamic ON/OFF and AUTO/ALL/OFF labels");
        int entryCacheBase = HoloMapUiController.entryCacheFingerprintForTests("minecraft:overworld", 10L, 2L,
                "", "", true, true);
        helper.assertTrue(entryCacheBase == HoloMapUiController.entryCacheFingerprintForTests(
                        "minecraft:overworld", 10L, 2L, "", "", true, true),
                "ScreenCore grouped list cache key should be stable for unchanged state");
        helper.assertTrue(entryCacheBase != HoloMapUiController.entryCacheFingerprintForTests(
                        "minecraft:overworld", 10L, 3L, "", "", true, true)
                        && entryCacheBase != HoloMapUiController.entryCacheFingerprintForTests(
                                "minecraft:overworld", 10L, 2L, mission.id().toString(), "", true, true),
                "ScreenCore grouped list cache key should invalidate on waypoint revision and selection");
        helper.succeed();
    }

    private static void serviceProviderIsolation(GameTestHelper helper) {
        HoloMapService.INSTANCE.clearForTests();
        Identifier layerId = id("layer/test_isolation");
        Identifier goodProvider = id("provider/good");
        Identifier failingProvider = id("provider/failing");
        HoloMapService.INSTANCE.registerProvider(new IMapDataProvider() {
            @Override
            public Identifier providerId() {
                return goodProvider;
            }

            @Override
            public List<IMapLayer> layers(Player player) {
                return List.of(new EchoMapLayer(layerId, "Isolation", 500, 0xFF66E8FF, true));
            }

            @Override
            public List<IMapMarker> markers(Player player) {
                return List.of(marker(id("marker/good"), layerId, goodProvider,
                        IMapMarker.MarkerKind.GENERIC, IMapMarker.MarkerState.DISCOVERED, 12.0D));
            }

            @Override
            public boolean refresh(ServerPlayer player, String reason) {
                return true;
            }
        });
        HoloMapService.INSTANCE.registerProvider(new IMapDataProvider() {
            @Override
            public Identifier providerId() {
                return failingProvider;
            }

            @Override
            public List<IMapLayer> layers(Player player) {
                throw new IllegalStateException("intentional layer failure");
            }

            @Override
            public List<IMapMarker> markers(Player player) {
                throw new IllegalStateException("intentional marker failure");
            }

            @Override
            public boolean refresh(ServerPlayer player, String reason) {
                throw new IllegalStateException("intentional refresh failure");
            }
        });
        helper.assertTrue(HoloMapService.INSTANCE.providerCount() == 2,
                "HoloMap should retain healthy and failing providers for isolation");
        helper.assertTrue(HoloMapService.INSTANCE.layers(null).stream().anyMatch(layer -> layer.id().equals(layerId)),
                "Healthy provider layer should survive a failing provider");
        helper.assertTrue(HoloMapService.INSTANCE.markers(null).size() == 1,
                "Healthy provider marker should survive a failing provider");
        helper.assertTrue(HoloMapService.INSTANCE.refresh(helper.makeMockServerPlayerInLevel(), "gametest"),
                "Healthy provider refresh should still report success");
        resetHoloMapService();
        helper.succeed();
    }

    private static void snapshotFilteringAndCap(GameTestHelper helper) {
        HoloMapService.INSTANCE.clearForTests();
        EchoCoreServices.registerMapMarkerService(HoloMapService.INSTANCE);
        Identifier layerId = id("layer/snapshot_cap");
        Identifier providerId = id("provider/snapshot_cap");
        int cap = configuredMarkerCap();
        int visibleMarkerCount = cap + 12;
        HoloMapService.INSTANCE.registerProvider(new IMapDataProvider() {
            @Override
            public Identifier providerId() {
                return providerId;
            }

            @Override
            public List<IMapLayer> layers(Player player) {
                return List.of(new EchoMapLayer(layerId, "Snapshot Cap", 501, 0xFF92F7A6, true));
            }

            @Override
            public List<IMapMarker> markers(Player player) {
                List<IMapMarker> markers = new ArrayList<>();
                markers.add(marker(id("marker/hidden"), layerId, providerId,
                        IMapMarker.MarkerKind.GENERIC, IMapMarker.MarkerState.HIDDEN, -999.0D));
                for (int i = 0; i < visibleMarkerCount; i++) {
                    IMapMarker.MarkerState state = i == 0
                            ? IMapMarker.MarkerState.LOCKED
                            : IMapMarker.MarkerState.DISCOVERED;
                    markers.add(marker(id("marker/visible_" + i), layerId, providerId,
                            IMapMarker.MarkerKind.GENERIC, state, i % 64));
                }
                return markers;
            }
        });

        HoloMapSnapshotPacket snapshot = HoloMapSnapshotPacket.from(helper.makeMockServerPlayerInLevel());
        helper.assertTrue(snapshot.markers().size() == cap,
                "Snapshot should cap visible markers at configured max");
        helper.assertTrue(snapshot.markers().stream().noneMatch(marker -> marker.state() == IMapMarker.MarkerState.HIDDEN),
                "Hidden markers should be omitted from normal snapshots");
        helper.assertTrue(snapshot.markers().stream().noneMatch(marker -> marker.state() == IMapMarker.MarkerState.LOCKED),
                "Locked marker state should be omitted from normal discovery-gated snapshots");
        helper.assertTrue(snapshot.overlays().stream().noneMatch(overlay -> overlay.sourceId().equals(providerId))
                        && snapshot.zones().stream().noneMatch(zone -> zone.sourceId().equals(providerId)),
                "Non-field marker kinds should not auto-promote radius data into overlay or HD zone draw payloads");
        resetHoloMapService();
        helper.succeed();
    }

    private static void discoveryGatedSnapshot(GameTestHelper helper) {
        HoloMapService.INSTANCE.clearForTests();
        EchoCoreServices.registerMapMarkerService(HoloMapService.INSTANCE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Identifier providerId = id("provider/discovery_gate");
        Identifier nearLayer = id("layer/discovery_near");
        Identifier farLayer = id("layer/discovery_far");
        Identifier lockedLayer = id("layer/discovery_locked");
        int radius = Config.mapInterestRadiusBlocks();
        HoloMapService.INSTANCE.registerProvider(new IMapDataProvider() {
            @Override
            public Identifier providerId() {
                return providerId;
            }

            @Override
            public List<IMapLayer> layers(Player player) {
                return List.of(
                        new EchoMapLayer(nearLayer, "Discovery Near", 501, 0xFF92F7A6, true),
                        new EchoMapLayer(farLayer, "Discovery Far", 502, 0xFFFF5C7A, true),
                        new EchoMapLayer(lockedLayer, "Discovery Locked", 503, 0xFFFFD166, true));
            }

            @Override
            public List<IMapMarker> markers(Player player) {
                double x = player.getX();
                double z = player.getZ();
                return List.of(
                        mapMarker(id("marker/discovered_near"), nearLayer, providerId,
                                IMapMarker.MarkerState.DISCOVERED, x + 24.0D, z),
                        mapMarker(id("marker/checked_near"), nearLayer, providerId,
                                IMapMarker.MarkerState.CHECKED, x + 40.0D, z),
                        mapMarker(id("marker/locked_near"), lockedLayer, providerId,
                                IMapMarker.MarkerState.LOCKED, x + 56.0D, z),
                        mapMarker(id("marker/hidden_near"), lockedLayer, providerId,
                                IMapMarker.MarkerState.HIDDEN, x + 72.0D, z),
                        mapMarker(id("marker/discovered_far"), farLayer, providerId,
                                IMapMarker.MarkerState.DISCOVERED, x + radius + 900.0D, z));
            }
        });
        HoloMapService.INSTANCE.registerHoloProvider(new IHoloMapDataProvider() {
            @Override
            public Identifier providerId() {
                return id("provider/discovery_gate_rich");
            }

            @Override
            public List<HoloMapRouteData> routes(Player player) {
                double x = player.getX();
                double z = player.getZ();
                return List.of(
                        new HoloMapRouteData(id("route/window_crossing"), HoloMapIds.ROUTES, providerId,
                                "Window Crossing", "", player.level().dimension(), 0xFF92F7A6,
                                IMapMarker.MarkerState.DISCOVERED,
                                routePoints(player.level().dimension(), x, z,
                                        -radius * 2.0D, -radius - 100.0D, -64.0D, 0.0D,
                                        64.0D, radius + 100.0D, radius * 2.0D)),
                        new HoloMapRouteData(id("route/far_only"), HoloMapIds.ROUTES, providerId,
                                "Far Only", "", player.level().dimension(), 0xFF92F7A6,
                                IMapMarker.MarkerState.DISCOVERED,
                                routePoints(player.level().dimension(), x, z,
                                        radius + 700.0D, radius + 900.0D)));
            }

            @Override
            public List<HoloMapZoneData> zones(Player player) {
                double x = player.getX();
                double z = player.getZ();
                return List.of(
                        HoloMapZoneData.circle(id("zone/discovered_near"), HoloMapIds.HAZARDS, providerId,
                                HoloMapZonePattern.SOLID, IMapMarker.MarkerState.DISCOVERED,
                                "Discovered Near", "", player.level().dimension(),
                                x + 48.0D, player.getY(), z, 32.0F, 0x335CDAFF, 0xAA5CDAFF,
                                HoloMapPrecision.PRECISE, 80),
                        HoloMapZoneData.circle(id("zone/discovered_far"), HoloMapIds.HAZARDS, providerId,
                                HoloMapZonePattern.SOLID, IMapMarker.MarkerState.DISCOVERED,
                                "Discovered Far", "", player.level().dimension(),
                                x + radius + 900.0D, player.getY(), z, 32.0F, 0x335CDAFF, 0xAA5CDAFF,
                                HoloMapPrecision.PRECISE, 80),
                        HoloMapZoneData.circle(id("zone/locked_near"), HoloMapIds.HAZARDS, providerId,
                                HoloMapZonePattern.SOLID, IMapMarker.MarkerState.LOCKED,
                                "Locked Near", "", player.level().dimension(),
                                x + 48.0D, player.getY(), z + 48.0D, 32.0F, 0x335CDAFF, 0xAA5CDAFF,
                                HoloMapPrecision.PRECISE, 80));
            }
        });

        HoloMapSnapshotPacket snapshot = HoloMapSnapshotPacket.from(player);
        List<Identifier> markerIds = snapshot.markers().stream().map(HoloMapSnapshotPacket.MarkerData::id).toList();
        helper.assertTrue(markerIds.contains(id("marker/discovered_near")),
                "Discovery gate should include discovered nearby markers");
        helper.assertTrue(markerIds.contains(id("marker/checked_near")),
                "Discovery gate should include checked nearby markers");
        helper.assertFalse(markerIds.contains(id("marker/locked_near"))
                        || markerIds.contains(id("marker/hidden_near"))
                        || markerIds.contains(id("marker/discovered_far")),
                "Discovery gate should omit locked, hidden, and far markers");
        List<Identifier> zoneIds = snapshot.zones().stream().map(HoloMapSnapshotPacket.ZoneData::id).toList();
        helper.assertTrue(zoneIds.contains(id("zone/discovered_near")),
                "Discovery gate should include discovered nearby zones");
        helper.assertFalse(zoneIds.contains(id("zone/discovered_far")) || zoneIds.contains(id("zone/locked_near")),
                "Discovery gate should omit far or locked zones");
        HoloMapSnapshotPacket.RouteData route = snapshot.routes().stream()
                .filter(candidate -> candidate.id().equals(id("route/window_crossing")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected crossing route in discovery-gated snapshot"));
        helper.assertTrue(route.points().size() == 5
                        && route.points().getFirst().order() == 1
                        && route.points().getLast().order() == 5,
                "Discovery gate should trim long routes to in-window points plus one adjacent point per side");
        helper.assertTrue(snapshot.routes().stream().noneMatch(candidate -> candidate.id().equals(id("route/far_only"))),
                "Discovery gate should omit far-only routes");
        List<Identifier> layerIds = snapshot.layers().stream().map(HoloMapSnapshotPacket.LayerData::id).toList();
        helper.assertTrue(layerIds.contains(nearLayer)
                        && layerIds.contains(HoloMapIds.HAZARDS)
                        && layerIds.contains(HoloMapIds.ROUTES),
                "Discovery gate should keep only layers with eligible content");
        helper.assertFalse(layerIds.contains(farLayer) || layerIds.contains(lockedLayer),
                "Discovery gate should hide far-only and locked-only layer metadata");
        resetHoloMapService();
        helper.succeed();
    }

    private static void debugMarkerSavedDataCodec(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HoloMapSavedData data = new HoloMapSavedData();
        EchoMapMarker marker = data.addDebugMarker(player, HoloMapIds.HAZARDS);
        helper.assertTrue(marker != null, "Debug marker should be created for server players");
        helper.assertTrue(data.debugMarkers(player.level()).size() == 1,
                "Debug marker should be queryable for the player dimension");

        JsonElement encoded = HoloMapSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();
        HoloMapSavedData decoded = HoloMapSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        List<EchoMapMarker> decodedMarkers = decoded.debugMarkers(player.level());
        helper.assertTrue(decodedMarkers.size() == 1, "Debug marker should survive codec save/load");
        EchoMapMarker decodedMarker = decodedMarkers.getFirst();
        helper.assertTrue(HoloMapIds.HAZARDS.equals(decodedMarker.layerId()),
                "Debug marker layer should survive codec save/load");
        helper.assertTrue(HoloMapIds.DEBUG_SOURCE.equals(decodedMarker.sourceId()),
                "Debug marker source should remain the debug source");
        helper.assertTrue(decodedMarker.precise(), "Debug marker should preserve precise coordinates");
        resetHoloMapService();
        helper.succeed();
    }

    private static void terrainSavedDataCodec(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HoloMapTerrainSavedData data = new HoloMapTerrainSavedData();
        int[] legacyPixels = filledPixels(0xFF356E4A);
        int[] surfacePixels = filledPixels(0xFF4A7962);
        data.putForTests(player.getUUID().toString(), Level.OVERWORLD.identifier().toString(),
                4, -7, 99L, legacyPixels);
        data.putForTests(player.getUUID().toString(), Level.OVERWORLD.identifier().toString(),
                5, -7, 100L, HoloMapTerrainTile.CURRENT_VERSION,
                HoloMapTerrainTile.DetailMode.SURFACE_SHADED, surfacePixels);
        JsonElement encoded = HoloMapTerrainSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();
        HoloMapTerrainSavedData decoded = HoloMapTerrainSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        HoloMapTerrainSavedData.TerrainStats stats = decoded.stats(player.getUUID(), Level.OVERWORLD);
        helper.assertTrue(stats.total() == 2 && stats.legacy() == 1 && stats.biomeFallback() == 1
                        && stats.surfaceShaded() == 1,
                "Terrain tile metadata should survive codec save/load");
        helper.assertTrue(decoded.tiles(player.getUUID(), Level.OVERWORLD, 4, -7, 0, 8).isEmpty(),
                "Legacy fallback terrain should remain stored but hidden from renderable tile queries");
        List<HoloMapTerrainTile> tiles = decoded.tiles(player.getUUID(), Level.OVERWORLD, 5, -7, 0, 8);
        helper.assertTrue(tiles.size() == 1, "Renderable terrain tile should survive codec save/load");
        HoloMapTerrainTile tile = tiles.getFirst();
        helper.assertTrue(tile.chunkX() == 5 && tile.chunkZ() == -7,
                "Terrain tile chunk coordinates should survive codec save/load");
        helper.assertTrue(tile.pixel(0, 0) == 0xFF4A7962,
                "Terrain tile pixels should survive codec save/load");
        helper.assertTrue(tile.version() == HoloMapTerrainTile.CURRENT_VERSION,
                "Current terrain tile version should survive codec save/load");
        helper.assertTrue(tile.detailMode() == HoloMapTerrainTile.DetailMode.SURFACE_SHADED,
                "Current terrain tile detail should survive codec save/load");
        helper.succeed();
    }

    private static void terrainV3TileMetadata(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HoloMapTerrainSavedData data = new HoloMapTerrainSavedData();
        int[] pixels = filledPixels(0xFF6D7C70);
        data.putForTests(player.getUUID().toString(), Level.OVERWORLD.identifier().toString(), 2, 3, 144L,
                HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.SURFACE_SHADED, pixels);

        JsonElement encoded = HoloMapTerrainSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();
        HoloMapTerrainSavedData decoded = HoloMapTerrainSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        List<HoloMapTerrainTile> tiles = decoded.tiles(player.getUUID(), Level.OVERWORLD, 2, 3, 1, 8);
        helper.assertTrue(tiles.size() == 1, "V3 terrain tile should survive codec save/load");
        HoloMapTerrainTile tile = tiles.getFirst();
        helper.assertTrue(decoded.hasRenderableTile(player.getUUID(), Level.OVERWORLD, 2, 3),
                "Server terrain helper should report current non-fallback tiles as renderable");
        helper.assertTrue(tile.version() == HoloMapTerrainTile.CURRENT_VERSION,
                "V3 terrain tile version should survive codec save/load");
        helper.assertTrue(tile.detailMode() == HoloMapTerrainTile.DetailMode.SURFACE_SHADED,
                "V3 terrain tile detail mode should survive codec save/load");
        helper.assertTrue(tile.renderableSurface(), "Surface-shaded V3 terrain should be renderable");
        HoloMapTerrainSavedData.TerrainStats stats = decoded.stats(player.getUUID(), Level.OVERWORLD);
        helper.assertTrue(stats.surfaceShaded() == 1 && stats.legacy() == 0,
                "V3 terrain stats should count shaded non-legacy tiles");
        helper.assertFalse(decoded.needsSample(player.getUUID(), Level.OVERWORLD, 2, 3, 145L, 2400L),
                "Fresh V3 terrain tiles should not need immediate resampling");
        helper.assertFalse(decoded.saveTile(player.getUUID(), Level.OVERWORLD, 4, 5, 146L,
                        HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.BIOME_FALLBACK, pixels),
                "All-fallback terrain tiles should not be saved as renderable map terrain");

        HoloMapTerrainSavedData legacy = new HoloMapTerrainSavedData();
        legacy.putForTests(player.getUUID().toString(), Level.OVERWORLD.identifier().toString(), 2, 3, 144L, pixels);
        helper.assertTrue(legacy.tiles(player.getUUID(), Level.OVERWORLD, 2, 3, 0, 8).isEmpty(),
                "Legacy biome fallback terrain should remain stored but hidden from renderable tile queries");
        helper.assertFalse(legacy.hasRenderableTile(player.getUUID(), Level.OVERWORLD, 2, 3),
                "Server terrain helper should reject legacy biome fallback tiles");
        helper.assertTrue(legacy.needsSample(player.getUUID(), Level.OVERWORLD, 2, 3, 145L, 2400L),
                "Legacy terrain tiles should be eligible for lazy V3 resampling");
        helper.succeed();
    }

    private static void terrainPaletteDeterminism(GameTestHelper helper) {
        int plainsA = HoloMapTerrainPalette.colorForBiome("minecraft:overworld", "plains", 64, false);
        int plainsB = HoloMapTerrainPalette.colorForBiome("minecraft:overworld", "plains", 64, false);
        int water = HoloMapTerrainPalette.colorForBiome("minecraft:overworld", "river", 64, true);
        int end = HoloMapTerrainPalette.colorForBiome("minecraft:the_end", "end_highlands", 64, false);
        int sand = HoloMapTerrainPalette.colorForDescriptor("minecraft:overworld", "desert", 64,
                "sand", false, false);
        int stone = HoloMapTerrainPalette.colorForDescriptor("minecraft:overworld", "stony_peaks", 90,
                "stone", false, false);
        int snow = HoloMapTerrainPalette.colorForDescriptor("minecraft:overworld", "snowy_plains", 74,
                "snow", false, false);
        int lava = HoloMapTerrainPalette.colorForDescriptor("minecraft:the_nether", "nether_wastes", 32,
                "lava", false, false);
        helper.assertTrue(plainsA == plainsB, "Terrain palette should be deterministic for identical input");
        helper.assertTrue(plainsA != water, "Terrain palette should distinguish land from water");
        helper.assertTrue(end != plainsA, "Terrain palette should distinguish End terrain from overworld plains");
        helper.assertTrue(sand != stone, "Surface palette should distinguish sand from stone");
        helper.assertTrue(snow != sand, "Surface palette should highlight snow separately from sand");
        helper.assertTrue(lava != stone, "Surface palette should highlight lava separately from stone");
        helper.succeed();
    }

    private static void terrainScannerAndRequestCaps(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ServerLevel level = (ServerLevel) player.level();
        HoloMapTerrainSavedData data = HoloMapTerrainSavedData.get(level);
        data.clear(player.getUUID());

        int sampled = HoloMapTerrainScanner.scanAround(player, 2, 1);
        helper.assertTrue(sampled <= 1, "Terrain scanner should respect the per-pass sample cap");
        helper.assertTrue(data.discoverableTileCount(player.getUUID(), level.dimension()) <= 1,
                "Scanner should not save more terrain tiles than the per-pass cap");

        int centerChunkX = Math.floorDiv(player.blockPosition().getX(), 16);
        int centerChunkZ = Math.floorDiv(player.blockPosition().getZ(), 16);
        HoloMapTerrainTile remoteSample = HoloMapTerrainScanner.sampleChunk(level,
                centerChunkX + 1000, centerChunkZ + 1000, level.getGameTime());
        helper.assertTrue(remoteSample == null, "Scanner should not sample or generate unloaded remote chunks");
        level.getChunk(centerChunkX, centerChunkZ);
        data.clear(player.getUUID());
        HoloMapTerrainScanner.clearForTests();
        int requestSampled = HoloMapTerrainScanner.scanRequestedViewport(player,
                level.dimension().identifier().toString(), centerChunkX, centerChunkZ, 0, 1);
        helper.assertTrue(requestSampled == 1
                        && data.discoverableTileCount(player.getUUID(), level.dimension()) == 1,
                "Terrain tile requests should opportunistically scan already-loaded viewport chunks");
        int wrongDimensionSampled = HoloMapTerrainScanner.scanRequestedViewport(player,
                "minecraft:the_nether", centerChunkX, centerChunkZ, 0, 1);
        int remoteRequestSampled = HoloMapTerrainScanner.scanRequestedViewport(player,
                level.dimension().identifier().toString(), centerChunkX + 1000, centerChunkZ + 1000, 4, 8);
        helper.assertTrue(wrongDimensionSampled == 0 && remoteRequestSampled == 0,
                "Request-priority terrain scans should stay dimension-scoped and skip unloaded chunks");
        data.clear(player.getUUID());
        data.putForTests(player.getUUID().toString(), level.dimension().identifier().toString(),
                centerChunkX, centerChunkZ, Math.max(0L, level.getGameTime() - 100L),
                HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.SURFACE_SHADED,
                filledPixels(0xFF51615F));
        HoloMapTerrainScanner.clearForTests();
        int skipped = HoloMapTerrainScanner.scanAround(player, 0, 1, false);
        helper.assertTrue(skipped == 0, "Scanner should skip fresh current-version terrain tiles");
        int forced = HoloMapTerrainScanner.scanAround(player, 0, 1, true);
        helper.assertTrue(forced == 1, "Forced terrain resample should refresh a loaded current tile");
        helper.assertTrue(data.tiles(player.getUUID(), level.dimension(), centerChunkX, centerChunkZ, 0, 8)
                        .stream().allMatch(HoloMapTerrainTile::renderableSurface),
                "Scanner should only expose renderable real surface terrain tiles");

        int beforeRemote = data.discoverableTileCount(player.getUUID(), level.dimension());
        HoloMapTileBatchPacket remote = HoloMapTileBatchPacket.from(player, new HoloMapTileRequestPacket(
                level.dimension().identifier().toString(), centerChunkX + 1000, centerChunkZ + 1000, 4));
        int afterRemote = data.discoverableTileCount(player.getUUID(), level.dimension());
        helper.assertTrue(beforeRemote == afterRemote,
                "Tile requests should not reveal or save undiscovered remote terrain");
        helper.assertTrue(remote.tiles().isEmpty(), "Remote undiscovered tile requests should return no tiles");

        data.clear(player.getUUID());
        data.putForTests(player.getUUID().toString(), level.dimension().identifier().toString(),
                centerChunkX, centerChunkZ, 1L, filledPixels(0xFF14262A));
        data.putForTests(player.getUUID().toString(), level.dimension().identifier().toString(),
                centerChunkX + 1, centerChunkZ, 2L,
                HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.BIOME_FALLBACK,
                filledPixels(0xFF14262A));
        data.putForTests(player.getUUID().toString(), level.dimension().identifier().toString(),
                centerChunkX + 2, centerChunkZ, 3L,
                HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.SURFACE_SHADED,
                filledPixels(0xFF245C66));
        HoloMapTileBatchPacket filteredBatch = HoloMapTileBatchPacket.from(player, new HoloMapTileRequestPacket(
                level.dimension().identifier().toString(), centerChunkX, centerChunkZ, 4));
        helper.assertTrue(filteredBatch.discoveredCount() == 1 && filteredBatch.tiles().size() == 1
                        && filteredBatch.tiles().getFirst().chunkX() == centerChunkX + 2,
                "Tile batches should send only remembered real loaded-chunk surface tiles");
        HoloMapTerrainClientState.clear();
        HoloMapTerrainClientState.apply(new HoloMapTileBatchPacket(level.dimension().identifier().toString(), 1,
                level.getGameTime(), List.of(
                        new HoloMapTerrainTile(level.dimension().identifier().toString(), centerChunkX, centerChunkZ,
                                1L, filledPixels(0xFF14262A)),
                        new HoloMapTerrainTile(level.dimension().identifier().toString(), centerChunkX + 2,
                                centerChunkZ, 3L, HoloMapTerrainTile.CURRENT_VERSION,
                                HoloMapTerrainTile.DetailMode.SURFACE_SHADED, filledPixels(0xFF245C66)))));
        helper.assertTrue(HoloMapTerrainClientState.tiles(level.dimension().identifier().toString(),
                        centerChunkX - 1, centerChunkX + 3, centerChunkZ - 1, centerChunkZ + 1).size() == 1
                        && HoloMapTerrainClientState.tileCount(level.dimension().identifier().toString()) == 1,
                "Client terrain cache should reject legacy and fallback placeholder tiles");
        HoloMapTerrainClientState.clear();
        data.clear(player.getUUID());
        int maxBatch = HoloMapTileBatchPacket.maxBatchSize();
        helper.assertTrue(maxBatch >= 128 && maxBatch <= 1024,
                "Configured terrain tile batch size should stay inside packet safety bounds");
        int safeRequestRadius = new HoloMapTileRequestPacket(level.dimension().identifier().toString(),
                centerChunkX, centerChunkZ, 64).safeRadius();
        helper.assertTrue(safeRequestRadius >= 1 && safeRequestRadius <= 64,
                "Configured terrain request radius should stay inside packet safety bounds");
        int requestDiameter = safeRequestRadius * 2 + 1;
        int requestArea = requestDiameter * requestDiameter;
        int targetTiles = Math.min(maxBatch + 24, requestArea);
        for (int i = 0; i < targetTiles; i++) {
            data.putForTests(player.getUUID().toString(), level.dimension().identifier().toString(),
                    centerChunkX + i % requestDiameter - safeRequestRadius,
                    centerChunkZ + i / requestDiameter - safeRequestRadius, i,
                    HoloMapTerrainTile.CURRENT_VERSION, HoloMapTerrainTile.DetailMode.SURFACE_SHADED,
                    filledPixels(0xFF245C66 + i));
        }
        HoloMapTileBatchPacket batch = HoloMapTileBatchPacket.from(player, new HoloMapTileRequestPacket(
                level.dimension().identifier().toString(), centerChunkX, centerChunkZ, safeRequestRadius));
        helper.assertTrue(batch.tiles().size() == Math.min(maxBatch, requestArea),
                "Tile batch response should respect configured packet cap");
        data.clear(player.getUUID());
        HoloMapTerrainScanner.clearForTests();
        helper.succeed();
    }

    private static void waypointSavedDataCodec(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HoloMapWaypointSavedData data = new HoloMapWaypointSavedData();
        HoloMapWaypoint waypoint = HoloMapWaypoint.create(Scope.PERSONAL, player.getUUID(),
                Level.OVERWORLD.identifier().toString(), 32.0D, 70.0D, -48.0D,
                "Codec Relay", 0xFF92F7A6, 12L);
        helper.assertTrue(data.upsert(player, waypoint, false), "Personal waypoint should be accepted");
        JsonElement encoded = HoloMapWaypointSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();
        HoloMapWaypointSavedData decoded = HoloMapWaypointSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        List<HoloMapWaypoint> waypoints = decoded.waypointsFor(player.getUUID(), 16);
        helper.assertTrue(waypoints.size() == 1, "Waypoint should survive codec save/load");
        HoloMapWaypoint decodedWaypoint = waypoints.getFirst();
        helper.assertTrue(decodedWaypoint.scope() == Scope.PERSONAL, "Waypoint scope should survive codec save/load");
        helper.assertTrue(decodedWaypoint.owner().equals(player.getUUID()), "Waypoint owner should survive codec save/load");
        helper.assertTrue(decodedWaypoint.x() == 32.0D && decodedWaypoint.z() == -48.0D,
                "Waypoint coordinates should survive codec save/load");
        helper.succeed();
    }

    private static void waypointMutationRules(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HoloMapWaypointSavedData data = new HoloMapWaypointSavedData();
        HoloMapWaypoint personal = HoloMapWaypoint.create(Scope.PERSONAL, player.getUUID(),
                Level.OVERWORLD.identifier().toString(), 8.0D, 64.0D, 8.0D,
                "Personal", 0xFF92F7A6, 1L);
        HoloMapWaypoint shared = HoloMapWaypoint.create(Scope.SHARED, player.getUUID(),
                Level.OVERWORLD.identifier().toString(), 16.0D, 64.0D, 16.0D,
                "Shared", 0xFFFFDA73, 1L);
        helper.assertTrue(data.upsert(player, personal, false), "Players should be able to upsert personal waypoints");
        helper.assertFalse(data.upsert(player, shared, false), "Shared waypoint upsert should require permission");
        helper.assertTrue(data.upsert(player, shared, true), "Shared waypoint upsert should work with permission");
        helper.assertFalse(data.delete(player, shared.id(), false), "Shared waypoint delete should require permission");
        helper.assertTrue(data.delete(player, shared.id(), true), "Shared waypoint delete should work with permission");
        helper.assertTrue(data.delete(player, personal.id(), false), "Personal waypoint delete should work for owner");
        helper.succeed();
    }

    private static void chunkActionCreatesSavedWaypoint(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HoloMapWaypointSavedData data = HoloMapWaypointSavedData.get(player.level().getServer());
        data.clearForTests();
        HoloMapChunkActions.clearForTests();
        HoloMapChunkActions.register(BuiltinHoloMapChunkActionProvider.INSTANCE);
        HoloMapChunkSelection selection = new HoloMapChunkSelection(Level.OVERWORLD, 2, -3);
        try {
            HoloMapChunkActionResult result = HoloMapChunkActions.handle(
                    player,
                    selection,
                    BuiltinHoloMapChunkActionProvider.PROVIDER_ID,
                    BuiltinHoloMapChunkActionProvider.CREATE_PERSONAL_WAYPOINT);
            helper.assertTrue(result.success(), "HoloMap chunk action should report a saved waypoint result.");
            List<HoloMapWaypoint> waypoints = data.waypointsFor(player, 16);
            helper.assertTrue(waypoints.size() == 1,
                    "HoloMap chunk action should create one persisted personal waypoint.");
            HoloMapWaypoint waypoint = waypoints.getFirst();
            helper.assertTrue(waypoint.scope() == Scope.PERSONAL && waypoint.owner().equals(player.getUUID()),
                    "HoloMap chunk action should save a personal waypoint owned by the acting player.");
            helper.assertTrue(waypoint.x() == selection.centerX()
                            && waypoint.z() == selection.centerZ()
                            && waypoint.dimension().equals(Level.OVERWORLD.identifier().toString()),
                    "HoloMap chunk action should save the selected chunk center and dimension.");
        } finally {
            HoloMapChunkActions.clearForTests();
            data.clearForTests();
        }
        helper.succeed();
    }

    private static void waypointClientMerge(GameTestHelper helper) {
        HoloMapWaypointClientState.clearForTests();
        HoloMapWaypoint local = HoloMapWaypoint.create(Scope.LOCAL, HoloMapWaypoint.NO_OWNER,
                Level.OVERWORLD.identifier().toString(), 1.0D, 64.0D, 1.0D,
                "Local", 0xFF38DFF4, 1L);
        HoloMapWaypoint personal = HoloMapWaypoint.create(Scope.PERSONAL, java.util.UUID.randomUUID(),
                Level.OVERWORLD.identifier().toString(), 2.0D, 64.0D, 2.0D,
                "Personal", 0xFF92F7A6, 2L);
        HoloMapWaypoint deathpoint = new HoloMapWaypoint(id("deathpoint/test/client"), UUID.randomUUID(),
                Scope.PERSONAL, Level.OVERWORLD.identifier().toString(), 3.0D, 64.0D, 3.0D,
                "Deathpoint", 0xFFFF6688, HoloMapWaypoint.DEATHPOINT_ICON, true, 3L, 3L);
        HoloMapWaypointClientState.setLocalWaypoints(List.of(local));
        HoloMapWaypointClientState.apply(new HoloMapWaypointSyncPacket(List.of(personal, deathpoint), 42L));
        List<HoloMapWaypoint> merged = HoloMapWaypointClientState.waypoints();
        helper.assertTrue(merged.size() == 3, "Client waypoint cache should merge local, server, and deathpoint waypoints");
        helper.assertTrue(merged.getFirst().scope() == Scope.LOCAL,
                "Local waypoints should sort before synced server waypoints");
        helper.assertTrue(merged.get(1).isDeathpoint(),
                "Deathpoints should sort ahead of normal personal synced waypoints");
        helper.assertTrue(merged.stream().anyMatch(HoloMapWaypoint::isDeathpoint),
                "Client waypoint cache should preserve synced deathpoints");
        helper.assertTrue(HoloMapWaypointClientState.lastSyncGameTime() == 42L,
                "Waypoint sync packet should update client sync time");
        HoloMapWaypointClientState.clearForTests();
        helper.succeed();
    }

    private static void deathpointPersonalVisibility(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HoloMapWaypointSavedData data = new HoloMapWaypointSavedData();
        HoloMapWaypoint deathpoint = data.recordDeathpoint(player, 10);
        helper.assertTrue(deathpoint != null && deathpoint.isDeathpoint(),
                "Deathpoint recording should create a deathpoint waypoint");
        helper.assertTrue(deathpoint.scope() == Scope.PERSONAL,
                "Deathpoints should be personal waypoints");
        helper.assertTrue(deathpoint.owner().equals(player.getUUID()),
                "Deathpoint owner should be the player that died");
        helper.assertTrue(data.waypointsFor(player.getUUID(), 16).stream()
                        .anyMatch(waypoint -> waypoint.id().equals(deathpoint.id())),
                "Deathpoint should be visible to its owner");
        helper.assertTrue(data.waypointsFor(UUID.randomUUID(), 16).stream()
                        .noneMatch(HoloMapWaypoint::isDeathpoint),
                "Deathpoint should not be visible to other players");
        helper.succeed();
    }

    private static void deathpointCodecAndRetention(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HoloMapWaypointSavedData data = new HoloMapWaypointSavedData();
        HoloMapWaypoint normal = HoloMapWaypoint.create(Scope.PERSONAL, player.getUUID(),
                Level.OVERWORLD.identifier().toString(), 8.0D, 64.0D, 8.0D,
                "Normal Personal", 0xFF92F7A6, 1L);
        data.putForTests(normal);
        for (int i = 0; i < 5; i++) {
            data.recordDeathpoint(player, 2);
        }
        List<HoloMapWaypoint> retained = data.waypointsFor(player.getUUID(), 16);
        helper.assertTrue(retained.stream().filter(HoloMapWaypoint::isDeathpoint).count() == 2L,
                "Deathpoint retention should keep only the newest configured deathpoints");
        helper.assertTrue(retained.stream().anyMatch(waypoint -> waypoint.id().equals(normal.id())),
                "Deathpoint retention should not evict normal personal waypoints");

        JsonElement encoded = HoloMapWaypointSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();
        HoloMapWaypointSavedData decoded = HoloMapWaypointSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        List<HoloMapWaypoint> decodedWaypoints = decoded.waypointsFor(player.getUUID(), 16);
        helper.assertTrue(decodedWaypoints.stream().filter(HoloMapWaypoint::isDeathpoint).count() == 2L,
                "Deathpoints should survive codec save/load");
        helper.assertTrue(decodedWaypoints.stream().anyMatch(waypoint -> waypoint.id().equals(normal.id())),
                "Normal personal waypoints should survive codec save/load with deathpoints");
        helper.succeed();
    }

    private static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String testName,
            Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                200,
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

    private static void resetHoloMapService() {
        HoloMapService.INSTANCE.clearForTests();
        HoloMapService.INSTANCE.registerBuiltins();
        EchoCoreServices.registerMapMarkerService(HoloMapService.INSTANCE);
    }

    private static void missionCoreContentRegistration(GameTestHelper helper) {
        InMemoryMissionRegistry registry = new InMemoryMissionRegistry();
        HoloMapMissionCoreIntegration.registerContent(registry);
        helper.assertTrue(registry.chapter(id("holomap")).isPresent(), "HoloMap MissionCore chapter should be owned by HoloMap.");
        assertMission(helper, registry, "discover_terrain", "terrain", MissionObjectiveType.ENTER_REGION);
        assertMission(helper, registry, "create_waypoint", "waypoint", MissionObjectiveType.CUSTOM);
        assertMission(helper, registry, "reveal_marker", "marker", MissionObjectiveType.DISCOVER_STRUCTURE);
        assertMission(helper, registry, "sync_route", "sync", MissionObjectiveType.ESTABLISH_ROUTE);
        helper.succeed();
    }

    private static void assertMission(
            GameTestHelper helper,
            InMemoryMissionRegistry registry,
            String missionPath,
            String objectiveKey,
            MissionObjectiveType type) {
        Identifier missionId = id(missionPath);
        MissionDefinition mission = registry.missionDefinition(missionId)
                .orElseThrow(() -> new AssertionError("Missing MissionCore mission: " + missionId));
        helper.assertTrue(mission.kind() == MissionKind.SIDE_OP, "HoloMap MissionCore missions should be side ops.");
        helper.assertTrue(!mission.rewards().isEmpty(), "HoloMap MissionCore mission should have a claimable reward: " + missionId);
        helper.assertTrue(mission.objectives().size() == 1, "HoloMap MissionCore mission should have one direct objective: " + missionId);
        helper.assertTrue(mission.objectives().getFirst().type() == type, "HoloMap objective type should stay stable: " + missionId);
        String target = mission.objectives().getFirst().criteria().get("target");
        helper.assertTrue(MissionHookTargets.objectiveTarget(EchoHoloMap.MODID, missionId, objectiveKey).toString().equals(target),
                "HoloMap MissionCore objective target should use MissionHookTargets: " + missionId);
    }

    private static List<Identifier> requiredLayerIds() {
        return HoloMapLayers.REQUIRED.stream().map(IMapLayer::id).toList();
    }

    private static int configuredMarkerCap() {
        try {
            return Math.max(32, Math.min(2048, Config.MAX_MARKERS.get()));
        } catch (RuntimeException exception) {
            return 384;
        }
    }

    private static EchoMapMarker marker(Identifier id, Identifier layerId, Identifier sourceId,
            IMapMarker.MarkerKind kind, IMapMarker.MarkerState state, double x) {
        return new EchoMapMarker(
                id,
                layerId,
                sourceId,
                kind,
                state,
                state == IMapMarker.MarkerState.HIDDEN ? "Hidden marker" : "Marker " + id.getPath(),
                "GameTest marker for HoloMap service validation.",
                Level.OVERWORLD,
                x,
                64.0D,
                x,
                16.0F,
                null,
                null,
                -1,
                true);
    }

    private static HoloMapSnapshotPacket.ZoneData zone(Identifier id, double x, double z, int priority) {
        return HoloMapSnapshotPacket.ZoneData.from(HoloMapZoneData.circle(id, HoloMapIds.HAZARDS,
                HoloMapIds.HAZARD_SOURCE, HoloMapZonePattern.SOLID, IMapMarker.MarkerState.DISCOVERED,
                "Zone " + id.getPath(), "GameTest zone.", Level.OVERWORLD, x, 64.0D, z,
                32.0F, 0x335CDAFF, 0xAA5CDAFF, HoloMapPrecision.ESTIMATED, priority));
    }

    private static HoloMapSnapshotPacket.MarkerData markerData(Identifier id, IMapMarker.MarkerKind kind,
            String title, double x) {
        Identifier layer = switch (kind) {
            case MISSION -> HoloMapIds.MISSIONS;
            case HAZARD -> HoloMapIds.HAZARDS;
            case ROUTE -> HoloMapIds.ROUTES;
            default -> HoloMapIds.ORBITAL_SCANS;
        };
        return new HoloMapSnapshotPacket.MarkerData(id, layer, HoloMapIds.CORE_SOURCE, kind,
                IMapMarker.MarkerState.DISCOVERED, title, "GameTest marker.",
                Level.OVERWORLD.identifier().toString(), x, 64.0D, x, 0.0F, null, "",
                -1, HoloMapPrecision.PRECISE, 50);
    }

    private static EchoMapMarker mapMarker(Identifier id, Identifier layerId, Identifier sourceId,
            IMapMarker.MarkerState state, double x, double z) {
        return new EchoMapMarker(
                id,
                layerId,
                sourceId,
                IMapMarker.MarkerKind.GENERIC,
                state,
                "Marker " + id.getPath(),
                "GameTest discovery-gated marker.",
                Level.OVERWORLD,
                x,
                64.0D,
                z,
                16.0F,
                null,
                null,
                -1,
                true);
    }

    private static List<HoloMapRoutePoint> routePoints(ResourceKey<Level> dimension,
            double centerX, double centerZ, double... offsets) {
        List<HoloMapRoutePoint> points = new ArrayList<>();
        for (int i = 0; i < offsets.length; i++) {
            points.add(new HoloMapRoutePoint(dimension, centerX + offsets[i], 64.0D, centerZ, i,
                    "P" + i, HoloMapPrecision.PRECISE));
        }
        return points;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, path);
    }

    private static Path workspaceRoot() {
        Path current = Path.of("").toAbsolutePath();
        Path cursor = current;
        while (cursor != null) {
            if (Files.exists(cursor.resolve("settings.gradle")) || Files.exists(cursor.resolve("settings.gradle.kts"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return current;
    }

    private static int[] filledPixels(int color) {
        int[] pixels = new int[HoloMapTerrainTile.PIXELS];
        java.util.Arrays.fill(pixels, color);
        return pixels;
    }
}
