package com.knoxhack.echo.machinecore;

import java.util.List;
import java.util.Map;

public record EchoMachineMaintenanceProfile(
        double wearPerOperation,
        int serviceIntervalTicks,
        List<String> serviceTags,
        boolean supportsFieldRepair,
        Map<String, String> attributes
) {
    public EchoMachineMaintenanceProfile {
        wearPerOperation = MachineContractGuards.nonNegative(wearPerOperation, "wear per operation");
        serviceIntervalTicks = MachineContractGuards.nonNegative(serviceIntervalTicks, "service interval ticks");
        serviceTags = MachineContractGuards.immutableList(serviceTags);
        attributes = MachineContractGuards.immutableMap(attributes);
    }
}
