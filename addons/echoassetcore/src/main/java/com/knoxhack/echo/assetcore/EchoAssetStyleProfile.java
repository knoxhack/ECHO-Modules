package com.knoxhack.echo.assetcore;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoAssetStyleProfile(
        EchoAssetStyleProfileId id,
        String displayName,
        String summary,
        String styleFamily,
        List<String> paletteHints,
        List<String> materialHints,
        String lightingRules,
        String shapeLanguage,
        List<String> mustHave,
        List<String> avoid,
        EchoAssetResolution defaultResolution,
        Map<String, String> attributes
) {
    public EchoAssetStyleProfile {
        Objects.requireNonNull(id, "id");
        displayName = AssetContractGuards.optionalText(displayName);
        summary = AssetContractGuards.optionalText(summary);
        styleFamily = AssetContractGuards.optionalText(styleFamily);
        paletteHints = AssetContractGuards.immutableList(paletteHints);
        materialHints = AssetContractGuards.immutableList(materialHints);
        lightingRules = AssetContractGuards.optionalText(lightingRules);
        shapeLanguage = AssetContractGuards.optionalText(shapeLanguage);
        mustHave = AssetContractGuards.immutableList(mustHave);
        avoid = AssetContractGuards.immutableList(avoid);
        defaultResolution = defaultResolution == null ? EchoAssetResolution.MINECRAFT_32 : defaultResolution;
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public static EchoAssetStyleProfile cyberglass() {
        return new EchoAssetStyleProfile(
                EchoAssetStyleProfileId.of("echo:cyberglass"),
                "ECHO Cyberglass",
                "Dense high-contrast ECHO UI and technical asset direction.",
                "cyberglass",
                List.of("deep graphite", "cyan accents", "warm warning amber", "status green"),
                List.of("brushed dark alloy", "etched glass", "subtle emissive seams"),
                "Clear rim light, restrained glow, no bloom-heavy blur.",
                "Angular panels, clipped corners, readable silhouettes.",
                List.of("strong silhouette", "clean pixel edges", "readable at inventory size"),
                List.of("photorealism", "blurred gradients", "text baked into icon", "oversized glow"),
                EchoAssetResolution.MINECRAFT_32,
                Map.of("fallback", "true")
        );
    }
}
