package com.knoxhack.echo.reportcore;

public enum EchoReportAudience {
    LAUNCHER("launcher"),
    COMMAND_CENTER("command_center"),
    CYBERDEX("cyberdex"),
    SUPPORT_TOOL("support_tool"),
    AI_AGENT("ai_agent"),
    NATIVE_CLI("native_cli"),
    HUMAN("human");

    private final String serializedName;

    EchoReportAudience(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
