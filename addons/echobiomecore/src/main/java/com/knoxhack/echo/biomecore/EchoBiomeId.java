package com.knoxhack.echo.biomecore;

public record EchoBiomeId(String value) {
    public EchoBiomeId {
        value = BiomeContractGuards.normalizedId(value, "biome id");
    }

    public static EchoBiomeId of(String value) {
        return new EchoBiomeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
