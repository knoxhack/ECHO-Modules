package com.knoxhack.echo.platformcore;

import java.util.Objects;

public record EchoIntegrationPoint(
        EchoModuleId ownerModuleId,
        EchoFeatureId featureId,
        String entrypoint,
        boolean optional,
        EchoRuntimeSide side,
        EchoApiStability apiStability
) {
    public EchoIntegrationPoint {
        Objects.requireNonNull(ownerModuleId, "ownerModuleId");
        Objects.requireNonNull(featureId, "featureId");
        entrypoint = EchoContractGuards.optionalText(entrypoint);
        side = side == null ? EchoRuntimeSide.COMMON : side;
        apiStability = apiStability == null ? EchoApiStability.EXPERIMENTAL : apiStability;
    }
}
