package com.knoxhack.echo.platformcore;

import java.util.Objects;
import java.util.Set;

public record EchoFeatureConsumer(
        EchoModuleId moduleId,
        Set<EchoFeatureRequirement> consumedFeatures
) {
    public EchoFeatureConsumer {
        Objects.requireNonNull(moduleId, "moduleId");
        consumedFeatures = EchoContractGuards.immutableSet(consumedFeatures);
    }
}
