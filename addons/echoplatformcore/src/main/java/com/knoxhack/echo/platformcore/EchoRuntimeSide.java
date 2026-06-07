package com.knoxhack.echo.platformcore;

public enum EchoRuntimeSide {
    COMMON("common"),
    CLIENT("client"),
    SERVER("server"),
    DATA("data"),
    DEV("dev"),
    LAUNCHER("launcher"),
    COMMAND_CENTER("command_center"),
    AI_AGENT("ai_agent");

    private final String serializedName;

    EchoRuntimeSide(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
