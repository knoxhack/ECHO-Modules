package com.knoxhack.echo.creaturecore;

public enum EchoCreatureRole {
    AMBIENT("ambient"),
    SCAVENGER("scavenger"),
    PREDATOR("predator"),
    GUARDIAN("guardian"),
    BOSS("boss"),
    MINION("minion"),
    SUPPORT("support"),
    CONSTRUCT("construct"),
    MUTANT("mutant"),
    ANOMALY("anomaly"),
    FACTION_NPC("faction_npc"),
    SUMMON("summon"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoCreatureRole(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
