package com.knoxhack.echo.encountercore;

public enum EchoEncounterConstraintKind {
    REGION("region"),
    POI("poi"),
    STRUCTURE("structure"),
    FACTION("faction"),
    WORLD_EVENT("world_event"),
    PROGRESSION("progression"),
    DIFFICULTY("difficulty"),
    WEATHER("weather"),
    TIME_WINDOW("time_window"),
    PLAYER_COUNT("player_count"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoEncounterConstraintKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
