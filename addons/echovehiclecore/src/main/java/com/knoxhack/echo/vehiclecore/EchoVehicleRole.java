package com.knoxhack.echo.vehiclecore;

public enum EchoVehicleRole {
    EXPLORATION("exploration"),
    CARGO("cargo"),
    CONVOY("convoy"),
    EXTRACTION("extraction"),
    DEPLOYMENT("deployment"),
    COMBAT_SUPPORT("combat_support"),
    UTILITY("utility"),
    DECORATIVE("decorative"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoVehicleRole(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
