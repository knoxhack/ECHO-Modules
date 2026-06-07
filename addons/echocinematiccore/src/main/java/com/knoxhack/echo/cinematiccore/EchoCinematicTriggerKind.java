package com.knoxhack.echo.cinematiccore;

public enum EchoCinematicTriggerKind {
    WORLD_START("world_start"),
    MISSION_STARTED("mission_started"),
    MISSION_COMPLETED("mission_completed"),
    BOSS_DISCOVERED("boss_discovered"),
    BOSS_DEFEATED("boss_defeated"),
    DROP_POD_DEPLOYED("drop_pod_deployed"),
    TERMINAL_OPENED("terminal_opened"),
    REGION_ENTERED("region_entered"),
    MANUAL("manual"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoCinematicTriggerKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
