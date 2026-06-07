package com.knoxhack.echo.eventcore;

public enum EchoEventTriggerKind {
    WORLD_TIME("world_time"),
    WEATHER_STATE("weather_state"),
    REGION_ENTERED("region_entered"),
    POI_DISCOVERED("poi_discovered"),
    FACTION_REPUTATION("faction_reputation"),
    STRUCTURE_DISCOVERED("structure_discovered"),
    PROGRESSION_UNLOCK("progression_unlock"),
    OBJECTIVE_COMPLETED("objective_completed"),
    SERVER_SIGNAL("server_signal"),
    HEALTH_DEGRADED("health_degraded"),
    RANDOM_ROLL("random_roll"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoEventTriggerKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
