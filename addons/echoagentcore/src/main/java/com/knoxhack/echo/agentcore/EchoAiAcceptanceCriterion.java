package com.knoxhack.echo.agentcore;

import java.util.List;
import java.util.Objects;

public record EchoAiAcceptanceCriterion(
        String id,
        EchoAiAcceptanceCriterionType type,
        String target,
        String description,
        boolean required,
        boolean requiresHumanReview,
        List<String> evidence
) {
    public EchoAiAcceptanceCriterion {
        id = AgentContractGuards.requireText(id, "acceptance criterion id");
        type = Objects.requireNonNull(type, "type");
        target = AgentContractGuards.optionalText(target);
        description = AgentContractGuards.requireText(description, "acceptance criterion description");
        requiresHumanReview = requiresHumanReview || type == EchoAiAcceptanceCriterionType.MANUAL_REVIEW_REQUIRED;
        evidence = AgentContractGuards.immutableList(evidence);
    }

    public static EchoAiAcceptanceCriterion required(String id, EchoAiAcceptanceCriterionType type, String description) {
        return new EchoAiAcceptanceCriterion(id, type, "", description, true, type == EchoAiAcceptanceCriterionType.MANUAL_REVIEW_REQUIRED, List.of());
    }
}
