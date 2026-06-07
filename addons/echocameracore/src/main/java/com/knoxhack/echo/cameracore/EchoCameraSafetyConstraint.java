package com.knoxhack.echo.cameracore;

import java.util.Map;

public record EchoCameraSafetyConstraint(
        boolean allowMotionSicknessRisk,
        double maxShakeIntensity,
        double maxFovChange,
        boolean respectReducedMotion,
        boolean allowControlLock,
        Map<String, String> attributes
) {
    public EchoCameraSafetyConstraint {
        maxShakeIntensity = CameraContractGuards.clamped01(maxShakeIntensity);
        maxFovChange = CameraContractGuards.nonNegative(maxFovChange, "max fov change");
        attributes = CameraContractGuards.immutableMap(attributes);
    }
}
