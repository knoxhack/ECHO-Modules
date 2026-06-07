package com.knoxhack.echorecovery.api;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;

public record EchoRecoveryResult(
        String id,
        String planId,
        String actionId,
        EchoRecoveryResultStatus status,
        String playerMessage,
        String developerDetails,
        boolean requiresFollowUp,
        List<EchoDiagnostic> diagnostics,
        List<String> producedFiles
) {
    public EchoRecoveryResult {
        id = RecoveryContractGuards.requireText(id, "recovery result id");
        planId = RecoveryContractGuards.optionalText(planId);
        actionId = RecoveryContractGuards.optionalText(actionId);
        status = status == null ? EchoRecoveryResultStatus.PLANNED : status;
        playerMessage = RecoveryContractGuards.optionalText(playerMessage);
        developerDetails = RecoveryContractGuards.optionalText(developerDetails);
        diagnostics = RecoveryContractGuards.immutableList(diagnostics);
        producedFiles = RecoveryContractGuards.immutableList(producedFiles);
    }

    public static EchoRecoveryResult needsConfirmation(String id, String planId, String actionId, String playerMessage) {
        return new EchoRecoveryResult(id, planId, actionId, EchoRecoveryResultStatus.NEEDS_CONFIRMATION, playerMessage, "", true, List.of(), List.of());
    }
}
