package com.knoxhack.echo.logisticscore;

public record EchoLogisticsRouteId(String value) {
    public EchoLogisticsRouteId {
        value = LogisticsContractGuards.normalizedId(value, "logistics route id");
    }

    public static EchoLogisticsRouteId of(String value) {
        return new EchoLogisticsRouteId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
