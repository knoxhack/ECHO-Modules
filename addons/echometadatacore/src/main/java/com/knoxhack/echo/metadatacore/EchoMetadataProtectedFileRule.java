package com.knoxhack.echo.metadatacore;

public record EchoMetadataProtectedFileRule(
        String pathPattern,
        String reason,
        boolean requiresHumanReview
) {
    public EchoMetadataProtectedFileRule {
        pathPattern = MetadataContractGuards.requireText(pathPattern, "protected file pattern");
        reason = MetadataContractGuards.optionalText(reason);
    }
}
