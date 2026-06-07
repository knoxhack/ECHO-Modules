package com.knoxhack.echo.assetcore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;
import java.util.Map;

public record EchoAssetValidationRequest(
        EchoModuleId moduleId,
        List<EchoAssetReference> references,
        List<EchoAssetStyleProfile> styleProfiles,
        boolean allowMissingOptionalAssets,
        boolean strictNaming,
        Map<String, String> attributes
) {
    public EchoAssetValidationRequest {
        references = AssetContractGuards.immutableList(references);
        styleProfiles = AssetContractGuards.immutableList(styleProfiles);
        attributes = AssetContractGuards.immutableMap(attributes);
    }
}
