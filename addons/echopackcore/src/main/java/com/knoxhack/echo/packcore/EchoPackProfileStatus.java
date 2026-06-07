package com.knoxhack.echo.packcore;

public enum EchoPackProfileStatus {
    VALID("valid"),
    VALID_WITH_WARNINGS("valid_with_warnings"),
    INVALID("invalid"),
    INVALID_JSON("invalid_json"),
    MISSING("missing"),
    FALLBACK("fallback"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoPackProfileStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean usable() {
        return this == VALID || this == VALID_WITH_WARNINGS || this == FALLBACK;
    }
}
