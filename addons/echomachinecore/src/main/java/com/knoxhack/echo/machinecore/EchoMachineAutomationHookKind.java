package com.knoxhack.echo.machinecore;

public enum EchoMachineAutomationHookKind {
    ITEM_INPUT("item_input"),
    ITEM_OUTPUT("item_output"),
    FLUID_INPUT("fluid_input"),
    FLUID_OUTPUT("fluid_output"),
    SIGNAL_INPUT("signal_input"),
    SIGNAL_OUTPUT("signal_output"),
    POWER_INPUT("power_input"),
    MAINTENANCE("maintenance"),
    REMOTE_STATUS("remote_status"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoMachineAutomationHookKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
