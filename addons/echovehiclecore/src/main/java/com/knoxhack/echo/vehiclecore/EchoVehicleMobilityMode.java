package com.knoxhack.echo.vehiclecore;

public enum EchoVehicleMobilityMode {
    GROUND("ground"),
    HOVER("hover"),
    AIR("air"),
    SPACE_DROP("space_drop"),
    RAIL("rail"),
    WATER("water"),
    STATIC("static"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoVehicleMobilityMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
