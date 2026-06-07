package com.knoxhack.echo.metadatacore;

public record EchoMetadataConflict(
        String target,
        String versionRange,
        String reason,
        String resolution
) {
    public EchoMetadataConflict {
        target = MetadataContractGuards.requireText(target, "conflict target");
        versionRange = MetadataContractGuards.optionalText(versionRange);
        reason = MetadataContractGuards.optionalText(reason);
        resolution = MetadataContractGuards.optionalText(resolution);
    }
}
