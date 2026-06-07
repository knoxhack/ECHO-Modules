package com.knoxhack.echo.reportcore;

public enum EchoReportIssueSeverity {
    NOTICE("NOTICE", false),
    WARNING("WARNING", false),
    ERROR("ERROR", true),
    FATAL("FATAL", true),
    UNKNOWN("UNKNOWN", false);

    private final String serializedName;
    private final boolean blocking;

    EchoReportIssueSeverity(String serializedName, boolean blocking) {
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
