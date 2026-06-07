package com.knoxhack.echo.powercore;

public enum EchoPowerInstabilityKind {
    OVERLOAD("overload"),
    BROWNOUT("brownout"),
    POWER_LOSS("power_loss"),
    SHORT_CIRCUIT("short_circuit"),
    WEATHER_INTERFERENCE("weather_interference"),
    NEXUS_CORRUPTION("nexus_corruption"),
    SIGNAL_NOISE("signal_noise"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoPowerInstabilityKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
