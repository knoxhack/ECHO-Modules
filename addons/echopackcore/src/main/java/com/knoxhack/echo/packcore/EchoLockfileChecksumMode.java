package com.knoxhack.echo.packcore;

public enum EchoLockfileChecksumMode {
    JAR("jar"),
    SOURCE_METADATA("source_metadata"),
    RESOURCE_METADATA("resource_metadata"),
    SYNTHETIC("synthetic"),
    MISSING("missing"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoLockfileChecksumMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
