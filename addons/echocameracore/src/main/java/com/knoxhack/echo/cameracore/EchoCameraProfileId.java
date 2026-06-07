package com.knoxhack.echo.cameracore;

public record EchoCameraProfileId(String value) {
    public EchoCameraProfileId {
        value = CameraContractGuards.normalizedId(value, "camera profile id");
    }

    public static EchoCameraProfileId of(String value) {
        return new EchoCameraProfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
