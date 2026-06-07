package com.knoxhack.echo.hudcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoMissionTrackerEntry(
        String entryId,
        String titleTranslationKey,
        EchoContentReference missionReference,
        EchoContentReference objectiveReference,
        String progressTranslationKey,
        int sortOrder,
        EchoContentGate visibilityGate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoMissionTrackerEntry {
        entryId = HudContractGuards.id(entryId, "mission tracker entry id");
        titleTranslationKey = HudContractGuards.requireText(titleTranslationKey, "mission tracker title translation key");
        progressTranslationKey = HudContractGuards.optionalText(progressTranslationKey);
        sortOrder = HudContractGuards.nonNegative(sortOrder, "mission tracker sort order");
        visibilityGate = visibilityGate == null ? EchoContentGate.open() : visibilityGate;
        diagnostics = HudContractGuards.immutableList(diagnostics);
        attributes = HudContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return visibilityGate.blocksWhenMissing() || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
