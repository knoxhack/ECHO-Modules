package com.knoxhack.echo.assetcore;

public record EchoAssetStyleProfileId(String value) {
    public EchoAssetStyleProfileId {
        value = AssetContractGuards.normalizedId(value, "asset style profile id");
    }

    public static EchoAssetStyleProfileId of(String value) {
        return new EchoAssetStyleProfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
