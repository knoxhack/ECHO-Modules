package com.knoxhack.echo.socialcore;

public enum EchoHostilityState {
    FRIENDLY("friendly"),
    NEUTRAL("neutral"),
    WARY("wary"),
    HOSTILE("hostile"),
    KILL_ON_SIGHT("kill_on_sight"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoHostilityState(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean hostile() {
        return this == HOSTILE || this == KILL_ON_SIGHT;
    }
}
