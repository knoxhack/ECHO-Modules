package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoFeatureRequirement;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoPackVariant(
        EchoPackVariantId id,
        String name,
        String summary,
        boolean defaultVariant,
        Set<EchoFeatureRequirement> requiredFeatures,
        Set<EchoFeatureRequirement> optionalFeatures,
        Map<String, String> configOverrides,
        List<String> notes
) {
    public EchoPackVariant {
        Objects.requireNonNull(id, "id");
        name = PackContractGuards.requireText(name, "pack variant name");
        summary = PackContractGuards.optionalText(summary);
        requiredFeatures = PackContractGuards.immutableSet(requiredFeatures);
        optionalFeatures = PackContractGuards.immutableSet(optionalFeatures);
        configOverrides = PackContractGuards.immutableStringMap(configOverrides);
        notes = PackContractGuards.immutableList(notes);
    }
}
