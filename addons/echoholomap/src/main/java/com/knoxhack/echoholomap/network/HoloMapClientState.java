package com.knoxhack.echoholomap.network;

import com.knoxhack.echoholomap.map.HoloMapVisibility;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HoloMapClientState {
    private static volatile HoloMapSnapshotPacket snapshot = HoloMapSnapshotPacket.empty();
    private static volatile SnapshotIndex index = SnapshotIndex.from(snapshot);

    private HoloMapClientState() {
    }

    public static void apply(HoloMapSnapshotPacket packet) {
        snapshot = packet == null ? HoloMapSnapshotPacket.empty() : packet;
        index = SnapshotIndex.from(snapshot);
    }

    public static HoloMapSnapshotPacket snapshot() {
        return snapshot;
    }

    public static HoloMapSnapshotPacket snapshotForDimension(String dimension) {
        return index.snapshotsByDimension().getOrDefault(normalizeDimension(dimension), index.emptySnapshot());
    }

    public static List<HoloMapSnapshotPacket.MarkerData> markersForDimension(String dimension) {
        return index.markersByDimension().getOrDefault(normalizeDimension(dimension), List.of());
    }

    public static List<HoloMapSnapshotPacket.RouteData> routesForDimension(String dimension) {
        return index.routesByDimension().getOrDefault(normalizeDimension(dimension), List.of());
    }

    public static List<HoloMapSnapshotPacket.OverlayData> overlaysForDimension(String dimension) {
        return index.overlaysByDimension().getOrDefault(normalizeDimension(dimension), List.of());
    }

    public static List<HoloMapSnapshotPacket.ZoneData> zonesForDimension(String dimension) {
        return index.zonesByDimension().getOrDefault(normalizeDimension(dimension), List.of());
    }

    private static String normalizeDimension(String dimension) {
        return dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.strip();
    }

    private record SnapshotIndex(
            Map<String, List<HoloMapSnapshotPacket.MarkerData>> markersByDimension,
            Map<String, List<HoloMapSnapshotPacket.RouteData>> routesByDimension,
            Map<String, List<HoloMapSnapshotPacket.OverlayData>> overlaysByDimension,
            Map<String, List<HoloMapSnapshotPacket.ZoneData>> zonesByDimension,
            Map<String, HoloMapSnapshotPacket> snapshotsByDimension,
            HoloMapSnapshotPacket emptySnapshot) {
        private static SnapshotIndex from(HoloMapSnapshotPacket snapshot) {
            Map<String, List<HoloMapSnapshotPacket.MarkerData>> markers = new LinkedHashMap<>();
            Map<String, List<HoloMapSnapshotPacket.RouteData>> routes = new LinkedHashMap<>();
            Map<String, List<HoloMapSnapshotPacket.OverlayData>> overlays = new LinkedHashMap<>();
            Map<String, List<HoloMapSnapshotPacket.ZoneData>> zones = new LinkedHashMap<>();
            for (HoloMapSnapshotPacket.MarkerData marker : snapshot.markers()) {
                if (!HoloMapVisibility.visibleInNormalView(marker.state())) {
                    continue;
                }
                markers.computeIfAbsent(normalizeDimension(marker.dimension()), ignored -> new java.util.ArrayList<>())
                        .add(marker);
            }
            for (HoloMapSnapshotPacket.RouteData route : snapshot.routes()) {
                if (!HoloMapVisibility.visibleInNormalView(route.state())) {
                    continue;
                }
                List<String> dimensions = route.points().stream()
                        .map(point -> normalizeDimension(point.dimension()))
                        .distinct()
                        .toList();
                if (dimensions.isEmpty()) {
                    dimensions = List.of(normalizeDimension(route.dimension()));
                }
                for (String dimension : dimensions) {
                    routes.computeIfAbsent(dimension, ignored -> new java.util.ArrayList<>()).add(route);
                }
            }
            for (HoloMapSnapshotPacket.OverlayData overlay : snapshot.overlays()) {
                if (!HoloMapVisibility.visibleInNormalView(overlay.state())) {
                    continue;
                }
                overlays.computeIfAbsent(normalizeDimension(overlay.dimension()), ignored -> new java.util.ArrayList<>())
                        .add(overlay);
            }
            for (HoloMapSnapshotPacket.ZoneData zone : snapshot.zones()) {
                if (!HoloMapVisibility.visibleInNormalView(zone.state())) {
                    continue;
                }
                zones.computeIfAbsent(normalizeDimension(zone.dimension()), ignored -> new java.util.ArrayList<>())
                        .add(zone);
            }
            Map<String, List<HoloMapSnapshotPacket.MarkerData>> frozenMarkers = freeze(markers);
            Map<String, List<HoloMapSnapshotPacket.RouteData>> frozenRoutes = freeze(routes);
            Map<String, List<HoloMapSnapshotPacket.OverlayData>> frozenOverlays = freeze(overlays);
            Map<String, List<HoloMapSnapshotPacket.ZoneData>> frozenZones = freeze(zones);
            HoloMapSnapshotPacket empty = new HoloMapSnapshotPacket(snapshot.layers(), List.of(), List.of(), List.of(),
                    List.of(), snapshot.diagnostics(), snapshot.statusLine(), snapshot.gameTime());
            return new SnapshotIndex(frozenMarkers, frozenRoutes, frozenOverlays, frozenZones,
                    filteredSnapshots(snapshot, frozenMarkers, frozenRoutes, frozenOverlays, frozenZones), empty);
        }

        private static Map<String, HoloMapSnapshotPacket> filteredSnapshots(HoloMapSnapshotPacket snapshot,
                Map<String, List<HoloMapSnapshotPacket.MarkerData>> markers,
                Map<String, List<HoloMapSnapshotPacket.RouteData>> routes,
                Map<String, List<HoloMapSnapshotPacket.OverlayData>> overlays,
                Map<String, List<HoloMapSnapshotPacket.ZoneData>> zones) {
            Map<String, HoloMapSnapshotPacket> snapshots = new LinkedHashMap<>();
            java.util.LinkedHashSet<String> dimensions = new java.util.LinkedHashSet<>();
            dimensions.addAll(markers.keySet());
            dimensions.addAll(routes.keySet());
            dimensions.addAll(overlays.keySet());
            dimensions.addAll(zones.keySet());
            for (String dimension : dimensions) {
                snapshots.put(dimension, new HoloMapSnapshotPacket(
                        snapshot.layers(),
                        markers.getOrDefault(dimension, List.of()),
                        routes.getOrDefault(dimension, List.of()),
                        overlays.getOrDefault(dimension, List.of()),
                        zones.getOrDefault(dimension, List.of()),
                        snapshot.diagnostics(),
                        snapshot.statusLine(),
                        snapshot.gameTime()));
            }
            return Map.copyOf(snapshots);
        }

        private static <T> Map<String, List<T>> freeze(Map<String, List<T>> input) {
            Map<String, List<T>> frozen = new LinkedHashMap<>();
            for (Map.Entry<String, List<T>> entry : input.entrySet()) {
                frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return Map.copyOf(frozen);
        }
    }
}
