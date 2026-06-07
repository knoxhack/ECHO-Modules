package com.knoxhack.echo.powercore;

public record EchoPowerNetworkId(String value) {
    public EchoPowerNetworkId {
        value = PowerContractGuards.normalizedId(value, "power network id");
    }

    public static EchoPowerNetworkId of(String value) {
        return new EchoPowerNetworkId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
