package com.knoxhack.echo.codexcore;

public enum EchoCodexCategory {
    LORE("lore"),
    ENEMIES("enemies"),
    RELICS("relics"),
    FACTIONS("factions"),
    LOGS("logs"),
    SYSTEMS("systems"),
    LOCATIONS("locations"),
    TERMINAL("terminal"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoCodexCategory(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
