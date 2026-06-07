package com.knoxhack.echo.machinecore;

public enum EchoMachineState {
    LOCKED("locked"),
    IDLE("idle"),
    ACTIVE("active"),
    PAUSED("paused"),
    JAMMED("jammed"),
    DAMAGED("damaged"),
    MAINTENANCE_REQUIRED("maintenance_required"),
    POWER_STARVED("power_starved"),
    OVERLOADED("overloaded"),
    OFFLINE("offline"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoMachineState(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean degraded() {
        return this == JAMMED || this == DAMAGED || this == MAINTENANCE_REQUIRED
                || this == POWER_STARVED || this == OVERLOADED || this == OFFLINE;
    }
}
