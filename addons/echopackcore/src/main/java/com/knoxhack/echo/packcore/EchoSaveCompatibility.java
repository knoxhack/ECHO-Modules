package com.knoxhack.echo.packcore;

public enum EchoSaveCompatibility {
    COMPATIBLE("compatible"),
    COMPATIBLE_WITH_WARNINGS("compatible_with_warnings"),
    MIGRATION_REQUIRED("migration_required"),
    BACKUP_REQUIRED("backup_required"),
    INCOMPATIBLE("incompatible"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoSaveCompatibility(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
