package com.knoxhack.echo.machinecore;

public record EchoMachineId(String value) {
    public EchoMachineId {
        value = MachineContractGuards.normalizedId(value, "machine id");
    }

    public static EchoMachineId of(String value) {
        return new EchoMachineId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
