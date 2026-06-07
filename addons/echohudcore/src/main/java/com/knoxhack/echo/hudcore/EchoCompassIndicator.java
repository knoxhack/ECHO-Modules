package com.knoxhack.echo.hudcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoCompassIndicator(
        String indicatorId,
        String labelTranslationKey,
        EchoContentReference targetReference,
        EchoContentReference iconReference,
        boolean distanceVisible,
        double maxDistanceBlocks,
        EchoContentGate visibilityGate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoCompassIndicator {
        indicatorId = HudContractGuards.id(indicatorId, "compass indicator id");
        labelTranslationKey = HudContractGuards.requireText(labelTranslationKey, "compass indicator label translation key");
        maxDistanceBlocks = HudContractGuards.nonNegative(maxDistanceBlocks, "compass indicator max distance");
        visibilityGate = visibilityGate == null ? EchoContentGate.open() : visibilityGate;
        diagnostics = HudContractGuards.immutableList(diagnostics);
        attributes = HudContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return visibilityGate.blocksWhenMissing() || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
