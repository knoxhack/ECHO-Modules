package com.knoxhack.echo.packcore;

public enum EchoLockfileSource {
    PACK_PROFILE("pack_profile"),
    WORKSPACE_SCAN("workspace_scan"),
    BUILT_JAR("built_jar"),
    SOURCE_METADATA("source_metadata"),
    RESOURCE_METADATA("resource_metadata"),
    SYNTHETIC("synthetic"),
    GENERATED_REPORT("generated_report"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoLockfileSource(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
