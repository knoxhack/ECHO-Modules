package com.knoxhack.echo.validationcore;

public enum EchoDiagnosticSeverity {
    INFO("INFO", 10, false),
    NOTICE("NOTICE", 20, false),
    WARNING("WARNING", 30, false),
    ERROR("ERROR", 40, true),
    FATAL("FATAL", 50, true),
    REPAIRABLE("REPAIRABLE", 35, false),
    EXPERIMENTAL("EXPERIMENTAL", 15, false),
    DEPRECATED("DEPRECATED", 25, false);

    private final String serializedName;
    private final int rank;
    private final boolean blocking;

    EchoDiagnosticSeverity(String serializedName, int rank, boolean blocking) {
        this.serializedName = serializedName;
        this.rank = rank;
        this.blocking = blocking;
    }

    public String serializedName() {
        return serializedName;
    }

    public int rank() {
        return rank;
    }

    public boolean blocking() {
        return blocking;
    }
}
