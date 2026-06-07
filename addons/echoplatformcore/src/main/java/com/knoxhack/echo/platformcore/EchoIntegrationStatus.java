package com.knoxhack.echo.platformcore;

public enum EchoIntegrationStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    UNAVAILABLE("unavailable"),
    DEGRADED("degraded"),
    DISABLED("disabled"),
    INCOMPATIBLE("incompatible"),
    MISSING_DEPENDENCY("missing_dependency"),
    MISSING_FEATURE("missing_feature");

    private final String serializedName;

    EchoIntegrationStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
