package com.knoxhack.echo.vehiclecore;

public record EchoVehicleId(String value) {
    public EchoVehicleId {
        value = VehicleContractGuards.normalizedId(value, "vehicle id");
    }

    public static EchoVehicleId of(String value) {
        return new EchoVehicleId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
