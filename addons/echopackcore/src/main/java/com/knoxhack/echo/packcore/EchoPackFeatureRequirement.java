package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoFeatureRequirement;

import java.util.Objects;

public record EchoPackFeatureRequirement(
        EchoFeatureRequirement requirement,
        EchoPackVariantId variantId,
        EchoPackChannelId channelId,
        String reason
) {
    public EchoPackFeatureRequirement {
        Objects.requireNonNull(requirement, "requirement");
        reason = PackContractGuards.optionalText(reason);
    }
}
