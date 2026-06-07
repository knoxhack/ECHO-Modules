package com.knoxhack.echo.vehiclecore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoVehicleProfile(
        EchoVehicleId id,
        EchoVehicleKind kind,
        EchoVehicleRole role,
        EchoVehicleMobilityMode mobilityMode,
        EchoModuleId ownerModule,
        EchoContentReference vehicleContentReference,
        EchoVehicleStorageProfile storageProfile,
        EchoVehicleFuelProfile fuelProfile,
        EchoVehicleDamageState defaultDamageState,
        EchoVehicleMountProfile mountProfile,
        EchoVehicleHookRefs hookRefs,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoVehicleProfile {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoVehicleKind.UNKNOWN : kind;
        role = role == null ? EchoVehicleRole.UNKNOWN : role;
        mobilityMode = mobilityMode == null ? EchoVehicleMobilityMode.UNKNOWN : mobilityMode;
        diagnostics = VehicleContractGuards.immutableList(diagnostics);
        attributes = VehicleContractGuards.immutableMap(attributes);
    }

    public boolean degradedByDefault() {
        return (defaultDamageState != null && defaultDamageState.degraded())
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
