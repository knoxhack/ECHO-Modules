package com.knoxhack.echo.atmospherecore;

import java.util.Map;

public record EchoSkyTintProfile(
        String skyTintId,
        int dayColorArgb,
        int nightColorArgb,
        int stormColorArgb,
        double celestialVisibility,
        Map<String, String> attributes
) {
    public EchoSkyTintProfile {
        skyTintId = AtmosphereContractGuards.normalizedId(skyTintId, "sky tint id");
        dayColorArgb = AtmosphereContractGuards.argb(dayColorArgb);
        nightColorArgb = AtmosphereContractGuards.argb(nightColorArgb);
        stormColorArgb = AtmosphereContractGuards.argb(stormColorArgb);
        celestialVisibility = AtmosphereContractGuards.clamped01(celestialVisibility);
        attributes = AtmosphereContractGuards.immutableMap(attributes);
    }
}
