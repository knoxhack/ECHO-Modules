package com.knoxhack.echo.healthcore;

import java.util.Locale;

public record EchoHealthMetricId(String value) {
    public EchoHealthMetricId {
        value = HealthContractGuards.requireText(value, "health metric id").toLowerCase(Locale.ROOT);
    }

    public static EchoHealthMetricId of(String value) {
        return new EchoHealthMetricId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
