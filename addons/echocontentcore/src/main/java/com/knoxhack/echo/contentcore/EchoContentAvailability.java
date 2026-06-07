package com.knoxhack.echo.contentcore;

public enum EchoContentAvailability {
    PRESENT("present", false),
    OPTIONAL_PRESENT("optional_present", false),
    OPTIONAL_MISSING("optional_missing", false),
    MISSING("missing", true),
    DISABLED("disabled", false),
    DEGRADED("degraded", false),
    CONFLICTING("conflicting", true),
    BLOCKED("blocked", true),
    UNKNOWN("unknown", false);

    private final String serializedName;
    private final boolean blocking;

    EchoContentAvailability(String serializedName, boolean blocking) {
        this.serializedName = serializedName;
        this.blocking = blocking;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean blocking() {
        return blocking;
    }

    public boolean available() {
        return this == PRESENT || this == OPTIONAL_PRESENT;
    }
}
