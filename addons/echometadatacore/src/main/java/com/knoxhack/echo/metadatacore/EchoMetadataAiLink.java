package com.knoxhack.echo.metadatacore;

import java.util.Set;

public record EchoMetadataAiLink(
        boolean available,
        String metadataPath,
        Set<EchoMetadataAgentLane> recommendedAgentLanes,
        boolean requiresHumanReview,
        String summary
) {
    public EchoMetadataAiLink {
        metadataPath = MetadataContractGuards.optionalText(metadataPath);
        if (available && metadataPath.isEmpty()) {
            metadataPath = EchoMetadataFileKind.AI_METADATA.defaultPath();
        }
        recommendedAgentLanes = MetadataContractGuards.immutableSet(recommendedAgentLanes);
        summary = MetadataContractGuards.optionalText(summary);
    }
}
