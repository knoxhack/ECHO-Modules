package com.knoxhack.echo.assetcore;

public record EchoAssetPath(String value) {
    public EchoAssetPath {
        value = AssetContractGuards.normalizedId(value, "asset path");
    }

    public static EchoAssetPath of(String value) {
        return new EchoAssetPath(value);
    }

    public boolean png() {
        return value.endsWith(".png");
    }

    public boolean json() {
        return value.endsWith(".json");
    }

    public boolean underAssets() {
        return value.startsWith("assets/");
    }

    @Override
    public String toString() {
        return value;
    }
}
