package com.knoxhack.echo.statuscore;

public enum EchoStatusStackingPolicy {
    NONE("none"),
    REFRESH_DURATION("refresh_duration"),
    INCREASE_INTENSITY("increase_intensity"),
    ADD_STACK("add_stack"),
    HIGHEST_WINS("highest_wins"),
    LOWEST_WINS("lowest_wins"),
    REPLACE("replace"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoStatusStackingPolicy(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
