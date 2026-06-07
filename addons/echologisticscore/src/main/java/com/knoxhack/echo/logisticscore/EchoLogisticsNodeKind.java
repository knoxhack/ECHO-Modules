package com.knoxhack.echo.logisticscore;

public enum EchoLogisticsNodeKind {
    DEPOT("depot"),
    STORAGE("storage"),
    ROUTER("router"),
    IMPORTER("importer"),
    EXPORTER("exporter"),
    REQUESTER("requester"),
    FACTORY_BRIDGE("factory_bridge"),
    CONVOY_DOCK("convoy_dock"),
    SIGNAL_RELAY("signal_relay"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoLogisticsNodeKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
