package com.knoxhack.echo.codexcore;

public enum EchoCodexEntryKind {
    LORE_DATABASE("lore_database"),
    ENEMY_SCAN("enemy_scan"),
    RELIC_ARCHIVE("relic_archive"),
    FACTION_PROFILE("faction_profile"),
    DISCOVERED_LOG("discovered_log"),
    TERMINAL_ARCHIVE("terminal_archive"),
    GUIDE_REFERENCE("guide_reference"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoCodexEntryKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
