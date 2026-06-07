package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoGameModeId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.platformcore.EchoTrustLevel;
import com.knoxhack.echo.schemacore.EchoSchemaDescriptor;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EchoPackProfile(
        EchoSchemaDescriptor schema,
        EchoPackId id,
        String name,
        String publisher,
        EchoPackType type,
        EchoGameModeId gameMode,
        EchoModuleId rootModule,
        String minecraftVersion,
        String loaderVersion,
        String worldProfile,
        String startProfile,
        String theme,
        EchoPackChannelId releaseChannel,
        boolean strictOfficialOnly,
        List<EchoPackModuleRequirement> requiredModules,
        List<EchoPackModuleRequirement> optionalModules,
        List<EchoPackFeatureRequirement> requiredFeatures,
        List<EchoPackFeatureRequirement> optionalFeatures,
        Set<EchoTrustLevel> allowedTrustLevels,
        List<EchoPackVariant> variants,
        List<EchoPackChannel> channels,
        EchoSaveCompatibilityResult saveCompatibility,
        List<EchoPackCompositionRule> compositionRules,
        List<EchoPerformanceProfile> performanceProfiles,
        EchoServerPackProfile serverProfile,
        EchoClientPackProfile clientProfile,
        EchoPackReadiness readiness
) {
    public EchoPackProfile {
        Objects.requireNonNull(id, "id");
        name = PackContractGuards.requireText(name, "pack profile name");
        publisher = PackContractGuards.optionalText(publisher);
        type = type == null ? EchoPackType.CUSTOM_PACK : type;
        gameMode = gameMode == null ? EchoGameModeId.of("custom") : gameMode;
        Objects.requireNonNull(rootModule, "rootModule");
        minecraftVersion = PackContractGuards.requireText(minecraftVersion, "minecraft version");
        loaderVersion = PackContractGuards.requireText(loaderVersion, "loader version");
        worldProfile = PackContractGuards.optionalText(worldProfile);
        startProfile = PackContractGuards.optionalText(startProfile);
        theme = PackContractGuards.optionalText(theme);
        releaseChannel = releaseChannel == null ? EchoPackConstants.CHANNEL_STABLE.id() : releaseChannel;
        requiredModules = PackContractGuards.immutableList(requiredModules);
        optionalModules = PackContractGuards.immutableList(optionalModules);
        requiredFeatures = PackContractGuards.immutableList(requiredFeatures);
        optionalFeatures = PackContractGuards.immutableList(optionalFeatures);
        allowedTrustLevels = PackContractGuards.immutableSet(allowedTrustLevels);
        variants = PackContractGuards.immutableList(variants);
        channels = PackContractGuards.immutableList(channels);
        compositionRules = PackContractGuards.immutableList(compositionRules);
        performanceProfiles = PackContractGuards.immutableList(performanceProfiles);
    }
}
