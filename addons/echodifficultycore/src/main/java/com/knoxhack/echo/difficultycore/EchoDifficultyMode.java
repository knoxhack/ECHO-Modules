package com.knoxhack.echo.difficultycore;

public enum EchoDifficultyMode {
    STATIC("static"),
    ADAPTIVE("adaptive"),
    SERVER_LOCKED("server_locked"),
    PACK_VARIANT("pack_variant"),
    STORY("story"),
    SURVIVAL("survival"),
    HARDCORE("hardcore"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoDifficultyMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
