package com.knoxhack.echo.lorecore;

public enum EchoLoreDelivery {
    TEXT("text"),
    VOICE("voice"),
    AUDIO_LOG("audio_log"),
    ENVIRONMENTAL("environmental"),
    BLACKBOX("blackbox"),
    TERMINAL_ARCHIVE("terminal_archive"),
    CODEX_ENTRY("codex_entry"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoLoreDelivery(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
