package com.knoxhack.echo.metadatacore;

public record EchoMetadataReplacement(
        String deprecatedId,
        String replacementId,
        String reason
) {
    public EchoMetadataReplacement {
        deprecatedId = MetadataContractGuards.requireText(deprecatedId, "deprecated id");
        replacementId = MetadataContractGuards.requireText(replacementId, "replacement id");
        reason = MetadataContractGuards.optionalText(reason);
    }
}
