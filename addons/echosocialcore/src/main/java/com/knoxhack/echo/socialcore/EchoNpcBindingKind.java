package com.knoxhack.echo.socialcore;

public enum EchoNpcBindingKind {
    FACTION("faction"),
    DIALOGUE("dialogue"),
    TRADER("trader"),
    MISSION_GIVER("mission_giver"),
    CUSTOM_SCREEN("custom_screen"),
    HOLOMAP_MARKER("holomap_marker"),
    TERMINAL_CONTACT("terminal_contact"),
    LENS_SCAN("lens_scan"),
    ECONOMY_HOOK("economy_hook"),
    CINEMATIC_HOOK("cinematic_hook"),
    AI_PROFILE("ai_profile"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoNpcBindingKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
