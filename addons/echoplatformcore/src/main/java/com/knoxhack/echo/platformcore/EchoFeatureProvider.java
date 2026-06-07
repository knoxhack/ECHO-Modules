package com.knoxhack.echo.platformcore;

import java.util.Objects;
import java.util.Set;

public record EchoFeatureProvider(
        EchoModuleId moduleId,
        Set<EchoFeatureDescriptor> providedFeatures
) {
    public EchoFeatureProvider {
        Objects.requireNonNull(moduleId, "moduleId");
        providedFeatures = EchoContractGuards.immutableSet(providedFeatures);
    }
}
