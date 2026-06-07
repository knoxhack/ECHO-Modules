package com.knoxhack.echo.hudcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoHudWidget(
        EchoHudWidgetId id,
        String translationKey,
        EchoHudWidgetKind kind,
        EchoModuleId owningModule,
        EchoHudAnchor anchor,
        int priority,
        EchoHudVisibility visibility,
        EchoContentReference sourceReference,
        EchoContentGate visibilityGate,
        EchoHudScaleRule scaleRule,
        EchoScreenSafeArea safeArea,
        Set<EchoFeatureId> optionalIntegrationFeatures,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoHudWidget {
        Objects.requireNonNull(id, "id");
        translationKey = HudContractGuards.requireText(translationKey, "hud widget translation key");
        kind = kind == null ? EchoHudWidgetKind.UNKNOWN : kind;
        anchor = anchor == null ? EchoHudAnchor.UNKNOWN : anchor;
        priority = HudContractGuards.nonNegative(priority, "hud widget priority");
        visibility = visibility == null ? EchoHudVisibility.UNKNOWN : visibility;
        visibilityGate = visibilityGate == null ? EchoContentGate.open() : visibilityGate;
        optionalIntegrationFeatures = HudContractGuards.immutableSet(optionalIntegrationFeatures);
        diagnostics = HudContractGuards.immutableList(diagnostics);
        attributes = HudContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return visibilityGate.blocksWhenMissing() || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
