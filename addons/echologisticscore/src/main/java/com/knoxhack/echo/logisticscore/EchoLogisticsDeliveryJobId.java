package com.knoxhack.echo.logisticscore;

public record EchoLogisticsDeliveryJobId(String value) {
    public EchoLogisticsDeliveryJobId {
        value = LogisticsContractGuards.normalizedId(value, "logistics delivery job id");
    }

    public static EchoLogisticsDeliveryJobId of(String value) {
        return new EchoLogisticsDeliveryJobId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
