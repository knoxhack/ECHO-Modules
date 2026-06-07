package com.knoxhack.echorecovery.api;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Objects;

public record EchoRecoveryAction(
        String id,
        EchoRecoveryActionKind kind,
        EchoRecoveryRisk risk,
        boolean requiresConfirmation,
        boolean destructive,
        String playerSummary,
        String developerDetails,
        List<EchoModuleId> affectedModules,
        List<EchoFeatureId> affectedFeatures,
        List<EchoDiagnostic> relatedDiagnostics,
        List<String> relatedFiles,
        List<String> safeCommandReferences
) {
    public EchoRecoveryAction {
        id = RecoveryContractGuards.requireText(id, "recovery action id");
        kind = Objects.requireNonNull(kind, "kind");
        risk = risk == null ? EchoRecoveryRisk.LOW : risk;
        destructive = destructive || risk == EchoRecoveryRisk.DESTRUCTIVE;
        requiresConfirmation = requiresConfirmation || destructive || risk.requiresConfirmation();
        playerSummary = RecoveryContractGuards.requireText(playerSummary, "player summary");
        developerDetails = RecoveryContractGuards.optionalText(developerDetails);
        affectedModules = RecoveryContractGuards.immutableList(affectedModules);
        affectedFeatures = RecoveryContractGuards.immutableList(affectedFeatures);
        relatedDiagnostics = RecoveryContractGuards.immutableList(relatedDiagnostics);
        relatedFiles = RecoveryContractGuards.immutableList(relatedFiles);
        safeCommandReferences = RecoveryContractGuards.immutableList(safeCommandReferences);
    }

    public static EchoRecoveryAction of(String id, EchoRecoveryActionKind kind, EchoRecoveryRisk risk, String playerSummary) {
        return new EchoRecoveryAction(id, kind, risk, false, risk == EchoRecoveryRisk.DESTRUCTIVE, playerSummary, "", List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
