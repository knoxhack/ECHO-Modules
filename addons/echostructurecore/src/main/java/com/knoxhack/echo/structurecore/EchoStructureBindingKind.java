package com.knoxhack.echo.structurecore;

public enum EchoStructureBindingKind {
    FACTION_OWNER("faction_owner"),
    MISSION_LINK("mission_link"),
    HOLOMAP_DISCOVERY("holomap_discovery"),
    LENS_SCAN("lens_scan"),
    LOOT_PROFILE("loot_profile"),
    REGION("region"),
    POI("poi"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoStructureBindingKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
