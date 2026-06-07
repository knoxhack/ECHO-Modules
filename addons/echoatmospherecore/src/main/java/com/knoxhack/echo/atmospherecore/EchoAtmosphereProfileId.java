package com.knoxhack.echo.atmospherecore;

public record EchoAtmosphereProfileId(String value) {
    public EchoAtmosphereProfileId {
        value = AtmosphereContractGuards.normalizedId(value, "atmosphere profile id");
    }

    public static EchoAtmosphereProfileId of(String value) {
        return new EchoAtmosphereProfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
