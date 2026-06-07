package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.agentcore.EchoAiAgentLane;
import com.knoxhack.echo.agentcore.EchoAiPromptBundle;
import com.knoxhack.echo.agentcore.EchoAiProtectedFileRule;

import java.util.List;
import java.util.Objects;

public record EchoBridgePromptSubmission(
        String submissionId,
        EchoBridgeSessionId sessionId,
        EchoAiPromptBundle promptBundle,
        String promptText,
        EchoAiAgentLane requestedLane,
        String submittedBy,
        boolean requiresHumanReview,
        List<EchoAiProtectedFileRule> protectedFileRules,
        List<EchoBridgeCommand> requestedCommands,
        long submittedAtEpochMillis
) {
    public EchoBridgePromptSubmission {
        submissionId = BridgeContractGuards.requireText(submissionId, "bridge prompt submission id");
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        promptText = BridgeContractGuards.optionalText(promptText);
        if (promptBundle == null && promptText.isBlank()) {
            throw new IllegalArgumentException("bridge prompt submission must include a prompt bundle or prompt text");
        }
        submittedBy = BridgeContractGuards.optionalText(submittedBy);
        protectedFileRules = BridgeContractGuards.immutableList(protectedFileRules);
        requestedCommands = BridgeContractGuards.immutableList(requestedCommands);
        requiresHumanReview = requiresHumanReview
                || (promptBundle != null && promptBundle.requiresHumanReview())
                || requestedCommands.stream().anyMatch(EchoBridgeCommand::requiresConfirmation);
        submittedAtEpochMillis = BridgeContractGuards.nonNegativeLong(submittedAtEpochMillis, "prompt submission timestamp");
    }
}
