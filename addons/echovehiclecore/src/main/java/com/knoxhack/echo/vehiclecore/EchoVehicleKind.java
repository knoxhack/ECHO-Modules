package com.knoxhack.echo.vehiclecore;

public enum EchoVehicleKind {
    ROVER("rover"),
    CONVOY_TRUCK("convoy_truck"),
    DROP_POD("drop_pod"),
    DRONE("drone"),
    SHUTTLE("shuttle"),
    MOUNT("mount"),
    RAIL("rail"),
    WATERCRAFT("watercraft"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoVehicleKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
