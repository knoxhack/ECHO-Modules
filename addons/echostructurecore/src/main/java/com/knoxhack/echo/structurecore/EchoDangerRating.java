package com.knoxhack.echo.structurecore;

public enum EchoDangerRating {
    NONE("none", 0),
    LOW("low", 1),
    MODERATE("moderate", 2),
    HIGH("high", 3),
    EXTREME("extreme", 4),
    STORY_LOCKED("story_locked", 5),
    UNKNOWN("unknown", -1);

    private final String serializedName;
    private final int level;

    EchoDangerRating(String serializedName, int level) {
        this.serializedName = serializedName;
        this.level = level;
    }

    public String serializedName() {
        return serializedName;
    }

    public int level() {
        return level;
    }
}
