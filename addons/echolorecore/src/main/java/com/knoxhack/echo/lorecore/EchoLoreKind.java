package com.knoxhack.echo.lorecore;

public enum EchoLoreKind {
    FRAGMENT("fragment"),
    AUDIO_LOG("audio_log"),
    BLACKBOX_ENTRY("blackbox_entry"),
    ENVIRONMENTAL_STORY("environmental_story"),
    TERMINAL_LOG("terminal_log"),
    RELIC_NOTE("relic_note"),
    FACTION_RECORD("faction_record"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoLoreKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
