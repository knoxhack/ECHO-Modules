package com.knoxhack.echo.hudcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoHazardMeter(
        String meterId,
        String labelTranslationKey,
        EchoContentReference statusReference,
        EchoContentReference iconReference,
        double minimumValue,
        double maximumValue,
        double warningThreshold,
        double criticalThreshold,
        String unitTranslationKey,
        EchoContentGate visibilityGate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoHazardMeter {
        meterId = HudContractGuards.id(meterId, "hazard meter id");
        labelTranslationKey = HudContractGuards.requireText(labelTranslationKey, "hazard meter label translation key");
        minimumValue = HudContractGuards.nonNegative(minimumValue, "hazard meter minimum value");
        maximumValue = HudContractGuards.nonNegative(maximumValue, "hazard meter maximum value");
        warningThreshold = HudContractGuards.nonNegative(warningThreshold, "hazard meter warning threshold");
        criticalThreshold = HudContractGuards.nonNegative(criticalThreshold, "hazard meter critical threshold");
        unitTranslationKey = HudContractGuards.optionalText(unitTranslationKey);
        visibilityGate = visibilityGate == null ? EchoContentGate.open() : visibilityGate;
        diagnostics = HudContractGuards.immutableList(diagnostics);
        attributes = HudContractGuards.immutableMap(attributes);
    }

    public boolean thresholdOrderValid() {
        return minimumValue <= warningThreshold && warningThreshold <= criticalThreshold && criticalThreshold <= maximumValue;
    }
}
