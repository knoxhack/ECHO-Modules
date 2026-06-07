package com.knoxhack.echorecovery.api;

public enum EchoRecoveryResultStatus {
    PLANNED("planned"),
    NEEDS_CONFIRMATION("needs_confirmation"),
    SKIPPED("skipped"),
    COMPLETED("completed"),
    FAILED("failed"),
    BLOCKED("blocked"),
    DEGRADED("degraded");

    private final String serializedName;

    EchoRecoveryResultStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
