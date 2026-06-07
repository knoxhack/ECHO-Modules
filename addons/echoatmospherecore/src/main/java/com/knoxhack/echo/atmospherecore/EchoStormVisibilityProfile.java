package com.knoxhack.echo.atmospherecore;

import java.util.Map;

public record EchoStormVisibilityProfile(
        String visibilityId,
        double clearVisibility,
        double stormVisibility,
        double screenHazeIntensity,
        boolean reducesDistantLights,
        Map<String, String> attributes
) {
    public EchoStormVisibilityProfile {
        visibilityId = AtmosphereContractGuards.normalizedId(visibilityId, "storm visibility id");
        clearVisibility = AtmosphereContractGuards.clamped01(clearVisibility);
        stormVisibility = AtmosphereContractGuards.clamped01(stormVisibility);
        screenHazeIntensity = AtmosphereContractGuards.clamped01(screenHazeIntensity);
        attributes = AtmosphereContractGuards.immutableMap(attributes);
    }
}
