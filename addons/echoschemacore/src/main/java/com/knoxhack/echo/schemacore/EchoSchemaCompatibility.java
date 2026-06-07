package com.knoxhack.echo.schemacore;

public enum EchoSchemaCompatibility {
    CURRENT("current"),
    BACKWARD_COMPATIBLE("backward_compatible"),
    FORWARD_COMPATIBLE("forward_compatible"),
    MIGRATION_AVAILABLE("migration_available"),
    MIGRATION_REQUIRED("migration_required"),
    INCOMPATIBLE("incompatible"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoSchemaCompatibility(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
