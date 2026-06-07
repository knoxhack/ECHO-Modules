package com.knoxhack.echo.logisticscore;

public enum EchoLogisticsChannelKind {
    ITEM("item"),
    FLUID("fluid"),
    SIGNAL("signal"),
    POWER_REQUEST("power_request"),
    REMOTE_STATUS("remote_status"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoLogisticsChannelKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
