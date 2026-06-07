package com.knoxhack.echo.powercore;

public enum EchoPowerNodeKind {
    GENERATOR("generator"),
    STORAGE("storage"),
    CONSUMER("consumer"),
    TRANSFER("transfer"),
    SWITCH("switch"),
    BREAKER("breaker"),
    METER("meter"),
    SUBSTATION("substation"),
    RELAY("relay"),
    INTERFERENCE_SOURCE("interference_source"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoPowerNodeKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
