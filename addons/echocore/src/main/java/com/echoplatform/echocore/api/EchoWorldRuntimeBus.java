package com.echoplatform.echocore.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class EchoWorldRuntimeBus {
    private static final List<Consumer<RegionEntered>> REGION_ENTERED = new ArrayList<>();
    private static final List<Consumer<RegionExited>> REGION_EXITED = new ArrayList<>();
    private static final List<Consumer<RegionDiscovered>> REGION_DISCOVERED = new ArrayList<>();
    private static final List<Consumer<RegionScanned>> REGION_SCANNED = new ArrayList<>();
    private static final List<Consumer<MarkerRevealed>> MARKER_REVEALED = new ArrayList<>();
    private static final List<Consumer<HazardChanged>> HAZARD_CHANGED = new ArrayList<>();
    private static final List<Consumer<StatusEffectSaved>> STATUS_EFFECT_SAVED = new ArrayList<>();
    private static final List<Consumer<StatusEffectLoaded>> STATUS_EFFECT_LOADED = new ArrayList<>();
    private static final List<Consumer<SpawnRuleTriggered>> SPAWN_RULE_TRIGGERED = new ArrayList<>();
    private static final List<Consumer<WorldCellSampled>> WORLD_CELL_SAMPLED = new ArrayList<>();
    private static final List<Consumer<StructurePoiResolved>> STRUCTURE_POI_RESOLVED = new ArrayList<>();
    private static final List<Consumer<WeatherSurfaceApplied>> WEATHER_SURFACE_APPLIED = new ArrayList<>();

    private EchoWorldRuntimeBus() {
    }

    public static AutoCloseable onRegionEntered(Consumer<RegionEntered> listener) {
        return subscribe(REGION_ENTERED, listener);
    }

    public static AutoCloseable onRegionExited(Consumer<RegionExited> listener) {
        return subscribe(REGION_EXITED, listener);
    }

    public static AutoCloseable onRegionDiscovered(Consumer<RegionDiscovered> listener) {
        return subscribe(REGION_DISCOVERED, listener);
    }

    public static AutoCloseable onRegionScanned(Consumer<RegionScanned> listener) {
        return subscribe(REGION_SCANNED, listener);
    }

    public static AutoCloseable onMarkerRevealed(Consumer<MarkerRevealed> listener) {
        return subscribe(MARKER_REVEALED, listener);
    }

    public static AutoCloseable onHazardChanged(Consumer<HazardChanged> listener) {
        return subscribe(HAZARD_CHANGED, listener);
    }

    public static AutoCloseable onStatusEffectSaved(Consumer<StatusEffectSaved> listener) {
        return subscribe(STATUS_EFFECT_SAVED, listener);
    }

    public static AutoCloseable onStatusEffectLoaded(Consumer<StatusEffectLoaded> listener) {
        return subscribe(STATUS_EFFECT_LOADED, listener);
    }

    public static AutoCloseable onSpawnRuleTriggered(Consumer<SpawnRuleTriggered> listener) {
        return subscribe(SPAWN_RULE_TRIGGERED, listener);
    }

    public static AutoCloseable onWorldCellSampled(Consumer<WorldCellSampled> listener) {
        return subscribe(WORLD_CELL_SAMPLED, listener);
    }

    public static AutoCloseable onStructurePoiResolved(Consumer<StructurePoiResolved> listener) {
        return subscribe(STRUCTURE_POI_RESOLVED, listener);
    }

    public static AutoCloseable onWeatherSurfaceApplied(Consumer<WeatherSurfaceApplied> listener) {
        return subscribe(WEATHER_SURFACE_APPLIED, listener);
    }

    public static void fireRegionEntered(RegionEntered event) {
        publish(REGION_ENTERED, event);
    }

    public static void fireRegionExited(RegionExited event) {
        publish(REGION_EXITED, event);
    }

    public static void fireRegionDiscovered(RegionDiscovered event) {
        publish(REGION_DISCOVERED, event);
    }

    public static void fireRegionScanned(RegionScanned event) {
        publish(REGION_SCANNED, event);
    }

    public static void fireMarkerRevealed(MarkerRevealed event) {
        publish(MARKER_REVEALED, event);
    }

    public static void fireHazardChanged(HazardChanged event) {
        publish(HAZARD_CHANGED, event);
    }

    public static void fireStatusEffectSaved(StatusEffectSaved event) {
        publish(STATUS_EFFECT_SAVED, event);
    }

    public static void fireStatusEffectLoaded(StatusEffectLoaded event) {
        publish(STATUS_EFFECT_LOADED, event);
    }

    public static void fireSpawnRuleTriggered(SpawnRuleTriggered event) {
        publish(SPAWN_RULE_TRIGGERED, event);
    }

    public static void fireWorldCellSampled(WorldCellSampled event) {
        publish(WORLD_CELL_SAMPLED, event);
    }

    public static void fireStructurePoiResolved(StructurePoiResolved event) {
        publish(STRUCTURE_POI_RESOLVED, event);
    }

    public static void fireWeatherSurfaceApplied(WeatherSurfaceApplied event) {
        publish(WEATHER_SURFACE_APPLIED, event);
    }

    public static synchronized void clearForTests() {
        REGION_ENTERED.clear();
        REGION_EXITED.clear();
        REGION_DISCOVERED.clear();
        REGION_SCANNED.clear();
        MARKER_REVEALED.clear();
        HAZARD_CHANGED.clear();
        STATUS_EFFECT_SAVED.clear();
        STATUS_EFFECT_LOADED.clear();
        SPAWN_RULE_TRIGGERED.clear();
        WORLD_CELL_SAMPLED.clear();
        STRUCTURE_POI_RESOLVED.clear();
        WEATHER_SURFACE_APPLIED.clear();
    }

    private static synchronized <T> AutoCloseable subscribe(List<Consumer<T>> listeners, Consumer<T> listener) {
        if (listener == null) {
            return () -> { };
        }
        listeners.add(listener);
        return () -> unsubscribe(listeners, listener);
    }

    private static synchronized <T> void unsubscribe(List<Consumer<T>> listeners, Consumer<T> listener) {
        listeners.remove(listener);
    }

    private static <T> void publish(List<Consumer<T>> listeners, T event) {
        if (event == null) {
            return;
        }
        List<Consumer<T>> snapshot;
        synchronized (EchoWorldRuntimeBus.class) {
            snapshot = List.copyOf(listeners);
        }
        for (Consumer<T> listener : snapshot) {
            listener.accept(event);
        }
    }

    public record RegionEntered(ServerPlayer player, WorldRegionInstance region) {
    }

    public record RegionExited(ServerPlayer player, WorldRegionInstance region) {
    }

    public record RegionDiscovered(ServerPlayer player, WorldRegionInstance region, WorldDiscoverySource source,
            boolean firstDiscovery) {
    }

    public record RegionScanned(ServerPlayer player, WorldRegionInstance region, WorldMarker marker) {
    }

    public record MarkerRevealed(ServerPlayer player, WorldMarker marker) {
    }

    public record HazardChanged(ServerPlayer player, WorldHazardSnapshot previous, WorldHazardSnapshot current) {
    }

    public record StatusEffectSaved(ServerPlayer player, String hazardId, String effectId, String saveKey,
            boolean saved) {
    }

    public record StatusEffectLoaded(ServerPlayer player, String hazardId, String effectId, String saveKey,
            boolean loaded) {
    }

    public record SpawnRuleTriggered(ServerPlayer player, WorldRegionInstance region, String ruleId, String entityId,
            int scaledBudget, int spawnCount, double spawnMultiplier) {
    }

    public record WorldCellSampled(ServerPlayer player, String worldId, BlockPos pos, String activeRegionId,
            String activeHazardId, boolean inRegion, boolean inHazard) {
    }

    public record StructurePoiResolved(ServerPlayer player, WorldRegionInstance region, String structureId,
            String poiId, BlockPos pos, boolean inRange) {
    }

    public record WeatherSurfaceApplied(ServerLevel level, String eventId, String weatherId, String phase,
            Map<String, Object> hudState, Map<String, Object> audioState, Map<String, Object> renderState,
            boolean applied) {
        public WeatherSurfaceApplied {
            hudState = hudState == null ? Map.of() : Map.copyOf(hudState);
            audioState = audioState == null ? Map.of() : Map.copyOf(audioState);
            renderState = renderState == null ? Map.of() : Map.copyOf(renderState);
        }
    }
}
