package com.knoxhack.echo.assetcore;

public record EchoAssetId(String value) {
    public EchoAssetId {
        value = AssetContractGuards.normalizedId(value, "asset id");
    }

    public static EchoAssetId of(String value) {
        return new EchoAssetId(value);
    }

    public static EchoAssetId of(String namespace, String path) {
        return new EchoAssetId(
                AssetContractGuards.requireText(namespace, "asset id namespace")
                        + ":"
                        + AssetContractGuards.requireText(path, "asset id path")
        );
    }

    public String namespace() {
        int split = value.indexOf(':');
        return split < 0 ? "" : value.substring(0, split);
    }

    public String path() {
        int split = value.indexOf(':');
        return split < 0 ? value : value.substring(split + 1);
    }

    public boolean namespaced() {
        return value.indexOf(':') > 0 && value.indexOf(':') < value.length() - 1;
    }

    @Override
    public String toString() {
        return value;
    }
}
