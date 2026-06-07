package com.knoxhack.echo.combatcore;

import java.util.Locale;

public record EchoArmorProfileId(String value) {
    public EchoArmorProfileId {
        value = CombatContractGuards.requireText(value, "armor profile id").toLowerCase(Locale.ROOT);
    }

    public static EchoArmorProfileId of(String value) {
        return new EchoArmorProfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
