package com.knoxhack.echo.platformcore;

import java.util.Objects;
import java.util.Set;

public record EchoFeatureDescriptor(
        EchoFeatureId id,
        String name,
        String summary,
        EchoApiStability apiStability,
        Set<EchoRuntimeSide> sides,
        Set<EchoCapabilityId> requiredCapabilities,
        EchoDeprecationInfo deprecation
) {
    public EchoFeatureDescriptor {
        Objects.requireNonNull(id, "id");
        name = EchoContractGuards.requireText(name, "feature name");
        summary = EchoContractGuards.optionalText(summary);
        apiStability = apiStability == null ? EchoApiStability.EXPERIMENTAL : apiStability;
        sides = EchoContractGuards.immutableSet(sides);
        requiredCapabilities = EchoContractGuards.immutableSet(requiredCapabilities);
        deprecation = deprecation == null ? EchoDeprecationInfo.notDeprecated() : deprecation;
    }
}
