package com.knoxhack.echo.packcore;

public enum EchoPackState {
    NOT_INSTALLED("not_installed"),
    INSTALLED("installed"),
    READY("ready"),
    DEGRADED("degraded"),
    UPDATE_AVAILABLE("update_available"),
    REPAIR_REQUIRED("repair_required"),
    MIGRATION_REQUIRED("migration_required"),
    INCOMPATIBLE("incompatible"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoPackState(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
