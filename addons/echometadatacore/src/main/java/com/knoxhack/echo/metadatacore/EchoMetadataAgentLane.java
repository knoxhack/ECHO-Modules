package com.knoxhack.echo.metadatacore;

public enum EchoMetadataAgentLane {
    ARCHITECT_AGENT("architect_agent"),
    LOADER_AGENT("loader_agent"),
    RUNTIME_AGENT("runtime_agent"),
    UI_AGENT("ui_agent"),
    ASSET_AGENT("asset_agent"),
    TEXTUREFORGE_AGENT("textureforge_agent"),
    MISSION_AGENT("mission_agent"),
    WORLD_AGENT("world_agent"),
    PACKAGING_AGENT("packaging_agent"),
    DIAGNOSTICS_AGENT("diagnostics_agent"),
    TEST_AGENT("test_agent"),
    RELEASE_AGENT("release_agent");

    private final String serializedName;

    EchoMetadataAgentLane(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
