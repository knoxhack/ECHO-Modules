package com.knoxhack.echo.machinecore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.List;
import java.util.Map;

public record EchoMachineAutomationHook(
        String hookId,
        EchoMachineAutomationHookKind kind,
        EchoContentReference targetReference,
        List<EchoFeatureId> optionalFeatures,
        boolean remoteSafe,
        Map<String, String> attributes
) {
    public EchoMachineAutomationHook {
        hookId = MachineContractGuards.normalizedId(hookId, "automation hook id");
        kind = kind == null ? EchoMachineAutomationHookKind.UNKNOWN : kind;
        optionalFeatures = MachineContractGuards.immutableList(optionalFeatures);
        attributes = MachineContractGuards.immutableMap(attributes);
    }
}
