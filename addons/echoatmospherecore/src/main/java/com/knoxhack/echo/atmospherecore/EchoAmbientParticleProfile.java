package com.knoxhack.echo.atmospherecore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.List;
import java.util.Map;

public record EchoAmbientParticleProfile(
        String particleProfileId,
        List<EchoContentReference> particleReferences,
        double density,
        boolean affectedByStormVisibility,
        Map<String, String> attributes
) {
    public EchoAmbientParticleProfile {
        particleProfileId = AtmosphereContractGuards.normalizedId(particleProfileId, "ambient particle profile id");
        particleReferences = AtmosphereContractGuards.immutableList(particleReferences);
        density = AtmosphereContractGuards.clamped01(density);
        attributes = AtmosphereContractGuards.immutableMap(attributes);
    }
}
