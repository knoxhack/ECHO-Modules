package com.knoxhack.echorecovery.api;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Objects;

public record EchoRecoveryRecommendation(
        String id,
        EchoRecoveryTrigger trigger,
        EchoRecoveryMode mode,
        EchoRecoveryRisk risk,
        String title,
        String playerMessage,
        String developerDetails,
        double confidence,
        List<EchoRecoveryAction> actions,
        List<EchoModuleId> affectedModules,
        List<EchoFeatureId> affectedFeatures,
        List<EchoDiagnostic> relatedDiagnostics
) {
    public EchoRecoveryRecommendation {
        id = RecoveryContractGuards.requireText(id, "recovery recommendation id");
        trigger = Objects.requireNonNull(trigger, "trigger");
        mode = mode == null ? EchoRecoveryMode.RECOVERY_MODE : mode;
        risk = risk == null ? EchoRecoveryRisk.LOW : risk;
        title = RecoveryContractGuards.requireText(title, "recovery recommendation title");
        playerMessage = RecoveryContractGuards.requireText(playerMessage, "player message");
        developerDetails = RecoveryContractGuards.optionalText(developerDetails);
        confidence = RecoveryContractGuards.boundedConfidence(confidence);
        actions = RecoveryContractGuards.immutableList(actions);
        affectedModules = RecoveryContractGuards.immutableList(affectedModules);
        affectedFeatures = RecoveryContractGuards.immutableList(affectedFeatures);
        relatedDiagnostics = RecoveryContractGuards.immutableList(relatedDiagnostics);
    }

    public boolean requiresConfirmation() {
        return risk.requiresConfirmation() || actions.stream().anyMatch(EchoRecoveryAction::requiresConfirmation);
    }
}
