package com.knoxhack.echo.encountercore;

public enum EchoEncounterParticipantRole {
    PLAYER("player"),
    ALLY("ally"),
    ENEMY("enemy"),
    NEUTRAL("neutral"),
    BOSS("boss"),
    ESCORT("escort"),
    RESCUE_TARGET("rescue_target"),
    TRADER("trader"),
    ENVIRONMENT("environment"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoEncounterParticipantRole(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
