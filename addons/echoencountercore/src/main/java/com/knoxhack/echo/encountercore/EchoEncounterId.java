package com.knoxhack.echo.encountercore;

public record EchoEncounterId(String value) {
    public EchoEncounterId {
        value = EncounterContractGuards.id(value, "encounter id");
    }

    public static EchoEncounterId of(String value) {
        return new EchoEncounterId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
