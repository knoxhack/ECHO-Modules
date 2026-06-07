package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.Objects;
import java.util.Set;

public record EchoFeatureProviderNode(
        EchoModuleId moduleId,
        Set<EchoRuntimeSide> runtimeSides,
        EchoApiStability apiStability,
        String trustLevel,
        boolean official,
        boolean enabled
) {
    public EchoFeatureProviderNode {
        Objects.requireNonNull(moduleId, "moduleId");
        runtimeSides = ModuleGraphContractGuards.immutableSet(runtimeSides);
        apiStability = apiStability == null ? EchoApiStability.EXPERIMENTAL : apiStability;
        trustLevel = ModuleGraphContractGuards.optionalText(trustLevel);
    }
}
