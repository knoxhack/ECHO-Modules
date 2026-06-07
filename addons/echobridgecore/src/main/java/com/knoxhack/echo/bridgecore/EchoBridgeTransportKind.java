package com.knoxhack.echo.bridgecore;

public enum EchoBridgeTransportKind {
    LOCAL_LOOPBACK("local_loopback"),
    STDIO("stdio"),
    NAMED_PIPE("named_pipe"),
    WEBSOCKET("websocket"),
    IPC("ipc"),
    FILE_WATCH("file_watch"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoBridgeTransportKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
