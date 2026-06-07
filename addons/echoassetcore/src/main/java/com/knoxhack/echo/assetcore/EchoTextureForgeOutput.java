package com.knoxhack.echo.assetcore;

public enum EchoTextureForgeOutput {
    NEEDED_ASSETS_JSON("needed-assets.json"),
    TEXTURE_PROMPTS_MD("texture-prompts.md"),
    ASSET_STYLE_GUIDE_MD("asset-style-guide.md"),
    MISSING_ICONS_MD("missing-icons.md"),
    TEXTUREFORGE_REPORT_JSON("textureforge-report.json");

    private final String fileName;

    EchoTextureForgeOutput(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
