package com.knoxhack.echo.powercore;

public record EchoPowerNodeId(String value) {
    public EchoPowerNodeId {
        value = PowerContractGuards.normalizedId(value, "power node id");
    }

    public static EchoPowerNodeId of(String value) {
        return new EchoPowerNodeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
