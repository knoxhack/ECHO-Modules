package com.knoxhack.echo.healthcore;

public enum EchoHealthMetricUnit {
    MILLISECONDS("milliseconds"),
    TICKS("ticks"),
    COUNT("count"),
    BYTES("bytes"),
    PERCENT("percent"),
    BOOLEAN("boolean"),
    STATE("state"),
    PER_SECOND("per_second"),
    CUSTOM("custom");

    private final String serializedName;

    EchoHealthMetricUnit(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
