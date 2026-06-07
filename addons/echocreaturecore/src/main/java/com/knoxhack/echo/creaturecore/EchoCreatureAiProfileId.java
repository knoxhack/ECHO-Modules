package com.knoxhack.echo.creaturecore;

import java.util.Locale;

public record EchoCreatureAiProfileId(String value) {
    public EchoCreatureAiProfileId {
        value = CreatureContractGuards.requireText(value, "creature ai profile id").toLowerCase(Locale.ROOT);
    }

    public static EchoCreatureAiProfileId of(String value) {
        return new EchoCreatureAiProfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
