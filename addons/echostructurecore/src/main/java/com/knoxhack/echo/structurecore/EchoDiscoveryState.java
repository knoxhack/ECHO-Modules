package com.knoxhack.echo.structurecore;

public enum EchoDiscoveryState {
    HIDDEN("hidden"),
    HINTED("hinted"),
    DISCOVERED("discovered"),
    SCANNED("scanned"),
    CLEARED("cleared"),
    LOCKED("locked"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoDiscoveryState(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
