package com.knoxhack.echo.lootcore;

public enum EchoDuplicationPolicy {
    ALLOW("allow"),
    UNIQUE_PER_PLAYER("unique_per_player"),
    UNIQUE_PER_TEAM("unique_per_team"),
    UNIQUE_PER_WORLD("unique_per_world"),
    COOLDOWN("cooldown"),
    STORY_ONCE("story_once"),
    BLOCK_DUPLICATES("block_duplicates"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoDuplicationPolicy(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean restrictive() {
        return this != ALLOW && this != UNKNOWN;
    }
}
