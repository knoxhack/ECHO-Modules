package com.knoxhack.echo.vehiclecore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.List;
import java.util.Map;

public record EchoVehicleFuelProfile(
        String fuelType,
        int capacity,
        double drainPerTick,
        List<EchoContentReference> acceptedFuelReferences,
        Map<String, String> attributes
) {
    public EchoVehicleFuelProfile {
        fuelType = VehicleContractGuards.optionalText(fuelType);
        capacity = VehicleContractGuards.nonNegative(capacity, "fuel capacity");
        drainPerTick = VehicleContractGuards.nonNegative(drainPerTick, "fuel drain per tick");
        acceptedFuelReferences = VehicleContractGuards.immutableList(acceptedFuelReferences);
        attributes = VehicleContractGuards.immutableMap(attributes);
    }
}
