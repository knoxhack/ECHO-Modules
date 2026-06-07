package com.knoxhack.echo.vehiclecore;

import java.util.Map;

public record EchoVehicleStorageProfile(
        int itemSlots,
        int fluidTanks,
        int passengerSeats,
        int cargoMassLimit,
        boolean preservesInventoryOnBreak,
        Map<String, String> attributes
) {
    public EchoVehicleStorageProfile {
        itemSlots = VehicleContractGuards.nonNegative(itemSlots, "item slots");
        fluidTanks = VehicleContractGuards.nonNegative(fluidTanks, "fluid tanks");
        passengerSeats = VehicleContractGuards.nonNegative(passengerSeats, "passenger seats");
        cargoMassLimit = VehicleContractGuards.nonNegative(cargoMassLimit, "cargo mass limit");
        attributes = VehicleContractGuards.immutableMap(attributes);
    }
}
