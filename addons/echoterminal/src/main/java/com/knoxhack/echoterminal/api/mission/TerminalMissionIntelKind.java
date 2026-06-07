package com.knoxhack.echoterminal.api.mission;

public enum TerminalMissionIntelKind {
    ARCHIVE("Archive"),
    ROUTE("Route"),
    DISCOVERY("Discovery"),
    FACTION("Faction"),
    POI("POI");

    private final String displayName;

    TerminalMissionIntelKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
