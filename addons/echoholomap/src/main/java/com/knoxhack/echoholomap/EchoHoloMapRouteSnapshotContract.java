package com.knoxhack.echoholomap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoHoloMapRouteSnapshotContract {
    public static final String MODULE_ID = "echoholomap";
    public static final String ADAPTERCORE_CONTRACT_ID = "echoholomap:layer/field_route";
    public static final String REFERENCE_ROUTE_ID = "echoashfallprotocol:route/first_power_cell";
    public static final String REFERENCE_REGION_ID = "echoashfallprotocol:wasteland_surface";

    private EchoHoloMapRouteSnapshotContract() {
    }

    public static Map<String, Object> executeReferenceSnapshot(String routeId, String regionId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        snapshot.put("service", "echoholomap:map_service");
        snapshot.put("snapshotExecuted", true);
        snapshot.put("routeId", routeId);
        snapshot.put("regionId", regionId);
        snapshot.put("center", Map.of("x", 128, "z", -64, "zoom", 3));
        snapshot.put("terrainTiles", List.of(
                tile("ash_waste", 7, 5, "safe"),
                tile("ruined_road", 8, 5, "route"),
                tile("radiation_pocket", 9, 5, "hazard")
        ));
        snapshot.put("markers", List.of(
                marker("mission", "echoterminal:field_ops/first_ten_minutes", 128, -64),
                marker("hazard", "echoashfallprotocol:ash_storm", 160, -48),
                marker("waypoint", "echoholomap:waypoint/field_cache", 192, -32)
        ));
        snapshot.put("routePoints", List.of(
                point(128, -64, "start"),
                point(160, -48, "avoid_hazard"),
                point(192, -32, "field_cache")
        ));
        snapshot.put("visibleLayerIds", List.of(
                "echoholomap:terrain",
                "echoholomap:mission_markers",
                "echoholomap:hazards",
                "echoholomap:waypoints"
        ));
        snapshot.put("referenceBehavior", "holomap_route_snapshot_guides_first_power_cell");
        return Map.copyOf(snapshot);
    }

    public static boolean referenceSnapshotPassed(Map<String, Object> snapshot) {
        return Boolean.TRUE.equals(snapshot.get("snapshotExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(snapshot.get("adapterCoreContract"))
                && REFERENCE_ROUTE_ID.equals(snapshot.get("routeId"))
                && REFERENCE_REGION_ID.equals(snapshot.get("regionId"))
                && list(snapshot.get("visibleLayerIds")).contains("echoholomap:mission_markers")
                && String.valueOf(snapshot.get("markers")).contains("echoashfallprotocol:ash_storm")
                && String.valueOf(snapshot.get("routePoints")).contains("field_cache");
    }

    private static Map<String, Object> tile(String biome, int x, int z, String state) {
        Map<String, Object> tile = new LinkedHashMap<>();
        tile.put("biome", biome);
        tile.put("x", x);
        tile.put("z", z);
        tile.put("state", state);
        return Map.copyOf(tile);
    }

    private static Map<String, Object> marker(String kind, String id, int x, int z) {
        Map<String, Object> marker = new LinkedHashMap<>();
        marker.put("kind", kind);
        marker.put("id", id);
        marker.put("x", x);
        marker.put("z", z);
        return Map.copyOf(marker);
    }

    private static Map<String, Object> point(int x, int z, String label) {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("x", x);
        point.put("z", z);
        point.put("label", label);
        return Map.copyOf(point);
    }

    private static List<String> list(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
