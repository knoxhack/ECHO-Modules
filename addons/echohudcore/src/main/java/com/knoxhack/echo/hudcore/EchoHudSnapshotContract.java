package com.knoxhack.echo.hudcore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoHudSnapshotContract {
    public static final String MODULE_ID = "echohudcore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echohudcore:hud/runtime_snapshot";
    public static final String REFERENCE_MISSION_ID = "echohudcore:field_mission";
    public static final String REFERENCE_HAZARD_ID = "echohudcore:field_hazard";

    private EchoHudSnapshotContract() {
    }

    public static Map<String, Object> executeReferenceSnapshot(String missionId, String hazardId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        snapshot.put("service", "echohudcore:hud_service");
        snapshot.put("snapshotExecuted", true);
        snapshot.put("missionId", missionId);
        snapshot.put("hazardId", hazardId);
        snapshot.put("screenSafeArea", Map.of(
                "left", 12,
                "top", 10,
                "right", 12,
                "bottom", 18
        ));
        snapshot.put("widgets", List.of(
                widget("echohudcore:mission_tracker", "top_left", List.of(
                        row("mission", missionId),
                        row("objective", "Craft a power cell"),
                        row("state", "active")
                )),
                widget("echohudcore:hazard_readout", "top_right", List.of(
                        row("hazard", hazardId),
                        row("severity", "warning"),
                        row("countermeasure", "seek_shelter")
                )),
                widget("echohudcore:compass_indicator", "bottom_center", List.of(
                        row("target", "echoterminal:field_ops/first_ten_minutes"),
                        row("distance", "128m"),
                        row("bearing", "NE")
                ))
        ));
        snapshot.put("visibleWidgetIds", List.of(
                "echohudcore:mission_tracker",
                "echohudcore:hazard_readout",
                "echohudcore:compass_indicator"
        ));
        snapshot.put("referenceBehavior", "hud_snapshot_tracks_mission_hazard_and_safe_area");
        return Map.copyOf(snapshot);
    }

    public static boolean referenceSnapshotPassed(Map<String, Object> snapshot) {
        return Boolean.TRUE.equals(snapshot.get("snapshotExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(snapshot.get("adapterCoreContract"))
                && REFERENCE_MISSION_ID.equals(snapshot.get("missionId"))
                && REFERENCE_HAZARD_ID.equals(snapshot.get("hazardId"))
                && list(snapshot.get("visibleWidgetIds")).contains("echohudcore:mission_tracker")
                && list(snapshot.get("visibleWidgetIds")).contains("echohudcore:hazard_readout")
                && String.valueOf(snapshot.get("widgets")).contains("seek_shelter");
    }

    private static Map<String, Object> widget(
            String id,
            String anchor,
            List<Map<String, String>> rows
    ) {
        Map<String, Object> widget = new LinkedHashMap<>();
        widget.put("id", id);
        widget.put("anchor", anchor);
        widget.put("rows", List.copyOf(rows));
        return Map.copyOf(widget);
    }

    private static Map<String, String> row(String key, String value) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("value", value);
        return Map.copyOf(row);
    }

    private static List<String> list(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
