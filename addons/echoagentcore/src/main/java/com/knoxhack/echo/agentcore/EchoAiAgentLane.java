package com.knoxhack.echo.agentcore;

public enum EchoAiAgentLane {
    ARCHITECT_AGENT("architect_agent"),
    VALIDATION_AGENT("validation_agent"),
    METADATA_AGENT("metadata_agent"),
    PACKOS_AGENT("packos_agent"),
    LAUNCHER_AGENT("launcher_agent"),
    COMMAND_CENTER_AGENT("command_center_agent"),
    CYBERDEX_AGENT("cyberdex_agent"),
    LOADER_AGENT("loader_agent"),
    RUNTIME_AGENT("runtime_agent"),
    UI_AGENT("ui_agent"),
    ASSET_AGENT("asset_agent"),
    TEXTUREFORGE_AGENT("textureforge_agent"),
    MISSION_AGENT("mission_agent"),
    WORLD_AGENT("world_agent"),
    PACKAGING_AGENT("packaging_agent"),
    DIAGNOSTICS_AGENT("diagnostics_agent"),
    DOCS_AGENT("docs_agent"),
    NATIVE_CLI_AGENT("native_cli_agent"),
    TEST_AGENT("test_agent"),
    RELEASE_AGENT("release_agent");

    private final String serializedName;

    EchoAiAgentLane(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
