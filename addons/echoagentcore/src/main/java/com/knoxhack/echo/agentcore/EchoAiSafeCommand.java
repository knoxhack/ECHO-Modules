package com.knoxhack.echo.agentcore;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record EchoAiSafeCommand(
        String id,
        String label,
        List<String> command,
        String workingDirectory,
        EchoAiCommandRisk risk,
        boolean requiresConfirmation,
        List<String> allowedArgs,
        Duration timeout,
        EchoAiCommandOutputParser outputParser,
        EchoAiCommandEnvironmentPolicy environmentPolicy,
        String playerSummary,
        String developerDetails
) {
    public EchoAiSafeCommand {
        id = AgentContractGuards.requireText(id, "safe command id");
        label = AgentContractGuards.requireText(label, "safe command label");
        command = AgentContractGuards.immutableList(command);
        if (command.isEmpty()) {
            throw new IllegalArgumentException("safe command must declare at least one command token");
        }
        workingDirectory = AgentContractGuards.optionalText(workingDirectory);
        risk = risk == null ? EchoAiCommandRisk.LOW : risk;
        environmentPolicy = environmentPolicy == null ? EchoAiCommandEnvironmentPolicy.WORKSPACE_ONLY : environmentPolicy;
        requiresConfirmation = requiresConfirmation || risk.requiresConfirmation() || environmentPolicy.requiresConfirmation();
        allowedArgs = AgentContractGuards.immutableList(allowedArgs);
        timeout = AgentContractGuards.positiveDuration(timeout == null ? Duration.ofMinutes(5L) : timeout, "safe command timeout");
        outputParser = Objects.requireNonNullElse(outputParser, EchoAiCommandOutputParser.RAW_TEXT);
        playerSummary = AgentContractGuards.optionalText(playerSummary);
        developerDetails = AgentContractGuards.optionalText(developerDetails);
    }

    public boolean executionBlockedByPolicy() {
        return environmentPolicy == EchoAiCommandEnvironmentPolicy.BLOCKED;
    }
}
