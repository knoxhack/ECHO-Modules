package com.knoxhack.echo.packcore;

public enum EchoInstallStateStatus {
    NOT_CONFIGURED("not_configured"),
    TARGET_MISSING("target_missing"),
    TARGET_UNREADABLE("target_unreadable"),
    CLEAN("clean"),
    DRIFT_DETECTED("drift_detected"),
    BLOCKED("blocked"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoInstallStateStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
