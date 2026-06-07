package com.knoxhack.echo.progressioncore;

import java.util.Map;
import java.util.Objects;

public record EchoUnlockEdge(
        String edgeId,
        EchoUnlockNodeId from,
        EchoUnlockNodeId to,
        EchoUnlockEdgeKind kind,
        boolean required,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoUnlockEdge {
        edgeId = ProgressionContractGuards.requireText(edgeId, "unlock edge id");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        kind = kind == null ? EchoUnlockEdgeKind.PREREQUISITE : kind;
        playerSummary = ProgressionContractGuards.optionalText(playerSummary);
        developerDetails = ProgressionContractGuards.optionalText(developerDetails);
        attributes = ProgressionContractGuards.immutableMap(attributes);
    }

    public boolean blocksWhenMissing() {
        return required || kind == EchoUnlockEdgeKind.PREREQUISITE || kind == EchoUnlockEdgeKind.BLOCKS;
    }
}
