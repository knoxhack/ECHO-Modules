package com.knoxhack.echo.cinematiccore;

public record EchoCinematicSequenceId(String value) {
    public EchoCinematicSequenceId {
        value = CinematicContractGuards.normalizedId(value, "cinematic sequence id");
    }

    public static EchoCinematicSequenceId of(String value) {
        return new EchoCinematicSequenceId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
