package com.knoxhack.echo.lootcore;

import java.util.Locale;

public record EchoLootProfileId(String value) {
    public EchoLootProfileId {
        value = LootContractGuards.requireText(value, "loot profile id").toLowerCase(Locale.ROOT);
    }

    public static EchoLootProfileId of(String value) {
        return new EchoLootProfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
