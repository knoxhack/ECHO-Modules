package com.knoxhack.echo.assetcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoAssetValidationResult(
        boolean valid,
        List<EchoMissingAssetDiagnostic> missingAssets,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoAssetValidationResult {
        missingAssets = AssetContractGuards.immutableList(missingAssets);
        diagnostics = AssetContractGuards.immutableList(diagnostics);
        attributes = AssetContractGuards.immutableMap(attributes);
        valid = valid && missingAssets.stream().noneMatch(EchoMissingAssetDiagnostic::blocking)
                && diagnostics.stream().noneMatch(EchoDiagnostic::blocking);
    }

    public static EchoAssetValidationResult ok() {
        return new EchoAssetValidationResult(true, List.of(), List.of(), Map.of());
    }
}
