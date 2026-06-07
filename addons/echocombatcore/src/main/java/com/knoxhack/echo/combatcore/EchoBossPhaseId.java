package com.knoxhack.echo.combatcore;

import java.util.Locale;

public record EchoBossPhaseId(String value) {
    public EchoBossPhaseId {
        value = CombatContractGuards.requireText(value, "boss phase id").toLowerCase(Locale.ROOT);
    }

    public static EchoBossPhaseId of(String value) {
        return new EchoBossPhaseId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
