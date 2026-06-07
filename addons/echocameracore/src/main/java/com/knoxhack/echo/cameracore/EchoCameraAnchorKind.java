package com.knoxhack.echo.cameracore;

public enum EchoCameraAnchorKind {
    PLAYER("player"),
    NPC("npc"),
    VEHICLE("vehicle"),
    STRUCTURE("structure"),
    POI("poi"),
    WORLD_POINT("world_point"),
    CUTSCENE_MARKER("cutscene_marker"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoCameraAnchorKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
