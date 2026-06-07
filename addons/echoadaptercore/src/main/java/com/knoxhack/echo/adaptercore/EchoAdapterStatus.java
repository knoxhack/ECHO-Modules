package com.knoxhack.echo.adaptercore;

public enum EchoAdapterStatus {
    ACTIVE_CURRENT("active_current"),
    ACTIVE_ALPHA("active_alpha"),
    ACTIVE("active"),
    CURRENT("current"),
    PLANNED("planned"),
    EXPERIMENTAL("experimental"),
    DEGRADED("degraded"),
    DISABLED("disabled"),
    INCOMPATIBLE("incompatible"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoAdapterStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
