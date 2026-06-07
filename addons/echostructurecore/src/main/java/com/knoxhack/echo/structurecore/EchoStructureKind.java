package com.knoxhack.echo.structurecore;

public enum EchoStructureKind {
    RUIN("ruin"),
    FACILITY("facility"),
    OUTPOST("outpost"),
    RELAY("relay"),
    DUNGEON("dungeon"),
    SETTLEMENT("settlement"),
    VEHICLE_WRECK("vehicle_wreck"),
    ORBITAL_DEBRIS("orbital_debris"),
    RIFT_SITE("rift_site"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoStructureKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
