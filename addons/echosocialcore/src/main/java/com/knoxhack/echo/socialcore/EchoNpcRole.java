package com.knoxhack.echo.socialcore;

public enum EchoNpcRole {
    SURVIVOR("survivor"),
    ENGINEER("engineer"),
    SCAVENGER("scavenger"),
    RELAY_OPERATOR("relay_operator"),
    BLACKBOX_ARCHIVIST("blackbox_archivist"),
    ARCANA_RESEARCHER("arcana_researcher"),
    FACTION_GUARD("faction_guard"),
    CONVOY_TRADER("convoy_trader"),
    WASTELAND_MEDIC("wasteland_medic"),
    TUTORIAL_GUIDE("tutorial_guide"),
    RITUALIST("ritualist"),
    STATION_SURVIVOR("station_survivor"),
    BASE_OPERATOR("base_operator"),
    POWER_TECHNICIAN("power_technician"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoNpcRole(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean serviceProvider() {
        return this == ENGINEER
                || this == RELAY_OPERATOR
                || this == CONVOY_TRADER
                || this == WASTELAND_MEDIC
                || this == BASE_OPERATOR
                || this == POWER_TECHNICIAN;
    }
}
