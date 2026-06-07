package com.knoxhack.echo.progressioncore;

public enum EchoUnlockState {
    HIDDEN("hidden"),
    LOCKED("locked"),
    AVAILABLE("available"),
    UNLOCKED("unlocked"),
    BLOCKED("blocked"),
    DEGRADED("degraded"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoUnlockState(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean visible() {
        return this != HIDDEN;
    }

    public boolean unlocked() {
        return this == UNLOCKED;
    }
}
