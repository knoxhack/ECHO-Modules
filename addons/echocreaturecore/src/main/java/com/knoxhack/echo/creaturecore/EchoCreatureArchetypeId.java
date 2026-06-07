package com.knoxhack.echo.creaturecore;

import java.util.Locale;

public record EchoCreatureArchetypeId(String value) {
    public EchoCreatureArchetypeId {
        value = CreatureContractGuards.requireText(value, "creature archetype id").toLowerCase(Locale.ROOT);
    }

    public static EchoCreatureArchetypeId of(String value) {
        return new EchoCreatureArchetypeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
