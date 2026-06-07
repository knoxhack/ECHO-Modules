package com.knoxhack.echo.combatcore;

import java.util.Locale;

public record EchoDamageTypeId(String value) {
    public EchoDamageTypeId {
        value = CombatContractGuards.requireText(value, "damage type id").toLowerCase(Locale.ROOT);
    }

    public static EchoDamageTypeId of(String value) {
        return new EchoDamageTypeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
