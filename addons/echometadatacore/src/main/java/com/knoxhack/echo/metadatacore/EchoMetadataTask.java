package com.knoxhack.echo.metadatacore;

import java.util.List;
import java.util.Set;

public record EchoMetadataTask(
        String id,
        String label,
        String summary,
        Set<EchoMetadataAgentLane> suggestedLanes,
        List<String> acceptanceHints
) {
    public EchoMetadataTask {
        id = MetadataContractGuards.requireText(id, "task id");
        label = MetadataContractGuards.requireText(label, "task label");
        summary = MetadataContractGuards.optionalText(summary);
        suggestedLanes = MetadataContractGuards.immutableSet(suggestedLanes);
        acceptanceHints = MetadataContractGuards.immutableList(acceptanceHints);
    }
}
