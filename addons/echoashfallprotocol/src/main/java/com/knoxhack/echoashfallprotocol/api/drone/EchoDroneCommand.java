package com.knoxhack.echoashfallprotocol.api.drone;

import java.util.Locale;

public enum EchoDroneCommand {
    SCAN_AREA("scan_area"),
    SCOUT_AHEAD("scout_ahead"),
    COLLECT_SCRAP("collect_scrap"),
    GUARD_HERE("guard_here"),
    RECALL("recall"),
    TOGGLE_ASSIST("toggle_assist"),
    STATUS("status"),
    TOGGLE_LIGHT("toggle_light"),
    SET_MODE("set_mode"),
    UNKNOWN("unknown");

    private final String id;

    EchoDroneCommand(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static EchoDroneCommand parse(String raw) {
        String command = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return switch (command) {
            case "scan", "scan_area", "area_scan" -> SCAN_AREA;
            case "scout", "scout_ahead" -> SCOUT_AHEAD;
            case "collect", "collect_scrap", "salvage", "scavenge", "scavenge_area" -> COLLECT_SCRAP;
            case "guard", "guard_here" -> GUARD_HERE;
            case "recall", "return" -> RECALL;
            case "assist", "toggle_assist" -> TOGGLE_ASSIST;
            case "status", "info" -> STATUS;
            case "light", "toggle_light" -> TOGGLE_LIGHT;
            case "follow", "dock", "combat", "patrol", "scout_mode", "salvage_mode", "guard_mode",
                    "patrol_mode" -> SET_MODE;
            default -> UNKNOWN;
        };
    }
}
