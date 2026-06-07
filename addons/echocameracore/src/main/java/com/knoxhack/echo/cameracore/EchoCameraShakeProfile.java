package com.knoxhack.echo.cameracore;

import java.util.Map;

public record EchoCameraShakeProfile(
        String shakeId,
        double intensity,
        double frequency,
        long durationTicks,
        boolean traumaBased,
        Map<String, String> attributes
) {
    public EchoCameraShakeProfile {
        shakeId = CameraContractGuards.normalizedId(shakeId, "camera shake id");
        intensity = CameraContractGuards.clamped01(intensity);
        frequency = CameraContractGuards.nonNegative(frequency, "shake frequency");
        durationTicks = Math.max(0L, durationTicks);
        attributes = CameraContractGuards.immutableMap(attributes);
    }
}
