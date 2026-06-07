package com.knoxhack.echo.logisticscore;

public record EchoLogisticsNetworkId(String value) {
    public EchoLogisticsNetworkId {
        value = LogisticsContractGuards.normalizedId(value, "logistics network id");
    }

    public static EchoLogisticsNetworkId of(String value) {
        return new EchoLogisticsNetworkId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
