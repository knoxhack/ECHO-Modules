package com.knoxhack.echo.statuscore;

public enum EchoStatusSeverity {
    TRACE("trace"),
    MINOR("minor"),
    MODERATE("moderate"),
    SEVERE("severe"),
    CRITICAL("critical"),
    LETHAL("lethal"),
    STORY_LOCKED("story_locked"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoStatusSeverity(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
