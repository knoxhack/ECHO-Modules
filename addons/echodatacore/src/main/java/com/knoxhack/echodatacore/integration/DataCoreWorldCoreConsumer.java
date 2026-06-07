package com.knoxhack.echodatacore.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoWorldRuntimeBus;
import com.knoxhack.echocore.api.IDataKey;
import com.knoxhack.echocore.api.IDataView;
import com.knoxhack.echocore.api.WorldHazardSnapshot;
import com.knoxhack.echocore.api.WorldMarker;
import com.knoxhack.echocore.api.WorldRegionInstance;
import com.knoxhack.echodatacore.DataCoreBuiltinKeys;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Consumes WorldCore runtime events through ECHO Core only. This keeps DataCore
 * useful for worlds with WorldCore while still loading safely without it.
 */
public final class DataCoreWorldCoreConsumer {
    private static boolean registered;

    private DataCoreWorldCoreConsumer() {
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
        EchoWorldRuntimeBus.onRegionEntered(DataCoreWorldCoreConsumer::onRegionEntered);
        EchoWorldRuntimeBus.onRegionDiscovered(DataCoreWorldCoreConsumer::onRegionDiscovered);
        EchoWorldRuntimeBus.onRegionScanned(DataCoreWorldCoreConsumer::onRegionScanned);
        EchoWorldRuntimeBus.onMarkerRevealed(DataCoreWorldCoreConsumer::onMarkerRevealed);
        EchoWorldRuntimeBus.onHazardChanged(DataCoreWorldCoreConsumer::onHazardChanged);
    }

    private static void onRegionEntered(EchoWorldRuntimeBus.RegionEntered event) {
        ServerPlayer player = event.player();
        WorldRegionInstance region = event.region();
        if (player == null || region == null) {
            return;
        }
        EchoCoreServices.playerData(player).set(DataCoreBuiltinKeys.WORLDCORE_LAST_REGION,
                region.definitionId().toString());
    }

    private static void onRegionDiscovered(EchoWorldRuntimeBus.RegionDiscovered event) {
        ServerPlayer player = event.player();
        WorldRegionInstance region = event.region();
        if (player == null || region == null) {
            return;
        }
        IDataView playerData = EchoCoreServices.playerData(player);
        playerData.set(DataCoreBuiltinKeys.WORLDCORE_LAST_REGION, region.definitionId().toString());
        playerData.set(DataCoreBuiltinKeys.WORLDCORE_LAST_DISCOVERY_SOURCE,
                event.source() == null ? "" : event.source().name());
        if (event.firstDiscovery()) {
            increment(playerData, DataCoreBuiltinKeys.WORLDCORE_REGION_DISCOVERIES);
            increment(EchoCoreServices.worldData(player.level()), DataCoreBuiltinKeys.WORLDCORE_WORLD_REGION_DISCOVERIES);
        }
    }

    private static void onRegionScanned(EchoWorldRuntimeBus.RegionScanned event) {
        ServerPlayer player = event.player();
        if (player == null || event.marker() == null) {
            return;
        }
        EchoCoreServices.playerData(player).set(DataCoreBuiltinKeys.WORLDCORE_LAST_MARKER,
                event.marker().id().toString());
    }

    private static void onMarkerRevealed(EchoWorldRuntimeBus.MarkerRevealed event) {
        ServerPlayer player = event.player();
        WorldMarker marker = event.marker();
        if (player == null || marker == null) {
            return;
        }
        IDataView playerData = EchoCoreServices.playerData(player);
        playerData.set(DataCoreBuiltinKeys.WORLDCORE_LAST_MARKER, marker.id().toString());
        increment(playerData, DataCoreBuiltinKeys.WORLDCORE_MARKERS_REVEALED);
        increment(EchoCoreServices.worldData(player.level()), DataCoreBuiltinKeys.WORLDCORE_WORLD_MARKERS_REVEALED);
    }

    private static void onHazardChanged(EchoWorldRuntimeBus.HazardChanged event) {
        ServerPlayer player = event.player();
        WorldHazardSnapshot current = event.current();
        if (player == null || current == null) {
            return;
        }
        IDataView playerData = EchoCoreServices.playerData(player);
        playerData.set(DataCoreBuiltinKeys.WORLDCORE_ACTIVE_HAZARDS, current.hazardIds().stream()
                .map(Identifier::toString)
                .collect(Collectors.joining(",")));
        playerData.set(DataCoreBuiltinKeys.WORLDCORE_ACTIVE_HAZARD_SEVERITY, (long) current.severity());
        increment(EchoCoreServices.worldData(player.level()), DataCoreBuiltinKeys.WORLDCORE_WORLD_HAZARD_CHANGES);
    }

    private static void increment(IDataView view, IDataKey<Long> key) {
        if (view == null || key == null) {
            return;
        }
        Long current = view.get(key);
        view.set(key, Math.max(0L, current == null ? 1L : current + 1L));
    }
}
