package com.knoxhack.echo.biomecore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoBiomeProfile(
        EchoBiomeId id,
        EchoModuleId ownerModule,
        Set<EchoBiomeTag> tags,
        List<EchoBiomeHazardOverlay> hazardOverlays,
        EchoBiomeAmbientProfile ambientProfile,
        EchoBiomeProfileRefs references,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoBiomeProfile {
        Objects.requireNonNull(id, "id");
        tags = BiomeContractGuards.immutableSet(tags);
        hazardOverlays = BiomeContractGuards.immutableList(hazardOverlays);
        diagnostics = BiomeContractGuards.immutableList(diagnostics);
        attributes = BiomeContractGuards.immutableMap(attributes);
    }

    public boolean degraded() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
