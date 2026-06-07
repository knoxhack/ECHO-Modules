package com.knoxhack.echo.assetcore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoNeededAssetList(
        EchoModuleId moduleId,
        String outputFile,
        List<EchoAssetReference> requiredAssets,
        List<EchoAssetReference> optionalAssets,
        List<EchoMissingAssetDiagnostic> missingAssets,
        Map<String, String> attributes
) {
    public EchoNeededAssetList {
        Objects.requireNonNull(moduleId, "moduleId");
        outputFile = AssetContractGuards.optionalText(outputFile);
        requiredAssets = AssetContractGuards.immutableList(requiredAssets);
        optionalAssets = AssetContractGuards.immutableList(optionalAssets);
        missingAssets = AssetContractGuards.immutableList(missingAssets);
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public int totalNeeded() {
        return requiredAssets.size() + optionalAssets.size();
    }
}
