package com.knoxhack.echo.assetcore;

public enum EchoAssetKind {
    TEXTURE("texture"),
    MODEL("model"),
    ICON("icon"),
    SOUND("sound"),
    MUSIC("music"),
    PARTICLE_TEXTURE("particle_texture"),
    ENTITY_TEXTURE("entity_texture"),
    BLOCK_TEXTURE("block_texture"),
    ITEM_TEXTURE("item_texture"),
    UI_TEXTURE("ui_texture"),
    THEME_TEXTURE("theme_texture"),
    TEXTURE_SHEET("texture_sheet"),
    ANIMATION("animation"),
    FONT("font"),
    LANGUAGE("language"),
    EUI_LAYOUT("eui_layout"),
    EUI_STYLE("eui_style"),
    SHADER("shader"),
    RENDER_PROFILE("render_profile"),
    SOUND_PROFILE("sound_profile"),
    ASSET_MANIFEST("asset_manifest"),
    STYLE_PROFILE("style_profile"),
    PROMPT_SHEET("prompt_sheet"),
    MISSING_ASSET_REPORT("missing_asset_report"),
    TEXTUREFORGE_REPORT("textureforge_report"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoAssetKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean textureLike() {
        return this == TEXTURE || this == ICON || this == PARTICLE_TEXTURE || this == ENTITY_TEXTURE
                || this == BLOCK_TEXTURE || this == ITEM_TEXTURE || this == UI_TEXTURE || this == THEME_TEXTURE
                || this == TEXTURE_SHEET;
    }

    public boolean reportLike() {
        return this == MISSING_ASSET_REPORT || this == TEXTUREFORGE_REPORT || this == PROMPT_SHEET;
    }
}
