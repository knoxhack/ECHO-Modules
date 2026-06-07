package com.knoxhack.echo.cameracore;

public enum EchoCameraMode {
    CINEMATIC("cinematic"),
    SHOWCASE("showcase"),
    NPC_CONVERSATION("npc_conversation"),
    VEHICLE("vehicle"),
    SCREENSHOT("screenshot"),
    CREATOR_TOOL("creator_tool"),
    DEBUG("debug"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoCameraMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
