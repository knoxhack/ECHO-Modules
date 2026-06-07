package com.knoxhack.echo.vehiclecore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoVehicleMountProfile(
        int maxRiders,
        boolean hasDriverSeat,
        boolean supportsMountUi,
        EchoContentReference mountUiReference,
        Map<String, String> attributes
) {
    public EchoVehicleMountProfile {
        maxRiders = VehicleContractGuards.nonNegative(maxRiders, "max riders");
        attributes = VehicleContractGuards.immutableMap(attributes);
    }
}
