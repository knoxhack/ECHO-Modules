package com.knoxhack.echo.biomecore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.List;
import java.util.Map;

public record EchoBiomeProfileRefs(
        List<EchoContentReference> weatherProfiles,
        List<EchoContentReference> spawnProfiles,
        List<EchoContentReference> musicProfiles,
        List<EchoContentReference> fogProfiles,
        List<EchoContentReference> skyProfiles,
        List<EchoContentReference> holomapBiomeLayers,
        List<EchoFeatureId> optionalFeatures,
        Map<String, String> attributes
) {
    public EchoBiomeProfileRefs {
        weatherProfiles = BiomeContractGuards.immutableList(weatherProfiles);
        spawnProfiles = BiomeContractGuards.immutableList(spawnProfiles);
        musicProfiles = BiomeContractGuards.immutableList(musicProfiles);
        fogProfiles = BiomeContractGuards.immutableList(fogProfiles);
        skyProfiles = BiomeContractGuards.immutableList(skyProfiles);
        holomapBiomeLayers = BiomeContractGuards.immutableList(holomapBiomeLayers);
        optionalFeatures = BiomeContractGuards.immutableList(optionalFeatures);
        attributes = BiomeContractGuards.immutableMap(attributes);
    }
}
