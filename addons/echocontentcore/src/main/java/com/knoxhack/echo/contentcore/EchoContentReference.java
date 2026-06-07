package com.knoxhack.echo.contentcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoContentReference(
        String referenceId,
        EchoContentId fromContent,
        EchoContentKind fromKind,
        EchoContentId targetContent,
        EchoContentKind targetKind,
        EchoModuleId declaringModule,
        EchoContentAvailability availability,
        EchoContentReferenceKind referenceKind,
        boolean required,
        EchoContentGate gate,
        EchoContentSource source,
        List<EchoDiagnostic> diagnostics,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoContentReference {
        referenceId = ContentContractGuards.requireText(referenceId, "content reference id");
        Objects.requireNonNull(fromContent, "fromContent");
        fromKind = fromKind == null ? EchoContentKind.UNKNOWN : fromKind;
        Objects.requireNonNull(targetContent, "targetContent");
        targetKind = targetKind == null ? EchoContentKind.UNKNOWN : targetKind;
        Objects.requireNonNull(declaringModule, "declaringModule");
        availability = availability == null ? EchoContentAvailability.UNKNOWN : availability;
        referenceKind = referenceKind == null ? EchoContentReferenceKind.UNKNOWN : referenceKind;
        required = required || referenceKind.blockingWhenUnavailable();
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = ContentContractGuards.immutableList(diagnostics);
        playerSummary = ContentContractGuards.optionalText(playerSummary);
        developerDetails = ContentContractGuards.optionalText(developerDetails);
        attributes = ContentContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return (required && !availability.available())
                || availability.blocking()
                || gate.blocksWhenMissing()
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
