package com.knoxhack.echo.agentcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EchoAiTask(
        EchoAiTaskId id,
        String title,
        String summary,
        EchoAiTaskStatus status,
        EchoAiTaskPriority priority,
        EchoAiAgentLane lane,
        EchoPackId packId,
        Set<EchoModuleId> affectedModules,
        Set<EchoFeatureId> affectedFeatures,
        List<EchoAiAcceptanceCriterion> acceptanceCriteria,
        List<EchoAiSafeCommand> safeCommands,
        List<EchoAiProtectedFileRule> protectedFileRules,
        List<EchoDiagnostic> diagnostics,
        List<String> relatedDocs,
        boolean requiresHumanReview
) {
    public EchoAiTask {
        id = Objects.requireNonNull(id, "id");
        title = AgentContractGuards.requireText(title, "AI task title");
        summary = AgentContractGuards.optionalText(summary);
        status = status == null ? EchoAiTaskStatus.PROPOSED : status;
        priority = priority == null ? EchoAiTaskPriority.NORMAL : priority;
        lane = lane == null ? EchoAiAgentLane.ARCHITECT_AGENT : lane;
        affectedModules = AgentContractGuards.immutableSet(affectedModules);
        affectedFeatures = AgentContractGuards.immutableSet(affectedFeatures);
        acceptanceCriteria = AgentContractGuards.immutableList(acceptanceCriteria);
        safeCommands = AgentContractGuards.immutableList(safeCommands);
        protectedFileRules = AgentContractGuards.immutableList(protectedFileRules);
        diagnostics = AgentContractGuards.immutableList(diagnostics);
        relatedDocs = AgentContractGuards.immutableList(relatedDocs);
        requiresHumanReview = requiresHumanReview
                || acceptanceCriteria.stream().anyMatch(EchoAiAcceptanceCriterion::requiresHumanReview)
                || safeCommands.stream().anyMatch(EchoAiSafeCommand::requiresConfirmation)
                || protectedFileRules.stream().anyMatch(EchoAiProtectedFileRule::requiresHumanReview);
    }

    public boolean hasExecutableModelOnlyCommands() {
        return !safeCommands.isEmpty();
    }
}
