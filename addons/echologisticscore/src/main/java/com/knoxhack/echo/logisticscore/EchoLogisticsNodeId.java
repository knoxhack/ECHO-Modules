package com.knoxhack.echo.logisticscore;

public record EchoLogisticsNodeId(String value) {
    public EchoLogisticsNodeId {
        value = LogisticsContractGuards.normalizedId(value, "logistics node id");
    }

    public static EchoLogisticsNodeId of(String value) {
        return new EchoLogisticsNodeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
