package com.knoxhack.echo.assetcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoTextureForgeReport(
        String reportId,
        EchoModuleId moduleId,
        String generatedAt,
        EchoNeededAssetList neededAssets,
        EchoTextureForgePromptSheet promptSheet,
        List<EchoMissingAssetDiagnostic> missingIcons,
        List<EchoTextureForgeNamingRule> namingRules,
        List<EchoDiagnostic> diagnostics,
        Map<EchoTextureForgeOutput, String> outputFiles,
        Map<String, String> attributes
) {
    public EchoTextureForgeReport {
        reportId = AssetContractGuards.normalizedId(reportId, "textureforge report id");
        Objects.requireNonNull(moduleId, "moduleId");
        generatedAt = AssetContractGuards.optionalText(generatedAt);
        missingIcons = AssetContractGuards.immutableList(missingIcons);
        namingRules = AssetContractGuards.immutableList(namingRules);
        diagnostics = AssetContractGuards.immutableList(diagnostics);
        outputFiles = AssetContractGuards.immutableMap(outputFiles);
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return missingIcons.stream().anyMatch(EchoMissingAssetDiagnostic::blocking)
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
