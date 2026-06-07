package com.knoxhack.echo.combatcore;

import java.util.Locale;

public record EchoWeaponTraitId(String value) {
    public EchoWeaponTraitId {
        value = CombatContractGuards.requireText(value, "weapon trait id").toLowerCase(Locale.ROOT);
    }

    public static EchoWeaponTraitId of(String value) {
        return new EchoWeaponTraitId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
