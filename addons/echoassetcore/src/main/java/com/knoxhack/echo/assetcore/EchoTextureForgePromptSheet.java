package com.knoxhack.echo.assetcore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoTextureForgePromptSheet(
        String sheetId,
        EchoModuleId moduleId,
        EchoAssetStyleProfile styleProfile,
        String outputFile,
        List<EchoTextureForgePromptSpec> prompts,
        Map<String, String> attributes
) {
    public EchoTextureForgePromptSheet {
        sheetId = AssetContractGuards.normalizedId(sheetId, "textureforge prompt sheet id");
        Objects.requireNonNull(moduleId, "moduleId");
        styleProfile = styleProfile == null ? EchoAssetStyleProfile.cyberglass() : styleProfile;
        outputFile = AssetContractGuards.optionalText(outputFile);
        prompts = AssetContractGuards.immutableList(prompts).stream()
                .sorted(Comparator.comparingInt(EchoTextureForgePromptSpec::priority)
                        .thenComparing(EchoTextureForgePromptSpec::promptId))
                .toList();
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public List<EchoTextureForgePromptSpec> readyPrompts() {
        return prompts.stream().filter(EchoTextureForgePromptSpec::ready).toList();
    }
}
