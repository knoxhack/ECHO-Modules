package com.knoxhack.echo.assetcore;

import java.util.Map;
import java.util.Set;

public record EchoAssetVariant(
        String variantId,
        String label,
        EchoAssetKind kind,
        EchoAssetResolution resolution,
        boolean required,
        Set<String> states,
        Map<String, String> attributes
) {
    public EchoAssetVariant {
        variantId = AssetContractGuards.normalizedId(variantId, "asset variant id");
        label = AssetContractGuards.optionalText(label);
        kind = kind == null ? EchoAssetKind.UNKNOWN : kind;
        states = AssetContractGuards.immutableSet(states);
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public static EchoAssetVariant base(EchoAssetKind kind, EchoAssetResolution resolution) {
        return new EchoAssetVariant("base", "Base", kind, resolution, true, Set.of(), Map.of());
    }
}
