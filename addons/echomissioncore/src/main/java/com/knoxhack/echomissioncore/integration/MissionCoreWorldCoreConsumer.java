package com.knoxhack.echomissioncore.integration;

import com.knoxhack.echocore.api.EchoWorldRuntimeBus;
import com.knoxhack.echocore.api.WorldHazardSnapshot;
import com.knoxhack.echocore.api.WorldMarker;
import com.knoxhack.echocore.api.WorldRegionInstance;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echomissioncore.service.MissionCoreService;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Bridges WorldCore runtime events into MissionCore objective progress without
 * making MissionCore depend on the WorldCore implementation module.
 */
public final class MissionCoreWorldCoreConsumer {
    private static final Identifier REGION_DISCOVERED =
            Identifier.fromNamespaceAndPath("echoworldcore", "region_discovered");
    private static final Identifier MARKER_REVEALED =
            Identifier.fromNamespaceAndPath("echoworldcore", "marker_revealed");
    private static final Identifier HAZARD_CHANGED =
            Identifier.fromNamespaceAndPath("echoworldcore", "hazard_changed");

    private static boolean registered;

    private MissionCoreWorldCoreConsumer() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        registerListeners();
    }

    public static synchronized void registerForTests() {
        registered = true;
        registerListeners();
    }

    private static void registerListeners() {
        EchoWorldRuntimeBus.onRegionEntered(MissionCoreWorldCoreConsumer::onRegionEntered);
        EchoWorldRuntimeBus.onRegionDiscovered(MissionCoreWorldCoreConsumer::onRegionDiscovered);
        EchoWorldRuntimeBus.onRegionScanned(MissionCoreWorldCoreConsumer::onRegionScanned);
        EchoWorldRuntimeBus.onMarkerRevealed(MissionCoreWorldCoreConsumer::onMarkerRevealed);
        EchoWorldRuntimeBus.onHazardChanged(MissionCoreWorldCoreConsumer::onHazardChanged);
    }

    private static void onRegionEntered(EchoWorldRuntimeBus.RegionEntered event) {
        ServerPlayer player = event.player();
        WorldRegionInstance region = event.region();
        if (player == null || region == null) {
            return;
        }
        MissionCoreService.INSTANCE.recordObjective(player, MissionObjectiveType.ENTER_REGION,
                region.definitionId(), 1, context("region_entered", region, null));
    }

    private static void onRegionDiscovered(EchoWorldRuntimeBus.RegionDiscovered event) {
        ServerPlayer player = event.player();
        WorldRegionInstance region = event.region();
        if (player == null || region == null || !event.firstDiscovery()) {
            return;
        }
        Map<String, String> context = context("region_discovered", region, null);
        context.put("source", event.source() == null ? "" : event.source().name());
        MissionCoreService.INSTANCE.recordObjective(player, MissionObjectiveType.CUSTOM,
                REGION_DISCOVERED, 1, context);
        MissionCoreService.INSTANCE.recordObjective(player, MissionObjectiveType.CUSTOM,
                region.definitionId(), 1, context);
    }

    private static void onRegionScanned(EchoWorldRuntimeBus.RegionScanned event) {
        ServerPlayer player = event.player();
        WorldRegionInstance region = event.region();
        WorldMarker marker = event.marker();
        if (player == null || region == null) {
            return;
        }
        Identifier target = marker != null && marker.regionId() != null ? marker.regionId() : region.definitionId();
        MissionCoreService.INSTANCE.recordObjective(player, MissionObjectiveType.DISCOVER_STRUCTURE,
                target, 1, context("region_scanned", region, marker));
    }

    private static void onMarkerRevealed(EchoWorldRuntimeBus.MarkerRevealed event) {
        ServerPlayer player = event.player();
        WorldMarker marker = event.marker();
        if (player == null || marker == null) {
            return;
        }
        Map<String, String> context = new LinkedHashMap<>();
        context.put("world_event", "marker_revealed");
        context.put("marker_id", marker.id().toString());
        context.put("marker_type", marker.type().name());
        if (marker.regionId() != null) {
            context.put("region_id", marker.regionId().toString());
        }
        MissionCoreService.INSTANCE.recordObjective(player, MissionObjectiveType.CUSTOM,
                MARKER_REVEALED, 1, context);
        MissionCoreService.INSTANCE.recordObjective(player, MissionObjectiveType.CUSTOM,
                marker.id(), 1, context);
    }

    private static void onHazardChanged(EchoWorldRuntimeBus.HazardChanged event) {
        ServerPlayer player = event.player();
        WorldHazardSnapshot current = event.current();
        if (player == null || current == null || current.safeZone()) {
            return;
        }
        Map<String, String> context = new LinkedHashMap<>();
        context.put("world_event", "hazard_changed");
        context.put("severity", Integer.toString(current.severity()));
        context.put("hazards", current.hazardIds().toString());
        MissionCoreService.INSTANCE.recordObjective(player, MissionObjectiveType.CUSTOM,
                HAZARD_CHANGED, 1, context);
        for (Identifier hazardId : current.hazardIds()) {
            MissionCoreService.INSTANCE.recordObjective(player, MissionObjectiveType.CUSTOM,
                    hazardId, 1, context);
        }
    }

    private static Map<String, String> context(String eventName, WorldRegionInstance region, WorldMarker marker) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("world_event", eventName);
        context.put("region_id", region.definitionId().toString());
        context.put("region_type", region.type().name());
        context.put("region_instance", region.id().toString());
        if (marker != null) {
            context.put("marker_id", marker.id().toString());
            context.put("marker_type", marker.type().name());
        }
        return context;
    }
}
