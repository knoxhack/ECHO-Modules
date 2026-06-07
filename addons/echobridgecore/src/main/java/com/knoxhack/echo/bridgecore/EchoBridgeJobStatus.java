package com.knoxhack.echo.bridgecore;

public enum EchoBridgeJobStatus {
    QUEUED("queued"),
    STARTING("starting"),
    RUNNING("running"),
    WAITING_FOR_OUTPUT("waiting_for_output"),
    STREAMING("streaming"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELED("canceled"),
    BLOCKED("blocked"),
    NEEDS_CONFIRMATION("needs_confirmation");

    private final String serializedName;

    EchoBridgeJobStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean active() {
        return this == STARTING || this == RUNNING || this == WAITING_FOR_OUTPUT || this == STREAMING;
    }

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == CANCELED;
    }

    public boolean requiresAttention() {
        return this == BLOCKED || this == NEEDS_CONFIRMATION || this == FAILED;
    }
}
