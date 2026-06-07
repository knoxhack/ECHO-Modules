package com.knoxhack.echo.spawncore;

public enum EchoSpawnDensityPolicy {
    VANILLA_COMPATIBLE("vanilla_compatible"),
    LOW("low"),
    STANDARD("standard"),
    HIGH("high"),
    HORDE("horde"),
    BOSS_ONLY("boss_only"),
    DISABLED("disabled"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoSpawnDensityPolicy(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
