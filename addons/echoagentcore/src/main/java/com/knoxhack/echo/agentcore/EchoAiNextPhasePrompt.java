package com.knoxhack.echo.agentcore;

import java.util.List;

public record EchoAiNextPhasePrompt(
        String id,
        String targetPhase,
        String title,
        String prompt,
        List<EchoAiAcceptanceCriterion> acceptanceCriteria,
        List<String> contextSummary,
        List<String> requiredRules,
        String sourceRunReportId
) {
    public EchoAiNextPhasePrompt {
        id = AgentContractGuards.requireText(id, "next phase prompt id");
        targetPhase = AgentContractGuards.requireText(targetPhase, "target phase");
        title = AgentContractGuards.requireText(title, "next phase prompt title");
        prompt = AgentContractGuards.requireText(prompt, "next phase prompt");
        acceptanceCriteria = AgentContractGuards.immutableList(acceptanceCriteria);
        contextSummary = AgentContractGuards.immutableList(contextSummary);
        requiredRules = AgentContractGuards.immutableList(requiredRules);
        sourceRunReportId = AgentContractGuards.optionalText(sourceRunReportId);
    }
}
