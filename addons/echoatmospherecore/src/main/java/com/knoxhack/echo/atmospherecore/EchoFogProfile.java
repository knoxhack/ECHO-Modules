package com.knoxhack.echo.atmospherecore;

import java.util.Map;

public record EchoFogProfile(
        String fogId,
        int colorArgb,
        double density,
        double startDistance,
        double endDistance,
        boolean stormAffected,
        Map<String, String> attributes
) {
    public EchoFogProfile {
        fogId = AtmosphereContractGuards.normalizedId(fogId, "fog profile id");
        colorArgb = AtmosphereContractGuards.argb(colorArgb);
        density = AtmosphereContractGuards.clamped01(density);
        startDistance = Math.max(0.0D, startDistance);
        endDistance = Math.max(startDistance, endDistance);
        attributes = AtmosphereContractGuards.immutableMap(attributes);
    }
}
