package com.knoxhack.echo.schemacore;

public enum EchoSchemaIssueSeverity {
    INFO("info"),
    NOTICE("notice"),
    WARNING("warning"),
    ERROR("error"),
    FATAL("fatal"),
    DEPRECATED("deprecated"),
    EXPERIMENTAL("experimental");

    private final String serializedName;

    EchoSchemaIssueSeverity(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
