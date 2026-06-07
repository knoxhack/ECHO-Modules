package com.knoxhack.echo.reportcore;

public enum EchoReportStatus {
    PASS("PASS", false),
    PASS_WITH_WARNINGS("PASS_WITH_WARNINGS", false),
    DEGRADED("DEGRADED", false),
    FAILED("FAILED", true),
    BLOCKED("BLOCKED", true),
    NOT_RUN("NOT_RUN", false),
    UNKNOWN("UNKNOWN", false);

    private final String serializedName;
    private final boolean blocking;

    EchoReportStatus(String serializedName, boolean blocking) {
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
