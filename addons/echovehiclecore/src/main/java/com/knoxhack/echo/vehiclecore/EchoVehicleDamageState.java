package com.knoxhack.echo.vehiclecore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoVehicleDamageState(
        double hullIntegrity,
        double engineIntegrity,
        boolean immobilized,
        boolean storageCompromised,
        String playerSummary,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoVehicleDamageState {
        hullIntegrity = VehicleContractGuards.clamped01(hullIntegrity);
        engineIntegrity = VehicleContractGuards.clamped01(engineIntegrity);
        playerSummary = VehicleContractGuards.optionalText(playerSummary);
        diagnostics = VehicleContractGuards.immutableList(diagnostics);
        attributes = VehicleContractGuards.immutableMap(attributes);
    }

    public boolean degraded() {
        return immobilized || storageCompromised || hullIntegrity < 0.35D
                || engineIntegrity < 0.35D || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
