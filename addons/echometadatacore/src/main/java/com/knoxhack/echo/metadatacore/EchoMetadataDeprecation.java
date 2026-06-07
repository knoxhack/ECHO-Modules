package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Objects;

public record EchoMetadataDeprecation(
        EchoFeatureId featureId,
        String sinceVersion,
        String removalVersion,
        String reason,
        String replacement
) {
    public EchoMetadataDeprecation {
        Objects.requireNonNull(featureId, "featureId");
        sinceVersion = MetadataContractGuards.optionalText(sinceVersion);
        removalVersion = MetadataContractGuards.optionalText(removalVersion);
        reason = MetadataContractGuards.optionalText(reason);
        replacement = MetadataContractGuards.optionalText(replacement);
    }
}
