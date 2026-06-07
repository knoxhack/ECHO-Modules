package com.knoxhack.echo.biomecore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoBiomeHazardOverlay(
        String overlayId,
        EchoContentReference statusReference,
        EchoContentReference weatherReference,
        EchoContentReference visualReference,
        double intensity,
        boolean visibleOnHud,
        Map<String, String> attributes
) {
    public EchoBiomeHazardOverlay {
        overlayId = BiomeContractGuards.normalizedId(overlayId, "hazard overlay id");
        intensity = BiomeContractGuards.clamped01(intensity);
        attributes = BiomeContractGuards.immutableMap(attributes);
    }
}
