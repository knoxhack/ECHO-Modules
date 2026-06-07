package com.knoxhack.echo.spawncore;

public enum EchoSpawnRuleKind {
    BIOME("biome"),
    HAZARD("hazard"),
    POI("poi"),
    STRUCTURE("structure"),
    FACTION("faction"),
    WEATHER("weather"),
    EVENT("event"),
    PROGRESSION("progression"),
    DIFFICULTY("difficulty"),
    LIGHT_LEVEL("light_level"),
    TIME_OF_DAY("time_of_day"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoSpawnRuleKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
