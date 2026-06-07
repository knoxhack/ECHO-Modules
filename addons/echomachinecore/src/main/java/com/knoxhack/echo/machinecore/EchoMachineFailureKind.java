package com.knoxhack.echo.machinecore;

public enum EchoMachineFailureKind {
    NONE("none"),
    WEAR("wear"),
    POWER_LOSS("power_loss"),
    OVERHEAT("overheat"),
    JAM("jam"),
    INPUT_MISSING("input_missing"),
    OUTPUT_BLOCKED("output_blocked"),
    UPGRADE_CONFLICT("upgrade_conflict"),
    STRUCTURE_INVALID("structure_invalid"),
    AUTOMATION_BLOCKED("automation_blocked"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoMachineFailureKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
