package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoApiStability;

public record EchoMetadataTransform(
        String id,
        String kind,
        EchoApiStability stability,
        boolean required,
        String summary
) {
    public EchoMetadataTransform {
        id = MetadataContractGuards.requireText(id, "transform id");
        kind = MetadataContractGuards.requireText(kind, "transform kind");
        stability = stability == null ? EchoApiStability.EXPERIMENTAL : stability;
        summary = MetadataContractGuards.optionalText(summary);
    }
}
