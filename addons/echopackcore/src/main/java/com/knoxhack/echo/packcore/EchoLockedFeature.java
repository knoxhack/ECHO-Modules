package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Objects;

public record EchoLockedFeature(
        EchoFeatureId featureId,
        String requirement,
        EchoLockfileStatus status,
        String providerModuleId
) {
    public EchoLockedFeature {
        Objects.requireNonNull(featureId, "featureId");
        requirement = PackContractGuards.optionalText(requirement);
        status = status == null ? EchoLockfileStatus.LOCKED : status;
        providerModuleId = PackContractGuards.optionalText(providerModuleId);
    }
}
