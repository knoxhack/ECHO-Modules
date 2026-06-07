package com.knoxhack.echo.codexcore;

public enum EchoCodexDiscoveryState {
    HIDDEN("hidden"),
    TEASED("teased"),
    DISCOVERED("discovered"),
    UPDATED("updated"),
    ARCHIVED("archived"),
    LOCKED("locked"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoCodexDiscoveryState(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean visible() {
        return this == TEASED || this == DISCOVERED || this == UPDATED || this == ARCHIVED;
    }
}
