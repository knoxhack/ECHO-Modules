package com.knoxhack.echo.questdirector;

public enum EchoDirectorSignalKind {
    MISSION_AVAILABLE("mission_available"),
    ROUTE_STALLED("route_stalled"),
    ROUTE_ADVANCED("route_advanced"),
    WORLD_EVENT_ACTIVE("world_event_active"),
    CAMPAIGN_PRESSURE_CHANGED("campaign_pressure_changed"),
    PLAYER_RECOVERING("player_recovering"),
    REMINDER_DUE("reminder_due"),
    OBJECTIVE_BLOCKED("objective_blocked"),
    OPTIONAL_CONTENT_READY("optional_content_ready"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoDirectorSignalKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
