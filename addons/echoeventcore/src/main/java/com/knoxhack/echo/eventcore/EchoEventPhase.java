package com.knoxhack.echo.eventcore;

public enum EchoEventPhase {
    PLANNED("planned"),
    ELIGIBLE("eligible"),
    STARTING("starting"),
    ACTIVE("active"),
    ESCALATING("escalating"),
    RESOLVING("resolving"),
    COMPLETED("completed"),
    FAILED("failed"),
    COOLDOWN("cooldown"),
    DISABLED("disabled"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoEventPhase(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
