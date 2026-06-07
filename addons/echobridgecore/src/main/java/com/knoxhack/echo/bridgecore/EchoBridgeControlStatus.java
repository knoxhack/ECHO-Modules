package com.knoxhack.echo.bridgecore;

public enum EchoBridgeControlStatus {
    ACCEPTED("accepted"),
    QUEUED("queued"),
    RUNNING("running"),
    STREAMING("streaming"),
    NEEDS_CONFIRMATION("needs_confirmation"),
    COMPLETED("completed"),
    CANCELED("canceled"),
    BLOCKED("blocked"),
    NOT_CONFIGURED("not_configured"),
    UNAVAILABLE("unavailable"),
    REJECTED("rejected"),
    FAILED("failed");

    private final String serializedName;

    EchoBridgeControlStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean terminal() {
        return this == COMPLETED || this == CANCELED || this == BLOCKED || this == REJECTED || this == FAILED;
    }

    public boolean requiresAttention() {
        return this == NEEDS_CONFIRMATION || this == BLOCKED || this == NOT_CONFIGURED || this == UNAVAILABLE || this == FAILED;
    }
}
