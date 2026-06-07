package com.knoxhack.echo.atmospherecore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoAtmosphereProfile(
        EchoAtmosphereProfileId id,
        EchoModuleId ownerModule,
        EchoFogProfile fogProfile,
        EchoSkyTintProfile skyTintProfile,
        EchoAmbientParticleProfile particleProfile,
        EchoStormVisibilityProfile stormVisibilityProfile,
        EchoAtmosphereHookRefs hookRefs,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoAtmosphereProfile {
        Objects.requireNonNull(id, "id");
        diagnostics = AtmosphereContractGuards.immutableList(diagnostics);
        attributes = AtmosphereContractGuards.immutableMap(attributes);
    }

    public boolean degraded() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
