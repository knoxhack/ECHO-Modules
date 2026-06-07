package com.knoxhack.echo.socialcore;

public enum EchoAllianceState {
    ALLIED("allied"),
    COOPERATIVE("cooperative"),
    NEUTRAL("neutral"),
    TENSE("tense"),
    RIVAL("rival"),
    HOSTILE("hostile"),
    WAR("war"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoAllianceState(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean conflict() {
        return this == RIVAL || this == HOSTILE || this == WAR;
    }
}
