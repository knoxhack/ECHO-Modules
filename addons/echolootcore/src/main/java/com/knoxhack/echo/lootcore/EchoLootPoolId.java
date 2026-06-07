package com.knoxhack.echo.lootcore;

import java.util.Locale;

public record EchoLootPoolId(String value) {
    public EchoLootPoolId {
        value = LootContractGuards.requireText(value, "loot pool id").toLowerCase(Locale.ROOT);
    }

    public static EchoLootPoolId of(String value) {
        return new EchoLootPoolId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
