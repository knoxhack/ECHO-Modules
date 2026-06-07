package com.knoxhack.echo.packcore;

import java.util.Map;
import java.util.Objects;

public record EchoPerformanceProfile(
        EchoPackVariantId variantId,
        String name,
        EchoHardwareRecommendation hardwareRecommendation,
        int targetFps,
        int maxViewDistance,
        Map<String, String> tuning
) {
    public EchoPerformanceProfile {
        Objects.requireNonNull(variantId, "variantId");
        name = PackContractGuards.requireText(name, "performance profile name");
        targetFps = Math.max(targetFps, 0);
        maxViewDistance = Math.max(maxViewDistance, 0);
        tuning = PackContractGuards.immutableStringMap(tuning);
    }
}
