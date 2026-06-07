package com.knoxhack.echo.bridgecore;

public enum EchoBridgeSafeActionStatus {
    PENDING_CONFIRMATION("pending_confirmation"),
    APPROVED("approved"),
    REJECTED("rejected"),
    EXPIRED("expired"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELED("canceled"),
    BLOCKED("blocked");

    private final String serializedName;

    EchoBridgeSafeActionStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean terminal() {
        return this != PENDING_CONFIRMATION && this != APPROVED;
    }
}
