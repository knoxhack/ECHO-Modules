package com.knoxhack.echo.packcore;

public enum EchoLockfileVerificationStatus {
    VALID("valid", false),
    VALID_WITH_WARNINGS("valid_with_warnings", false),
    DRIFT_DETECTED("drift_detected", false),
    REPAIRABLE("repairable", false),
    BLOCKED("blocked", true),
    UNSUPPORTED("unsupported", true),
    MISSING("missing", false),
    UNKNOWN("unknown", false);

    private final String serializedName;
    private final boolean blocking;

    EchoLockfileVerificationStatus(String serializedName, boolean blocking) {
        this.serializedName = serializedName;
        this.blocking = blocking;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean blocking() {
        return blocking;
    }
}
