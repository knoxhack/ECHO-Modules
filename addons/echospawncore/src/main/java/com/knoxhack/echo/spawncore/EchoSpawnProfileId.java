package com.knoxhack.echo.spawncore;

import java.util.Locale;

public record EchoSpawnProfileId(String value) {
    public EchoSpawnProfileId {
        value = SpawnContractGuards.requireText(value, "spawn profile id").toLowerCase(Locale.ROOT);
    }

    public static EchoSpawnProfileId of(String value) {
        return new EchoSpawnProfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
