package com.knoxhack.echo.metadatacore;

public enum EchoMetadataFileKind {
    MODULE_MANIFEST("META-INF/echo.mod.json"),
    AI_METADATA("META-INF/echo.ai.json");

    private final String defaultPath;

    EchoMetadataFileKind(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public String defaultPath() {
        return defaultPath;
    }
}
