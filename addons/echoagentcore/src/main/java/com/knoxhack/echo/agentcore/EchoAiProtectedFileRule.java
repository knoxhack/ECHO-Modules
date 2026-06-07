package com.knoxhack.echo.agentcore;

import java.util.Set;

public record EchoAiProtectedFileRule(
        String pathPattern,
        EchoAiProtectionLevel protectionLevel,
        String reason,
        boolean requiresHumanReview,
        Set<EchoAiAgentLane> allowedLanes
) {
    public EchoAiProtectedFileRule {
        pathPattern = AgentContractGuards.requireText(pathPattern, "protected file path pattern");
        protectionLevel = protectionLevel == null ? EchoAiProtectionLevel.REVIEW_REQUIRED : protectionLevel;
        reason = AgentContractGuards.optionalText(reason);
        requiresHumanReview = requiresHumanReview || protectionLevel.requiresHumanReview();
        allowedLanes = AgentContractGuards.immutableSet(allowedLanes);
    }

    public static EchoAiProtectedFileRule blocked(String pathPattern, String reason) {
        return new EchoAiProtectedFileRule(pathPattern, EchoAiProtectionLevel.BLOCKED, reason, true, Set.of());
    }
}
