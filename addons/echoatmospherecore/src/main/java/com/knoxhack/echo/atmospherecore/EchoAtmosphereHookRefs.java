package com.knoxhack.echo.atmospherecore;

import com.knoxhack.echo.assetcore.EchoAssetReference;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.List;
import java.util.Map;

public record EchoAtmosphereHookRefs(
        EchoContentReference biomeAmbienceReference,
        EchoContentReference renderCoreHookReference,
        EchoContentReference soundCoreHookReference,
        EchoContentReference weatherProfileReference,
        List<EchoFeatureId> optionalFeatures,
        List<EchoAssetReference> atmosphereAssets,
        Map<String, String> attributes
) {
    public EchoAtmosphereHookRefs {
        optionalFeatures = AtmosphereContractGuards.immutableList(optionalFeatures);
        atmosphereAssets = AtmosphereContractGuards.immutableList(atmosphereAssets);
        attributes = AtmosphereContractGuards.immutableMap(attributes);
    }
}
