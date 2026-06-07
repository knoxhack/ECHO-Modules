package com.knoxhack.echorecovery.api;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;

public record EchoRecoveryPlan(
        String id,
        String title,
        EchoRecoveryMode mode,
        EchoRecoveryContext context,
        EchoSafeModeProfile safeModeProfile,
        List<EchoRecoveryAction> actions,
        List<EchoRecoveryRecommendation> recommendations,
        List<EchoDiagnostic> diagnostics,
        boolean automaticExecutionAllowed,
        String playerSummary,
        String developerDetails
) {
    public EchoRecoveryPlan {
        id = RecoveryContractGuards.requireText(id, "recovery plan id");
        title = RecoveryContractGuards.requireText(title, "recovery plan title");
        mode = mode == null ? EchoRecoveryMode.RECOVERY_MODE : mode;
        context = context == null ? EchoRecoveryContext.empty(id + ".context") : context;
        safeModeProfile = safeModeProfile == null ? EchoSafeModeProfile.standard() : safeModeProfile;
        actions = RecoveryContractGuards.immutableList(actions);
        recommendations = RecoveryContractGuards.immutableList(recommendations);
        diagnostics = RecoveryContractGuards.immutableList(diagnostics);
        automaticExecutionAllowed = automaticExecutionAllowed
                && actions.stream().noneMatch(EchoRecoveryAction::requiresConfirmation)
                && actions.stream().noneMatch(EchoRecoveryAction::destructive);
        playerSummary = RecoveryContractGuards.optionalText(playerSummary);
        developerDetails = RecoveryContractGuards.optionalText(developerDetails);
    }

    public boolean requiresConfirmation() {
        return actions.stream().anyMatch(EchoRecoveryAction::requiresConfirmation)
                || recommendations.stream().anyMatch(EchoRecoveryRecommendation::requiresConfirmation);
    }
}
