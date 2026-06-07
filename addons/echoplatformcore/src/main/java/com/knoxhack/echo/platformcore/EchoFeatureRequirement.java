package com.knoxhack.echo.platformcore;

import java.util.Objects;

public record EchoFeatureRequirement(
        EchoFeatureId featureId,
        boolean required,
        String versionRange,
        String reason
) {
    public EchoFeatureRequirement {
        Objects.requireNonNull(featureId, "featureId");
        versionRange = EchoContractGuards.optionalText(versionRange);
        reason = EchoContractGuards.optionalText(reason);
    }

    public static EchoFeatureRequirement required(EchoFeatureId featureId, String reason) {
        return new EchoFeatureRequirement(featureId, true, "", reason);
    }

    public static EchoFeatureRequirement optional(EchoFeatureId featureId, String reason) {
        return new EchoFeatureRequirement(featureId, false, "", reason);
    }
}
