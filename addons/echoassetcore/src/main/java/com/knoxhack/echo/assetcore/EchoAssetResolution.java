package com.knoxhack.echo.assetcore;

public record EchoAssetResolution(int width, int height, String label) {
    public static final EchoAssetResolution MINECRAFT_16 = new EchoAssetResolution(16, 16, "minecraft_16");
    public static final EchoAssetResolution MINECRAFT_32 = new EchoAssetResolution(32, 32, "minecraft_32");
    public static final EchoAssetResolution UI_ICON_32 = new EchoAssetResolution(32, 32, "ui_icon_32");
    public static final EchoAssetResolution UI_ICON_64 = new EchoAssetResolution(64, 64, "ui_icon_64");

    public EchoAssetResolution {
        width = AssetContractGuards.positive(width, "asset resolution width");
        height = AssetContractGuards.positive(height, "asset resolution height");
        label = AssetContractGuards.optionalText(label);
    }

    public static EchoAssetResolution of(int width, int height) {
        return new EchoAssetResolution(width, height, width + "x" + height);
    }

    public boolean square() {
        return width == height;
    }
}
