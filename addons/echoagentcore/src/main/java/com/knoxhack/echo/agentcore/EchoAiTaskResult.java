package com.knoxhack.echo.agentcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Objects;

public record EchoAiTaskResult(
        EchoAiTaskId taskId,
        EchoAiTaskStatus status,
        String summary,
        List<EchoAiAcceptanceCriterion> satisfiedCriteria,
        List<EchoAiAcceptanceCriterion> failedCriteria,
        List<EchoDiagnostic> diagnostics,
        List<String> safeCommandReferences,
        List<String> generatedArtifacts,
        boolean requiresHumanReview
) {
    public EchoAiTaskResult {
        taskId = Objects.requireNonNull(taskId, "taskId");
        status = status == null ? EchoAiTaskStatus.NEEDS_REVIEW : status;
        summary = AgentContractGuards.optionalText(summary);
        satisfiedCriteria = AgentContractGuards.immutableList(satisfiedCriteria);
        failedCriteria = AgentContractGuards.immutableList(failedCriteria);
        diagnostics = AgentContractGuards.immutableList(diagnostics);
        safeCommandReferences = AgentContractGuards.immutableList(safeCommandReferences);
        generatedArtifacts = AgentContractGuards.immutableList(generatedArtifacts);
        requiresHumanReview = requiresHumanReview || status == EchoAiTaskStatus.NEEDS_REVIEW || !failedCriteria.isEmpty();
    }
}
