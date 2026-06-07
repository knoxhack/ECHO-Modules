package com.knoxhack.echo.machinecore;

public enum EchoMachineKind {
    SINGLE_BLOCK("single_block"),
    MULTIBLOCK("multiblock"),
    POWERED_STATION("powered_station"),
    CRAFTING_STATION("crafting_station"),
    REFINERY("refinery"),
    FABRICATOR("fabricator"),
    ASSEMBLER("assembler"),
    REPAIR_BENCH("repair_bench"),
    AUTOMATION_NODE("automation_node"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoMachineKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
