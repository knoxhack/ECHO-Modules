package com.knoxhack.echo.biomecore;

import com.knoxhack.echo.assetcore.EchoAssetReference;
import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.List;
import java.util.Map;

public record EchoBiomeAmbientProfile(
        EchoContentReference ambienceReference,
        EchoContentReference soundProfileReference,
        EchoContentReference particleProfileReference,
        List<EchoAssetReference> ambientAssets,
        Map<String, String> attributes
) {
    public EchoBiomeAmbientProfile {
        ambientAssets = BiomeContractGuards.immutableList(ambientAssets);
        attributes = BiomeContractGuards.immutableMap(attributes);
    }
}
