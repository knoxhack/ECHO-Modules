package com.knoxhack.echo.platformcore;

public enum EchoTrustLevel {
    OFFICIAL("official"),
    VERIFIED("verified"),
    COMMUNITY("community"),
    LOCAL("local"),
    EXPERIMENTAL("experimental"),
    BLOCKED("blocked");

    private final String serializedName;

    EchoTrustLevel(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
