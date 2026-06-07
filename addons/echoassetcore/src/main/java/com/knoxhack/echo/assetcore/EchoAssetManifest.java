package com.knoxhack.echo.assetcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.schemacore.EchoSchemaId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoAssetManifest(
        EchoSchemaId schemaId,
        EchoModuleId moduleId,
        String manifestId,
        List<EchoAssetReference> assets,
        List<EchoAssetStyleProfile> styleProfiles,
        List<EchoMissingAssetDiagnostic> missingAssets,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoAssetManifest {
        Objects.requireNonNull(moduleId, "moduleId");
        manifestId = AssetContractGuards.requireText(manifestId, "asset manifest id");
        assets = AssetContractGuards.immutableList(assets);
        styleProfiles = AssetContractGuards.immutableList(styleProfiles);
        missingAssets = AssetContractGuards.immutableList(missingAssets);
        diagnostics = AssetContractGuards.immutableList(diagnostics);
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return missingAssets.stream().anyMatch(EchoMissingAssetDiagnostic::blocking)
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
