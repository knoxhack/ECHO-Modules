package com.knoxhack.echo.assetcore;

public enum EchoTextureForgeTemplateKind {
    ITEM_TEXTURE("item_texture"),
    BLOCK_TEXTURE("block_texture"),
    MACHINE_BLOCK("machine_block"),
    ENTITY_TEXTURE("entity_texture"),
    ARMOR_TEXTURE("armor_texture"),
    UI_ICON("ui_icon"),
    THEME_UI_TEXTURE("theme_ui_texture"),
    PARTICLE_TEXTURE("particle_texture"),
    RENDER_PROFILE_ASSET("render_profile_asset"),
    SOUND_PROFILE_ICON("sound_profile_icon"),
    TEXTURE_SHEET("texture_sheet"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoTextureForgeTemplateKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
