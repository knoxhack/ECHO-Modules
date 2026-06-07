package com.knoxhack.echo.combatcore;

public enum EchoCombatDamageKind {
    PHYSICAL("physical"),
    ENERGY("energy"),
    THERMAL("thermal"),
    COLD("cold"),
    TOXIC("toxic"),
    RADIATION("radiation"),
    ARCANE("arcane"),
    SIGNAL("signal"),
    NEXUS("nexus"),
    BLEED("bleed"),
    SHOCK("shock"),
    TRUE_DAMAGE("true_damage"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoCombatDamageKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
