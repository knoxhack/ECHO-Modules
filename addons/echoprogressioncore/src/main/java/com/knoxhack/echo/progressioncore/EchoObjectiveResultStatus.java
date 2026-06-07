package com.knoxhack.echo.progressioncore;

public enum EchoObjectiveResultStatus {
    NOT_STARTED("not_started"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELED("canceled"),
    BLOCKED("blocked"),
    DEGRADED("degraded"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoObjectiveResultStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == CANCELED || this == BLOCKED;
    }
}
