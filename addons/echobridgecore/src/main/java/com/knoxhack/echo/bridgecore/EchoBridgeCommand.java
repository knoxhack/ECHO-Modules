package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.agentcore.EchoAiCommandEnvironmentPolicy;
import com.knoxhack.echo.agentcore.EchoAiCommandOutputParser;
import com.knoxhack.echo.agentcore.EchoAiCommandRisk;
import com.knoxhack.echo.agentcore.EchoAiSafeCommand;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record EchoBridgeCommand(
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
        boolean localOnly,
        String playerSummary,
        String developerDetails
) {
    public EchoBridgeCommand {
        id = BridgeContractGuards.requireText(id, "bridge command id");
        label = BridgeContractGuards.requireText(label, "bridge command label");
        command = BridgeContractGuards.immutableList(command);
        if (command.isEmpty()) {
            throw new IllegalArgumentException("bridge command must declare at least one command token");
        }
        workingDirectory = BridgeContractGuards.optionalText(workingDirectory);
        risk = risk == null ? EchoAiCommandRisk.LOW : risk;
        environmentPolicy = environmentPolicy == null ? EchoAiCommandEnvironmentPolicy.LOCAL_ONLY : environmentPolicy;
        requiresConfirmation = requiresConfirmation || risk.requiresConfirmation() || environmentPolicy.requiresConfirmation();
        allowedArgs = BridgeContractGuards.immutableList(allowedArgs);
        timeout = BridgeContractGuards.positiveDuration(timeout == null ? Duration.ofMinutes(5L) : timeout, "bridge command timeout");
        outputParser = Objects.requireNonNullElse(outputParser, EchoAiCommandOutputParser.RAW_TEXT);
        localOnly = true;
        playerSummary = BridgeContractGuards.optionalText(playerSummary);
        developerDetails = BridgeContractGuards.optionalText(developerDetails);
    }

    public EchoAiSafeCommand asSafeCommandDescriptor() {
        return new EchoAiSafeCommand(
                id,
                label,
                command,
                workingDirectory,
                risk,
                requiresConfirmation,
                allowedArgs,
                timeout,
                outputParser,
                environmentPolicy,
                playerSummary,
                developerDetails
        );
    }

    public boolean executionBlockedByPolicy() {
        return environmentPolicy == EchoAiCommandEnvironmentPolicy.BLOCKED;
    }
}
