package com.knoxhack.echo.metadatacore;

public enum EchoMetadataStatus {
    PRESENT("present"),
    MISSING("missing"),
    INVALID_JSON("invalid_json"),
    INVALID_SCHEMA("invalid_schema"),
    CONFLICT("conflict"),
    FALLBACK("fallback"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoMetadataStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
