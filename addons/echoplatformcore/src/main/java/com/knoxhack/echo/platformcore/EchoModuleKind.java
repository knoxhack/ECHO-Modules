package com.knoxhack.echo.platformcore;

public enum EchoModuleKind {
    ADDON("addon"),
    LIBRARY("library"),
    CONTENT_PACK("content_pack"),
    UI_PACK("ui_pack"),
    THEME_PACK("theme_pack"),
    WORLD_PACK("world_pack"),
    QUEST_PACK("quest_pack"),
    RECIPE_PACK("recipe_pack"),
    SOUND_PACK("sound_pack"),
    VISUAL_PACK("visual_pack"),
    SHADER_PACK("shader_pack"),
    GAME_ROOT("game_root"),
    SERVER_PROFILE("server_profile"),
    MODPACK_PROFILE("modpack_profile"),
    DEV_TOOL("dev_tool"),
    AI_TOOL("ai_tool"),
    LAUNCHER_TOOL("launcher_tool"),
    COMMAND_CENTER_TOOL("command_center_tool");

    private final String serializedName;

    EchoModuleKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
