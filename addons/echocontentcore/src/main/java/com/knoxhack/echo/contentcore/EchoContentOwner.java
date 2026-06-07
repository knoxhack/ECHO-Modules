package com.knoxhack.echo.contentcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoContentOwner(
        EchoContentId contentId,
        EchoContentKind kind,
        EchoModuleId moduleId,
        EchoContentPackRef packRef,
        EchoContentSource source,
        String displayName,
        String summary,
        Set<EchoFeatureId> providedFeatures,
        EchoContentGate gate,
        boolean official,
        Map<String, String> attributes
) {
    public EchoContentOwner {
        Objects.requireNonNull(contentId, "contentId");
        kind = kind == null ? EchoContentKind.UNKNOWN : kind;
        Objects.requireNonNull(moduleId, "moduleId");
        displayName = ContentContractGuards.optionalText(displayName);
        summary = ContentContractGuards.optionalText(summary);
        providedFeatures = ContentContractGuards.immutableSet(providedFeatures);
        gate = gate == null ? EchoContentGate.open() : gate;
        attributes = ContentContractGuards.immutableMap(attributes);
    }

    public boolean gated() {
        return gate.gated();
    }
}
