package com.knoxhack.echo.difficultycore;

public record EchoDifficultyProfileId(String value) {
    public EchoDifficultyProfileId {
        value = DifficultyContractGuards.id(value, "difficulty profile id");
    }

    public static EchoDifficultyProfileId of(String value) {
        return new EchoDifficultyProfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
