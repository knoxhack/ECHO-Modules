package com.knoxhack.echo.platformcore;

import java.util.Objects;

public record EchoCapability(
        EchoCapabilityId id,
        String description,
        EchoRuntimeSide side,
        EchoApiStability apiStability
) {
    public EchoCapability {
        Objects.requireNonNull(id, "id");
        description = EchoContractGuards.optionalText(description);
        side = side == null ? EchoRuntimeSide.COMMON : side;
        apiStability = apiStability == null ? EchoApiStability.EXPERIMENTAL : apiStability;
    }
}
