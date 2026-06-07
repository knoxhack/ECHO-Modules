package com.knoxhack.echo.encountercore;

public enum EchoEncounterType {
    RANDOM("random"),
    SCRIPTED("scripted"),
    POI_COMBAT("poi_combat"),
    FACTION_PATROL("faction_patrol"),
    AMBUSH("ambush"),
    RESCUE_EVENT("rescue_event"),
    BOSS_GATE("boss_gate"),
    SUPPLY_DROP("supply_drop"),
    CONVOY("convoy"),
    ANOMALY("anomaly"),
    TUTORIAL("tutorial"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoEncounterType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
