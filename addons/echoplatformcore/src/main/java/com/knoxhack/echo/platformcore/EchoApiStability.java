package com.knoxhack.echo.platformcore;

public enum EchoApiStability {
    STABLE("stable"),
    BETA("beta"),
    EXPERIMENTAL("experimental"),
    INTERNAL("internal"),
    DEPRECATED("deprecated"),
    REMOVED("removed");

    private final String serializedName;

    EchoApiStability(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
