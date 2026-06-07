package com.knoxhack.echo.assetcore;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoTextureForgePromptSpec(
        String promptId,
        EchoAssetReference asset,
        EchoTextureForgeTemplateKind templateKind,
        EchoAssetStyleProfile styleProfile,
        EchoAssetResolution resolution,
        List<String> hardRequirements,
        List<String> promptTags,
        List<String> avoid,
        String promptText,
        int priority,
        boolean safeForImageGeneration,
        Map<String, String> attributes
) {
    public EchoTextureForgePromptSpec {
        promptId = AssetContractGuards.normalizedId(promptId, "textureforge prompt id");
        Objects.requireNonNull(asset, "asset");
        templateKind = templateKind == null ? EchoTextureForgeTemplateKind.UNKNOWN : templateKind;
        styleProfile = styleProfile == null ? EchoAssetStyleProfile.cyberglass() : styleProfile;
        resolution = resolution == null ? styleProfile.defaultResolution() : resolution;
        hardRequirements = AssetContractGuards.immutableList(hardRequirements);
        promptTags = AssetContractGuards.immutableList(promptTags);
        avoid = AssetContractGuards.immutableList(avoid);
        promptText = AssetContractGuards.optionalText(promptText);
        priority = AssetContractGuards.nonNegative(priority, "textureforge prompt priority");
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public boolean ready() {
        return safeForImageGeneration && asset.path() != null && asset.kind().textureLike();
    }
}
