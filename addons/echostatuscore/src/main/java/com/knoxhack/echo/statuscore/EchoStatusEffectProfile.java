package com.knoxhack.echo.statuscore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoStatusEffectProfile(
        EchoStatusId id,
        EchoStatusKind kind,
        String displayName,
        EchoModuleId owningModule,
        EchoStatusSeverity defaultSeverity,
        EchoStatusStackingPolicy stackingPolicy,
        EchoContentReference statusEffectReference,
        EchoContentReference iconReference,
        EchoContentReference soundProfileReference,
        EchoContentReference particleProfileReference,
        Set<EchoFeatureId> optionalIntegrationFeatures,
        EchoContentGate gate,
        String playerSummary,
        String developerDetails,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoStatusEffectProfile {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoStatusKind.UNKNOWN : kind;
        displayName = StatusContractGuards.requireText(displayName, "status display name");
        defaultSeverity = defaultSeverity == null ? EchoStatusSeverity.UNKNOWN : defaultSeverity;
        stackingPolicy = stackingPolicy == null ? EchoStatusStackingPolicy.UNKNOWN : stackingPolicy;
        optionalIntegrationFeatures = StatusContractGuards.immutableSet(optionalIntegrationFeatures);
        gate = gate == null ? EchoContentGate.open() : gate;
        playerSummary = StatusContractGuards.optionalText(playerSummary);
        developerDetails = StatusContractGuards.optionalText(developerDetails);
        diagnostics = StatusContractGuards.immutableList(diagnostics);
        attributes = StatusContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
